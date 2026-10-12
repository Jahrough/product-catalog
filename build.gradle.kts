plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.jahlopez"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Force a secure version of commons-lang3 to remediate CVE-2025-48924
    constraints {
        implementation("org.apache.commons:commons-lang3:3.18.0") {
            because("Remediate CVE-2025-48924 transitive vulnerability")
        }
    }

    // Spring Boot Core Web & Database Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")

    // Database Migrations (Flyway)
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")

    // API Documentation (Swagger / OpenAPI 3)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Caching (Redis for high-traffic product catalog reads)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kubernetes Health Probes
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Lombok (Required for @Data, @Builder, etc. used in our models/DTOs)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
}