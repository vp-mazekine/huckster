import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("kapt") version "1.4.10"
    application
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.broxus"
version = "0.3.3"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
    maven { url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/") } // for SNAPSHOT builds
    maven { url = uri("https://jitpack.io") }
}

val novaLibVersion = "0.0.6-alpha"
val arrowVersion = "0.11.0"
val telegramBotVersion = "6.0.1"

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")

    //  Retrofit dependencies
    implementation("com.squareup.retrofit2:retrofit:2.3.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.3.0")
    implementation("com.squareup.retrofit2:converter-gson:2.4.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.0.1")

    //  Arrow dependencies
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")

    //  Logging
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.2.51")

    //  Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0-RC1")

    //  Google Docs
    implementation("com.google.api-client:google-api-client:1.30.4")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.6")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20200922-1.30.10")

    //  System utils
    implementation("org.apache.commons:commons-lang3:3.11")

    //  Broxus
    implementation("com.broxus:nova-lib:$novaLibVersion")

    //  Kotlin Telegram Bot
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegramBotVersion")
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