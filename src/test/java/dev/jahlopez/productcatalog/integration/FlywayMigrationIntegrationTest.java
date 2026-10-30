package dev.jahlopez.productcatalog.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void migrateFromV1_shiftsLegacyVersionsAndEnforcesConstraints() throws Exception {
        String schema = createSchema();
        try {
            migrateToVersionOne(schema);
            seedLegacyVersionZeroRows(schema);

            migrateToVersionTwo(schema);

            assertThat(queryLong(schema, "SELECT version FROM products WHERE sku = 'LEGACY-PRODUCT'"))
                    .isEqualTo(1L);
            assertThat(queryLong(schema,
                    "SELECT (payload::jsonb ->> 'version')::bigint FROM outbox WHERE aggregate_id = '7'"))
                    .isEqualTo(1L);

            execute(schema, """
                    INSERT INTO products (sku, name, price, stock_quantity)
                    VALUES ('DEFAULT-VERSION', 'Default version', 1.00, 0)
                    """);
            assertThat(queryLong(schema,
                    "SELECT version FROM products WHERE sku = 'DEFAULT-VERSION'"))
                    .isEqualTo(1L);

            assertThatThrownBy(() -> execute(schema, """
                    INSERT INTO products (sku, name, price, stock_quantity, version)
                    VALUES ('INVALID-VERSION', 'Invalid version', 1.00, 0, 0)
                    """)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insertOutbox(schema, "{\"version\":0}"))
                    .isInstanceOf(SQLException.class);
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    void migrateFromV1_rejectsMalformedLegacyPayload() throws Exception {
        String schema = createSchema();
        try {
            migrateToVersionOne(schema);
            insertOutbox(schema, "not-json");

            assertThatThrownBy(() -> migrateToVersionTwo(schema))
                    .isInstanceOf(FlywayException.class);
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    void migrateFromV1_rejectsProductVersionThatWouldOverflowInt() throws Exception {
        String schema = createSchema();
        try {
            migrateToVersionOne(schema);
            execute(schema, """
                    INSERT INTO products (sku, name, price, stock_quantity, version)
                    VALUES ('OVERFLOW-PRODUCT', 'Overflow product', 1.00, 0, 2147483647)
                    """);

            assertThatThrownBy(() -> migrateToVersionTwo(schema))
                    .isInstanceOf(FlywayException.class)
                    .hasStackTraceContaining("Cannot migrate product versions");
        } finally {
            dropSchema(schema);
        }
    }

    private static String createSchema() throws SQLException {
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        return schema;
    }

    private static void migrateToVersionOne(String schema) {
        flyway(schema)
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();
    }

    private static void migrateToVersionTwo(String schema) {
        flyway(schema).load().migrate();
    }

    private static FluentConfiguration flyway(String schema) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration");
    }

    private static void seedLegacyVersionZeroRows(String schema) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement product = connection.prepareStatement("""
                     INSERT INTO %s.products
                         (id, sku, name, price, stock_quantity, version)
                     VALUES (7, 'LEGACY-PRODUCT', 'Legacy product', 10.00, 2, 0)
                     """.formatted(schema));
             PreparedStatement event = connection.prepareStatement("""
                     INSERT INTO %s.outbox
                         (event_id, aggregate_type, aggregate_id, event_type, payload)
                     VALUES (?, 'product', '7', 'ProductCreated', '{"version":0}')
                     """.formatted(schema))) {
            product.executeUpdate();
            event.setObject(1, UUID.randomUUID());
            event.executeUpdate();
        }
    }

    private static void insertOutbox(String schema, String payload) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement event = connection.prepareStatement("""
                     INSERT INTO %s.outbox
                         (event_id, aggregate_type, aggregate_id, event_type, payload)
                     VALUES (?, 'product', 'new', 'ProductCreated', ?)
                     """.formatted(schema))) {
            event.setObject(1, UUID.randomUUID());
            event.setString(2, payload);
            event.executeUpdate();
        }
    }

    private static long queryLong(String schema, String query) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     query.replace(" FROM ", " FROM " + schema + "."))) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void execute(String schema, String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql.replace("INSERT INTO ", "INSERT INTO " + schema + "."));
        }
    }

    private static void dropSchema(String schema) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
