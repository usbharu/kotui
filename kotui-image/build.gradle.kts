import org.jetbrains.kotlin.konan.target.Family
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

val ktorVersion = "3.4.3"
val korimVersion = "4.0.10"
val hostFamily = when {
    System.getProperty("os.name").contains("Windows", ignoreCase = true) -> Family.MINGW
    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> Family.OSX
    else -> Family.LINUX
}

kotlin {
    jvm()
    macosArm64()
    linuxX64()
    mingwX64()
    js {
        nodejs()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family == hostFamily) {
            binaries {
                executable {
                    entryPoint = "dev.usbharu.kotui.image.main"
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":"))
            implementation("org.jetbrains.compose.runtime:runtime:1.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation("com.github.skydoves:landscapist-core:2.9.7")
        }

        val macosArm64Main by getting {
            dependencies {
                implementation("com.github.skydoves:landscapist-core:2.9.7")
            }
        }

        // Intermediate source set shared by linuxX64 / mingwX64 / js that hosts the
        // Ktor + korim loader code. Dependencies are declared per leaf target below
        // because Gradle's variant-aware resolution does not propagate KMP klib
        // dependencies declared in a freshly created intermediate set without an
        // attached Kotlin target.
        val ktorKorimMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("com.soywiz.korlibs.korim:korim:$korimVersion")
            }
        }

        val linuxX64Main by getting {
            dependsOn(ktorKorimMain)
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-curl:$ktorVersion")
                implementation("com.soywiz.korlibs.korim:korim:$korimVersion")
            }
        }
        val mingwX64Main by getting {
            dependsOn(ktorKorimMain)
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-winhttp:$ktorVersion")
                implementation("com.soywiz.korlibs.korim:korim:$korimVersion")
            }
        }
        val jsMain by getting {
            dependsOn(ktorKorimMain)
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("com.soywiz.korlibs.korim:korim:$korimVersion")
            }
        }
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

tasks.matching {
    it.name == "linkDebugTestLinuxX64" ||
        it.name == "linkReleaseTestLinuxX64" ||
        it.name == "linuxX64Test" ||
    it.name == "compileTestDevelopmentExecutableKotlinJs" ||
        it.name == "jsNodeTest" ||
        it.name == "jsTest" ||
        it.name == "jsBrowserTest"
}.configureEach {
    enabled = false
}
