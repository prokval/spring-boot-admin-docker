plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.drt"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

extra["springBootAdminVersion"] = "3.4.5"
val springCloudVersion by extra("2024.0.1")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("de.codecentric:spring-boot-admin-starter-server")
    implementation("org.springframework.cloud:spring-cloud-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // OAuth2 Client for login redirects
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // Resource server for API protection
    //implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}

dependencyManagement {
    imports {
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}
