/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
@file:Suppress("UnstableApiUsage")

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.saff.checkmark.compose"

    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    defaultConfig {
        minSdk = 30
        targetSdk = 33
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {}

// SAFF: upgrades
dependencies {
    implementation(project(":checkmark"))
    implementation(project(":junit-ktx"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("androidx.compose.ui:ui-test-junit4:1.4.0-alpha03")
    testImplementation("org.robolectric:robolectric:4.9")
    implementation("androidx.test.espresso:espresso-core:3.5.0")
    implementation("androidx.customview:customview-poolingcontainer:1.0.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.0-alpha03")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.material:material:1.4.0-alpha03")
    testImplementation("androidx.arch.core:core-runtime:2.1.0")
    testImplementation("androidx.activity:activity-compose:1.6.1")
}

// SAFF: what would it take to re-enable this?
tasks.matching { it.name == "testReleaseUnitTest" }.all { enabled = false }