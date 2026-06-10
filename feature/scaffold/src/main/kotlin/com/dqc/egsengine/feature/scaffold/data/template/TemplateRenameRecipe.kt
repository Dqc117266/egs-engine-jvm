package com.dqc.egsengine.feature.scaffold.data.template

/**
 * Recipe for renaming a cloned template's package / project-name tokens.
 * Any field left null is skipped during rewriting.
 */
data class TemplateRenameRecipe(
    val id: String,
    val oldPackage: String? = null,
    val oldProjectName: String? = null,
    val oldProjectNameDisplay: String? = null,
    val oldPackageToken: String? = null,
)

object TemplateRenameRecipes {
    val ANDROID: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "android",
            oldPackage = "com.example.egs_android_template",
            oldProjectName = "egs-android-template",
            oldProjectNameDisplay = "EGS-Android-Template",
            oldPackageToken = "egs_android_template",
        )

    val KMP: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "kmp",
            oldPackage = "org.mifos",
            oldProjectName = "egs-kmp-template",
            oldProjectNameDisplay = "egs-kmp-template",
        )

    val SERVER: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "server",
            oldPackage = "com.egs.server",
            oldProjectName = "egs-server-template",
            oldProjectNameDisplay = "egs-server-template",
            oldPackageToken = "egs_server",
        )

    val ADMIN: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "admin",
            oldProjectName = "egs-admin-template",
            oldProjectNameDisplay = "egs-admin-template",
        )

    fun detectFromPath(path: String): TemplateRenameRecipe? {
        val normalized = path.lowercase().replace('\\', '/')
        return when {
            "egs-android-template" in normalized -> ANDROID
            "egs-kmp-template" in normalized -> KMP
            "egs-server-template" in normalized -> SERVER
            "egs-admin-template" in normalized -> ADMIN
            else -> null
        }
    }
}
