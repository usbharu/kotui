import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    js {
        nodejs()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "dev.usbharu.kotui.markdown.main"
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":"))
            implementation("org.jetbrains.compose.runtime:runtime:1.8.0")
            implementation("dev.usbharu:markdown-kt:1.1.0-SNAPSHOT")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

tasks.matching {
    it.name == "compileTestDevelopmentExecutableKotlinJs" ||
        it.name == "jsNodeTest" ||
        it.name == "jsTest" ||
        it.name == "jsBrowserTest"
}.configureEach {
    enabled = false
}
