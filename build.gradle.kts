@file:OptIn(ExperimentalMainFunctionArgumentsDsl::class)

import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "dev.usbharu.kotui.ComposableSingletons*",
                    "dev.usbharu.kotui.MainKt",
                    "dev.usbharu.kotui.Main_jvmKt",
                    "dev.usbharu.kotui.TerminalResize_jvmKt*",
                    "dev.usbharu.kotui.TerminalKt",
                    "dev.usbharu.kotui.compose.clipboard.SystemClipboardWrite_jvmKt",
                    "dev.usbharu.kotui.compose.runtime.RunTuiKt",
                    "dev.usbharu.kotui.compose.runtime.MainLoop_jvmKt*",
                    "dev.usbharu.kotui.compose.runtime.Time_jvmKt",
                    "dev.usbharu.kotui.compose.widget.BadgeKt",
                    "dev.usbharu.kotui.compose.widget.BoxKt",
                    "dev.usbharu.kotui.compose.widget.ButtonKt",
                    "dev.usbharu.kotui.compose.widget.CenterKt",
                    "dev.usbharu.kotui.compose.widget.CheckboxKt",
                    "dev.usbharu.kotui.compose.widget.ColumnKt",
                    "dev.usbharu.kotui.compose.widget.DividerKt",
                    "dev.usbharu.kotui.compose.widget.ImageKt",
                    "dev.usbharu.kotui.compose.widget.ModalKt",
                    "dev.usbharu.kotui.compose.widget.PanelKt",
                    "dev.usbharu.kotui.compose.widget.ProgressBarKt",
                    "dev.usbharu.kotui.compose.widget.RadioGroupKt",
                    "dev.usbharu.kotui.compose.widget.RowKt",
                    "dev.usbharu.kotui.compose.widget.SelectKt",
                    "dev.usbharu.kotui.compose.widget.SelectableListKt",
                    "dev.usbharu.kotui.compose.widget.SpacerKt",
                    "dev.usbharu.kotui.compose.widget.SpinnerKt",
                    "dev.usbharu.kotui.compose.widget.TextKt",
                    "dev.usbharu.kotui.compose.widget.TextAreaKt",
                    "dev.usbharu.kotui.compose.widget.TextInputKt",
                    "dev.usbharu.kotui.compose.widget.VerticalDividerKt",
                    "dev.usbharu.kotui.utils.SixelSupport",
                )
            }
        }
        total {
            xml {
                onCheck = true
            }
            verify {
                rule("CommonMain branch coverage") {
                    minBound(90, CoverageUnit.BRANCH, AggregationType.COVERED_PERCENTAGE)
                }
            }
        }
    }
}

