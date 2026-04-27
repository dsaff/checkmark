rootProject.name = "sanctuary"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.1.10"
        kotlin("multiplatform") version "2.1.10"
        kotlin("plugin.serialization") version "2.1.10"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":prettyprint")
project(":prettyprint").projectDir = file("../prettyprint")

include(":befuzz")
project(":befuzz").projectDir = file("../../befuzz")