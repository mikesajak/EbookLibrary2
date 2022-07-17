plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("org.jetbrains.kotlin.kapt") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.3.2"
}

version = "0.1"
group = "com.mikesajak.library"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    kapt("io.micronaut.data:micronaut-data-processor")
    kapt("io.micronaut:micronaut-inject-java")
    kapt("io.micronaut.data:micronaut-data-hibernate-jpa")

    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut:micronaut-runtime")

    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")
    implementation("io.micronaut:micronaut-validation")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

//    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-hibernate-jpa")
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
//    implementation("io.micronaut.sql:micronaut-jdbc-tomcat")
    implementation("io.micronaut.beanvalidation:micronaut-hibernate-validator")
//    implementation("io.micronaut.flyway:micronaut-flyway")


    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
//    runtimeOnly("com.h2database:h2")
    implementation("com.h2database:h2")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

    implementation("cz.jirutka.rsql:rsql-parser:2.1.0")
    implementation("org.jooq:jooq:3.16.6")

    testImplementation("io.kotest:kotest-runner-junit5-jvm")
    testImplementation("io.kotest:kotest-assertions-core")
//    testImplementation("io.kotest:kotest-property")

    // https://mvnrepository.com/artifact/org.apache.jena/jena-tdb2
    implementation("org.apache.jena:jena-tdb2:4.5.0")
}


application {
    mainClass.set("com.mikesajak.library.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.mikesajak.library.*")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