tasks.register("verifyCommonMainFileBranchCoverage") {
    group = "verification"
    description = "Verifies that each root commonMain source file with branch counters reaches 90% branch coverage."
    dependsOn("koverXmlReport")

    doLast {
        val reportFile = listOf(
            layout.buildDirectory.file("reports/kover/report.xml").get().asFile,
            layout.buildDirectory.file("reports/kover/xml/report.xml").get().asFile,
            layout.buildDirectory.file("reports/kover/xml/result.xml").get().asFile,
        ).firstOrNull { it.isFile }
            ?: layout.buildDirectory.dir("reports/kover").get().asFile
                .walkTopDown()
                .firstOrNull { it.isFile && it.extension == "xml" }
            ?: throw GradleException("Kover XML report was not found under build/reports/kover.")

        val commonMainRoot = layout.projectDirectory.dir("src/commonMain/kotlin").asFile
        val commonMainFiles = commonMainRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.relativeTo(commonMainRoot).invariantSeparatorsPath }
            .toSet()

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportFile)
        val packages = document.getElementsByTagName("package")
        val excludedFiles = setOf(
            "dev/usbharu/kotui/Main.kt",
            "dev/usbharu/kotui/Terminal.kt",
            "dev/usbharu/kotui/compose/runtime/RunTui.kt",
            "dev/usbharu/kotui/compose/widget/Badge.kt",
            "dev/usbharu/kotui/compose/widget/Box.kt",
            "dev/usbharu/kotui/compose/widget/Button.kt",
            "dev/usbharu/kotui/compose/widget/Center.kt",
            "dev/usbharu/kotui/compose/widget/Checkbox.kt",
            "dev/usbharu/kotui/compose/widget/Column.kt",
            "dev/usbharu/kotui/compose/widget/Divider.kt",
            "dev/usbharu/kotui/compose/widget/Image.kt",
            "dev/usbharu/kotui/compose/widget/Modal.kt",
            "dev/usbharu/kotui/compose/widget/Panel.kt",
            "dev/usbharu/kotui/compose/widget/ProgressBar.kt",
            "dev/usbharu/kotui/compose/widget/RadioGroup.kt",
            "dev/usbharu/kotui/compose/widget/Row.kt",
            "dev/usbharu/kotui/compose/widget/Select.kt",
            "dev/usbharu/kotui/compose/widget/SelectableList.kt",
            "dev/usbharu/kotui/compose/widget/Spacer.kt",
            "dev/usbharu/kotui/compose/widget/Spinner.kt",
            "dev/usbharu/kotui/compose/widget/Text.kt",
            "dev/usbharu/kotui/compose/widget/TextArea.kt",
            "dev/usbharu/kotui/compose/widget/TextInput.kt",
            "dev/usbharu/kotui/compose/widget/VerticalDivider.kt",
        )
        val belowTargetFiles = mutableListOf<String>()
        var totalCovered = 0
        var totalBranches = 0

        for (packageIndex in 0 until packages.length) {
            val packageElement = packages.item(packageIndex) as Element
            val packageName = packageElement.getAttribute("name")
            if (!packageName.startsWith("dev/usbharu/kotui")) continue

            val sourceFiles = packageElement.getElementsByTagName("sourcefile")
            for (sourceIndex in 0 until sourceFiles.length) {
                val sourceElement = sourceFiles.item(sourceIndex) as Element
                val sourcePath = "$packageName/${sourceElement.getAttribute("name")}"
                if (sourcePath !in commonMainFiles) continue
                if (sourcePath in excludedFiles) continue

                val counters = sourceElement.getElementsByTagName("counter")
                for (counterIndex in 0 until counters.length) {
                    val counter = counters.item(counterIndex) as Element
                    if (counter.getAttribute("type") != "BRANCH") continue

                    val missed = counter.getAttribute("missed").toInt()
                    val covered = counter.getAttribute("covered").toInt()
                    val branches = missed + covered
                    if (branches == 0) continue

                    totalCovered += covered
                    totalBranches += branches

                    val percentage = covered * 100.0 / branches
                    if (percentage < 90.0) {
                        belowTargetFiles += "%s: %.2f%% (%d/%d branches)".format(
                            sourcePath,
                            percentage,
                            covered,
                            branches,
                        )
                    }
                }
            }
        }

        if (totalBranches == 0) {
            throw GradleException("No commonMain branch counters were found in ${reportFile.path}.")
        }

        val totalPercentage = totalCovered * 100.0 / totalBranches
        logger.lifecycle(
            "Root commonMain file branch coverage: %.2f%% (%d/%d branches)".format(
                totalPercentage,
                totalCovered,
                totalBranches,
            )
        )

        if (belowTargetFiles.isNotEmpty()) {
            logger.lifecycle(
                buildString {
                    appendLine("Files below 90% branch coverage:")
                    belowTargetFiles.sorted().forEach { appendLine(" - $it") }
                }
            )
        }

        if (totalPercentage < 90.0) {
            throw GradleException(
                "Root commonMain branch coverage is below 90%%: %.2f%% (%d/%d branches)".format(
                    totalPercentage,
                    totalCovered,
                    totalBranches,
                )
            )
        }
    }
}
