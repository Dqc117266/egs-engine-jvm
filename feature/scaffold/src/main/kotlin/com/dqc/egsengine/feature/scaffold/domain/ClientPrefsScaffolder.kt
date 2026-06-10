/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import java.io.File

/**
 * Routes `client gen prefs` to [KmpPreferencesScaffolder] or [AndroidPreferencesScaffolder] by workspace client platform.
 */
class ClientPrefsScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val kmpPreferencesScaffolder: KmpPreferencesScaffolder,
    private val androidPreferencesScaffolder: AndroidPreferencesScaffolder,
) {
    fun scaffoldPrefs(
        projectRoot: File,
        moduleName: String,
        fieldsArg: String,
        keyArg: String?,
        dryRun: Boolean,
        force: Boolean,
    ): KmpPreferencesScaffolder.KmpPreferencesScaffoldResult {
        val platform = workspaceConfigResolver.resolveClient(projectRoot).platform
        return when (platform) {
            Platform.ANDROID ->
                androidPreferencesScaffolder.scaffoldPrefs(
                    projectRoot = projectRoot,
                    moduleName = moduleName,
                    fieldsArg = fieldsArg,
                    keyArg = keyArg,
                    dryRun = dryRun,
                    force = force,
                )
            Platform.KMP, Platform.KMP_ANDROID ->
                kmpPreferencesScaffolder.scaffoldPrefs(
                    projectRoot = projectRoot,
                    moduleName = moduleName,
                    fieldsArg = fieldsArg,
                    keyArg = keyArg,
                    dryRun = dryRun,
                    force = force,
                )
            else ->
                error(
                    "client gen prefs requires workspace project 'client' with platform kmp, kmp_android, or android; got $platform",
                )
        }
    }
}
