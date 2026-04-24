plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "kotui"

includeBuild("third-party/markdown-kt") {
    dependencySubstitution {
        substitute(module("dev.usbharu:markdown-kt")).using(project(":library"))
    }
}

include(":kotui-markdown")
include(":kotui-image")
