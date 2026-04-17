@file:OptIn(ExperimentalMainFunctionArgumentsDsl::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
}

group = "dev.usbharu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvm()
    linuxX64()
    macosArm64()
    mingwX64()
    js() {
        nodejs() {
            passCliArgumentsToMainFunction()
        }
        binaries.executable()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "dev.usbharu.kotui.main"
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}
