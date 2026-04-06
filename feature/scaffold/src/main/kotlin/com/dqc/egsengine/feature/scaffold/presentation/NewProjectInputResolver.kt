package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.NewProjectTemplateUrls
import java.io.File

internal object NewProjectInputResolver {

    fun resolveProjectName(projectNameArg: String?): String =
        projectNameArg?.trim()?.takeIf { it.isNotBlank() }
            ?: run {
                print("? Project Name: ")
                val value = readlnOrNull()?.trim().orEmpty()
                require(value.isNotBlank()) { "Project name cannot be empty" }
                value
            }

    fun resolvePackageName(packageNameOption: String?): String =
        packageNameOption?.trim()?.takeIf { it.isNotBlank() }
            ?: run {
                val defaultPkg = "com.dqc.example"
                print("? Package Name [$defaultPkg]: ")
                val value = readlnOrNull()?.trim().orEmpty()
                value.ifBlank { defaultPkg }
            }

    fun resolveClientRaw(clientOption: String?): String =
        clientOption?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: run {
                print("? Client Platform (kmp/android/none) [kmp]: ")
                readlnOrNull()?.trim()?.lowercase()?.ifBlank { "kmp" } ?: "kmp"
            }

    fun resolveIncludeBackend(backendOption: String?): Boolean =
        backendOption?.trim()?.takeIf { it.isNotBlank() }?.let { parseBoolFlag(it) }
            ?: promptYesNo("? Include Backend", defaultYes = true)

    fun resolveIncludeWeb(webOption: String?): Boolean =
        webOption?.trim()?.takeIf { it.isNotBlank() }?.let { parseBoolFlag(it) }
            ?: promptYesNo("? Include Web Admin", defaultYes = true)

    fun resolveClientPlatform(clientRaw: String): Platform? =
        when (clientRaw) {
            "kmp" -> Platform.KMP
            "android" -> Platform.ANDROID
            "none" -> null
            else -> throw IllegalArgumentException("Unsupported client: $clientRaw (use: kmp, android, none)")
        }

    fun defaultClientTemplateUrl(platform: Platform): String? =
        when (platform) {
            Platform.KMP, Platform.KMP_ANDROID -> NewProjectTemplateUrls.KMP
            Platform.ANDROID -> NewProjectTemplateUrls.ANDROID
            else -> null
        }

    fun validateProjectName(name: String) {
        require(Regex("^[A-Za-z][A-Za-z0-9_-]*$").matches(name)) {
            "Invalid project name: $name (must start with letter, only [A-Za-z0-9_-])"
        }
    }

    fun validatePackageName(name: String) {
        require(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$").matches(name)) {
            "Invalid package name: $name (example: com.dqc.notes)"
        }
    }

    fun ensureTargetDirectory(targetDir: File) {
        if (!targetDir.exists()) return
        require(targetDir.isDirectory) { "Target path exists and is not a directory: ${targetDir.absolutePath}" }
        require(targetDir.listFiles().isNullOrEmpty()) { "Target directory is not empty: ${targetDir.absolutePath}" }
    }

    private fun parseBoolFlag(s: String): Boolean =
        when (s.lowercase()) {
            "true", "yes", "y", "1" -> true
            "false", "no", "n", "0" -> false
            else -> throw IllegalArgumentException("Invalid boolean: $s (use true/false or y/n)")
        }

    private fun promptYesNo(message: String, defaultYes: Boolean): Boolean {
        val hint = if (defaultYes) "(Y/n)" else "(y/N)"
        print("$message $hint: ")
        val v = readlnOrNull()?.trim()?.lowercase()
        return when {
            v.isNullOrBlank() -> defaultYes
            v == "y" || v == "yes" -> true
            v == "n" || v == "no" -> false
            else -> defaultYes
        }
    }
}
