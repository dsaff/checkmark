/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

plugins {
  kotlin("multiplatform")
  application
  kotlin("plugin.serialization")
  id("maven-publish")
}

// Currently can publish to mavenLocal as net.saff:befuzz-jvm:0.1.1
group = "net.saff"
version = "0.1.1"

kotlin {
  val jvmTarget = jvm {
    withJava()
  }

  // SAFF: re-enable
//  macosArm64 {
//    binaries {
//      executable()
//    }
//  }

//  macosX64 {
//    binaries {
//      executable()
//    }
//  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation("junit:junit:4.13.2")
        implementation("net.saff.checkmark:checkmark:0.1.5")
      }
    }
    val nativeMain by creating {
      dependsOn(commonMain)
    }
//    val macosX64Main by getting {
//      dependsOn(nativeMain)
//    }
//    val macosArm64Main by getting {
//      dependsOn(nativeMain)
//    }
  }

  // Thanks to https://github.com/jmfayard/kotlin-cli-starter/blob/5201ee91122b4572d40167e7fbfae2f341ce5dfb/build.gradle.kts#L139
  // Without whom I'd have never found this...
  tasks.withType<JavaExec> {
    // code to make run task in kotlin multiplatform work
    val compilation =
      jvmTarget.compilations.getByName<KotlinJvmCompilation>("main")

    val classes = files(
      compilation.runtimeDependencyFiles,
      compilation.output.allOutputs
    )
    classpath(classes)
  }
}

val jvmJars: Configuration by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
  extendsFrom(configurations["implementation"], configurations["runtimeOnly"])
}

artifacts {
  add("jvmJars", tasks.getByName("jvmJar"))
}

application {
  mainClass.set("net.saff.heap.ProfileKt")
}
dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
repositories {
  mavenCentral()
}
//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//  jvmTarget = "1.8"
//}
//val compileTestKotlin: KotlinCompile by tasks
//compileTestKotlin.kotlinOptions {
//  jvmTarget = "1.8"
//}