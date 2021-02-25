import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.3.61"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
}

group = "com.postindustria.ssai"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "Hoxton.SR1"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation(group="org.postgresql", name="postgresql", version="42.2.9")
    implementation(group="io.opencensus", name="opencensus-api", version="0.25.0")
    implementation(group="io.opencensus", name="opencensus-impl", version="0.25.0")
    implementation(group="io.opencensus", name="opencensus-exporter-stats-stackdriver", version="0.25.0")
    implementation("com.google.cloud:google-cloud-monitoring")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.cloud:spring-cloud-gcp-starter")
    implementation("org.springframework.cloud:spring-cloud-gcp-starter-pubsub")
    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
    implementation(project(":common"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
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
