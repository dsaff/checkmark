buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("java")
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("signing")
    id("maven-publish")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("junit:junit:4.13.2")
    implementation(kotlin("reflect"))
    testImplementation(project(":checkmark"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}