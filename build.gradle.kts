plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.jahlopez"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val mapstructVersion = "1.5.5.Final"
val springdocVersion = "3.2.0"
val logstashEncoderVersion = "9.0"
val testcontainersVersion = "2.0.5"
val testcontainersRedisVersion = "2.2.4"
val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-jackson2")
    runtimeOnly("org.postgresql:postgresql")

    // Database Migrations
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Observability & Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // Object Mapping
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // Annotation Processors & Boilerplate Reduction
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Test Annotation Support
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Testing Core
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("com.h2database:h2")

    // Integration Testing & Testcontainers
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
    testImplementation("com.redis:testcontainers-redis:$testcontainersRedisVersion")
    mockitoAgent("org.mockito:mockito-core") {
        isTransitive = false
    }
}

jacoco {
    toolVersion = "0.8.16"
}

// Scope MapStruct flags strictly to main compilation task to prevent unused-option test warnings
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(
        listOf(
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=WARN"
        )
    )
}

// Global compiler options
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.test {
    useJUnitPlatform()
    // Mockito's agent appends bootstrap classes; disable CDS to avoid its JVM startup warning.
    jvmArgs("-Xshare:off", "-javaagent:${mockitoAgent.singleFile}")
    finalizedBy(tasks.jacocoTestReport) // Automatically generate coverage report after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}