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

    compileSdk = 34

    defaultConfig {
        minSdk = 30
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {}

dependencies {
    implementation(project(":checkmark"))
    implementation(project(":junit-ktx"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("androidx.compose.ui:ui-test-junit4:1.6.7")
    testImplementation("org.robolectric:robolectric:4.9.2")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.customview:customview-poolingcontainer:1.0.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.material:material:1.6.7")
    testImplementation("androidx.arch.core:core-runtime:2.2.0")
    testImplementation("androidx.activity:activity-compose:1.9.0")
}

tasks.matching { it.name == "testReleaseUnitTest" }.all { enabled = false }

kotlin {
    jvmToolchain(20)
}