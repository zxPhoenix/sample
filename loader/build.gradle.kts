import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.2.4.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.3.61"
	kotlin("jvm") version "1.3.71"
	kotlin("plugin.spring") version "1.3.71"
}

group = "com.postindustria.ssai"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
	maven("http://maven.icm.edu.pl/artifactory/repo/")
}

extra["springCloudVersion"] = "Hoxton.SR3"

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.springframework.cloud:spring-cloud-gcp-starter")
	implementation("org.springframework.cloud:spring-cloud-gcp-starter-pubsub")
	implementation("com.github.kokorin.jaffree:jaffree:0.9.4")
	implementation(files("../m3u8-parser/build/libs/m3u8-parser-0.9.jar"))
	implementation(group="com.squareup.okhttp3", name="okhttp", version="4.2.2")
	runtimeOnly("org.postgresql:postgresql")

	implementation(project(":common"))

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.integration:spring-integration-test")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}
