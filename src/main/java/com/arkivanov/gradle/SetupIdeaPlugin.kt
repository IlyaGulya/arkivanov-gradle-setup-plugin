package com.arkivanov.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.withType
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.setupIdeaPlugin(
    group: String,
    version: String,
    sinceBuild: String,
    intellijVersion: String,
) {
    this.group = group
    this.version = version

    extensions.with<KotlinJvmProjectExtension> {
        target.disableCompilationsIfNeeded()

        tasks.withType<BuildSearchableOptionsTask>().configureEach {
            enabled = target.isCompilationAllowed
        }
    }

    extensions.with<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    tasks.withType<PatchPluginXmlTask>().configureEach {
        sinceBuild(sinceBuild)
    }

    extensions.with<IntelliJPluginExtension> {
        this.version = intellijVersion
        updateSinceUntilBuild = false
    }
}
