import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}

group = "com.janusleaf"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

// ============================================
// Integration Test Source Set
// ============================================
sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // OpenAI (for later)
    implementation("com.aallam.openai:openai-client:3.6.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    
    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Testing - JUnit 5
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito") // We use MockK instead
    }
    testImplementation("org.springframework.security:spring-security-test")
    
    // MockK - Kotlin-native mocking
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    
    // Kotest - Kotlin-native assertions and testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
    
    // Testcontainers - Real database for integration tests
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    
    // H2 for fast unit tests (optional fallback)
    testImplementation("com.h2database:h2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Test logging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ============================================
// Integration Test Task
// ============================================
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    useJUnitPlatform()
    
    // Run after unit tests
    shouldRunAfter(tasks.test)
    
    // Test logging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Make 'check' task run both unit and integration tests
tasks.check {
    dependsOn(tasks.named("integrationTest"))
}

// Make JPA work with Kotlin data classes
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
