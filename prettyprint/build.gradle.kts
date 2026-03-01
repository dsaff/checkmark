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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  testImplementation("junit:junit:4.13.1")
  implementation(kotlin("reflect"))
}

val sourcesJar by tasks.creating(Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
  dependsOn.add(tasks["javadoc"])
  archiveClassifier.set("javadoc")
  from(tasks["javadoc"])
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  jvmTarget = "1.8"
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "net.saff.checkmark"
      artifactId = "prettyprint"
      version = "0.1.4"

      from(components["java"])

      artifact(sourcesJar)
      artifact(javadocJar)

      pom {
        name.set("checkmark")
        description.set("Pretty-printing for debugging and assertions")
        url.set("https://github.com/dsaff/checkmark")

        licenses {
          license {
            name.set("Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("dsaff")
            name.set("David Saff")
            email.set("david@saff.net")
          }
        }

        scm {
          connection.set("https://github.com/dsaff/checkmark.git")
          developerConnection.set("https://github.com/dsaff/checkmark.git")
          url.set("https://github.com/dsaff/checkmark")
        }
      }
    }
  }

  val nexusUsername: String? by project
  val nexusPassword: String? by project

  repositories {
    maven {
      // For snapshots, url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
      url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
      credentials {
        username = nexusUsername
        password = nexusPassword
      }
    }
  }
}

signing {
  sign(publishing.publications["maven"])
}