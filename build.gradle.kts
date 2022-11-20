import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.springframework.boot:spring-boot-devtools")
    // Spring Cacheとは関係ないメモ
    // suspendな関数をコントローラーのメソッドにするために必要なライブラリ。
    // TODO これを入れないと動かない理由がわかってない。
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-reactor
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // spring-boot-starter-cacheにより、Spring Cacheを使う上で必要となるライブラリを丸ごと依存に含めることができる。
    implementation("org.springframework.boot:spring-boot-starter-cache")
    // Spring CacheのCaffeine実装。
    implementation("com.github.ben-manes.caffeine:caffeine")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
