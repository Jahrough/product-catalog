plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.jahlopez"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Security Overrides
    constraints {
        implementation("org.apache.commons:commons-lang3:3.18.0") {
            because("Remediate CVE-2025-48924 transitive vulnerability")
        }
    }

    // Spring Boot: Web, Persistence, and Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")

    // Database Migrations: Flyway
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")

    // API Documentation: OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Caching: Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Monitoring and Kubernetes Health Probes
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Object Mapping: MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Boilerplate Reduction: Lombok and MapStruct Binding
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Testing: JUnit 5 and Spring Boot
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Testing: In-Memory Database
    testImplementation("com.h2database:h2")

    // Integration Testing: Testcontainers (Fixed with Platform BOM & Explicit Versions)
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.5"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
}

tasks.test {
    useJUnitPlatform()
}