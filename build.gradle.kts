
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.8.0"
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "no.nav.tsm.mottak"
version = "0.0.2"

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val ktor_version = "2.3.7"
val kotlin_version = "1.9.22"
val logback_version = "1.4.14"
val logback_encoder_version = "7.2"
val prometheus_version = "1.6.3"
val exposed_version= "0.41.1"
val flyway_version= "10.18.2"
val postgres_version= "42.7.2"
val koin_version= "3.5.3"
val jackson_version= "2.16.1"
val conditional_version = "5.1.2"

dependencies {
    //implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.data:spring-data-r2dbc")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.postgresql:r2dbc-postgresql")

    implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("net.logstash.logback:logstash-logback-encoder:${logback_encoder_version}")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.5")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")

    implementation("org.flywaydb:flyway-core:$flyway_version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("no.nav.boot:boot-conditionals:$conditional_version")

}

tasks {
    bootJar {
        archiveFileName = "app.jar"
    }
}
