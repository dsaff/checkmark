pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
  plugins {
    kotlin("multiplatform") version "2.1.10"
    kotlin("jvm") version "1.7.0"
    kotlin("android") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("com.android.library") version "7.2.0"
  }
  resolutionStrategy {
    eachPlugin {
//      if (requested.id.namespace == "com.android") {
//        useModule("com.android.tools.build:gradle:${requested.version}")
//      }
    }
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

include(":checkmark")
include(":prettyprint")
include(":junit-ktx")
include(":checkmark-compose")