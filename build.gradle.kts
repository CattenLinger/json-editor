import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

val jacksonVersion = "2.13.0"

plugins {
    kotlin("jvm") version "1.7.10"
    id("application")
}

group = "net.catten"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("net.catten.property.editor.PropertyEditorMainAppKt")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus.shinonometn.com/repository/maven-public/")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    implementation("com.formdev:flatlaf:2.6")
    implementation("com.formdev:flatlaf-extras:2.6")
    implementation("com.formdev:svgSalamander:1.1.4")
    implementation("de.javagl:treetable:1.1.0-SNAPSHOT")
//    implementation(files("lib/org-netbeans-swing-outline.jar"))


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.4.4")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}