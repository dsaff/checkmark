pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
  plugins {
    kotlin("multiplatform") version "1.6.21"
    kotlin("jvm") version "1.6.21"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

include(":checkmark")
include(":prettyprint")