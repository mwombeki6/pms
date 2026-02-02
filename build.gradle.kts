plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.pms"
version = "0.0.1-SNAPSHOT"
description = "booking system for pms"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// WebFlux (Reactive HTTP)
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Flyway migrations (uses JDBC driver only for migration step)
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework:spring-jdbc")

	// R2DBC + Postgres
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// Coroutines bridge for Reactor/WebFlux
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	// Kotlin goodies
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// OpenAPI for WebFlux + Scalar UI
	implementation("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.1")
	implementation("org.springdoc:springdoc-openapi-starter-webflux-scalar:3.0.1")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
