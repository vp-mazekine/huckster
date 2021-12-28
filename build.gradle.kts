import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("kapt") version "1.4.10"
    application
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.broxus"
version = "0.4-RC1"

repositories {
    mavenCentral()
    jcenter()
    //maven { url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
    maven { url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/") } // for SNAPSHOT builds
    maven { url = uri("https://jitpack.io") }
}

val novaLibVersion = "0.0.7-alpha"
val arrowVersion = "0.11.0"
val telegramBotVersion = "6.0.1"
val log4jVersion = "2.15.0"
val retrofitVersion = "2.3.0" //"2.3.0"
val rxJavaVersion = "2.0.1" //"2.0.1"
val logbackVersion = "1.2.3" // "1.2.3"

dependencies {
    testImplementation(kotlin("test-junit", "1.4.0"))
    implementation(kotlin("stdlib-jdk8", "1.4.0"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")

    //  Retrofit dependencies
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:adapter-rxjava2:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("io.reactivex.rxjava2:rxandroid:$rxJavaVersion")

    //  Arrow dependencies
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")

    //  Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("io.sentry:sentry-log4j2:4.1.0")

    //  Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0-RC1") // 1.0.0-RC1

    //  Google Docs
    implementation("com.google.api-client:google-api-client:1.30.4")    //  1.30.4
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.6")  //  1.30.6
    implementation("com.google.apis:google-api-services-sheets:v4-rev20201102-1.30.10") //  1.30.10

    //  System utils
    implementation("org.apache.commons:commons-lang3:3.11") //  3.11

    //  Broxus
    implementation("com.broxus:nova-lib:$novaLibVersion")

    //  Kotlin Telegram Bot
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegramBotVersion")

    //  Gate.io
    implementation("io.gate:gate-api:6.23.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}.forEach {
    it.kotlinOptions { freeCompilerArgs = listOf("-Xnew-inference") }
}

application {
    mainClassName = "com.broxus.huckster.MainKt"    //  Have to leave it here until shadowJar fixes the compatibility bug
    mainClass.set("com.broxus.huckster.MainKt")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.jar {
    archiveBaseName.set("huckster")
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveExtension.set("jar")
}

tasks.shadowJar {
    archiveBaseName.set("hucksterFat")
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveExtension.set("jar")
    minimize()
}