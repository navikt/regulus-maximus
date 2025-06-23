
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.8.0"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

val kotlin_version = "2.0.21"
val logback_version = "1.5.18"
val logback_encoder_version = "8.0"
val flyway_version= "11.4.1"
val postgres_version= "42.7.7"
val jackson_version= "2.18.3"
val mockitVersion = "5.4.0"
val kafkaClientVersion = "3.9.1"
val googleCloudStorageVersion = "2.48.1"

dependencies {
    //implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("net.logstash.logback:logstash-logback-encoder:${logback_encoder_version}")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientVersion")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("com.google.cloud:google-cloud-storage:$googleCloudStorageVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks {
    test {
        useJUnitPlatform()
    }
    shadowJar {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes["Main-Class"] = "no.nav.tsm.mottak.ApplicationKt"
        }
    }

    bootJar {
        archiveFileName = "app.jar"
    }
}
