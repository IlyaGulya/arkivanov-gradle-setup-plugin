package com.arkivanov.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import kotlin.reflect.KClass

internal inline fun <reified T : Any> ExtensionContainer.with(block: T.() -> Unit) {
    getByType<T>().apply(block)
}

internal fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.with(block)
}

internal fun KotlinSourceSetContainer.sourceSets(block: NamedDomainObjectContainer<KotlinSourceSet>.() -> Unit) {
    sourceSets.block()
}

internal inline fun <reified T : Target> Project.doIfTargetEnabled(block: (T) -> Unit) {
    getEnabledTarget<T>()?.also(block)
}

internal inline fun <reified T : Target> Project.getEnabledTarget(): T? =
    enabledTargets
        .filterIsInstance<T>()
        .singleOrNull()

internal inline fun <reified T : Target> Project.isTargetEnabled(): Boolean =
    isTargetEnabled(T::class)

internal fun Project.isTargetEnabled(clazz: KClass<out Target>): Boolean =
    enabledTargets.any { it::class == clazz }

@Suppress("UNCHECKED_CAST")
internal var Project.enabledTargets: List<Target>
    get() = extra.get("enabled_targets") as List<Target>
    set(value) {
        check(!extra.has("enabled_targets")) { "Targets can be enabled only once" }
        extra.set("enabled_targets", value)
    }

internal fun KotlinTarget.disableCompilationsIfNeeded() {
    if (!isCompilationAllowed) {
        disableCompilations()
    }
}

internal fun KotlinTarget.disableCompilations() {
    compilations.configureEach {
        compileKotlinTask.enabled = false
    }
}

internal val KotlinTarget.isCompilationAllowed: Boolean
    get() =
        (name == KotlinMultiplatformPlugin.METADATA_TARGET_NAME) ||
            isTargetCompilationAllowed(targetClass ?: error("No target class found for $this"))

private val KotlinTarget.targetClass: KClass<out Target>?
    get() =
        when (platformType) {
            KotlinPlatformType.androidJvm -> Target.Android::class
            KotlinPlatformType.jvm -> Target.Jvm::class
            KotlinPlatformType.js -> Target.Js::class

            KotlinPlatformType.native ->
                Target::class.sealedSubclasses.find { clazz ->
                    name.startsWith(prefix = requireNotNull(clazz.simpleName), ignoreCase = true)
                }

            KotlinPlatformType.common,
            KotlinPlatformType.wasm -> null
        }

internal inline fun <reified T : Target> isTargetCompilationAllowed(): Boolean = isTargetCompilationAllowed(T::class)

internal fun isTargetCompilationAllowed(clazz: KClass<out Target>): Boolean {
    if (!EnvParams.splitTargets) {
        return true
    }

    val os = OperatingSystem.current()

    return when {
        os.isLinux -> clazz in Target.LINUX_SPLIT_CLASSES
        os.isMacOsX -> clazz in Target.MACOS_SPLIT_CLASSES
//        os.isLinux -> clazz in Target.MACOS_SPLIT_CLASSES
//        os.isMacOsX -> clazz in Target.LINUX_SPLIT_CLASSES
        else -> error("Unsupported OS type: $os")
    }
}
