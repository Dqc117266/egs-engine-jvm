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
 * Routes `client gen database` to [KmpDatabaseScaffolder] or [AndroidDatabaseScaffolder] by workspace client platform.
 */
class ClientDatabaseScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val kmpDatabaseScaffolder: KmpDatabaseScaffolder,
    private val androidDatabaseScaffolder: AndroidDatabaseScaffolder,
) {
    fun scaffoldDatabase(
        projectRoot: File,
        sqlFile: File,
        moduleName: String,
        dryRun: Boolean = false,
        repo: Boolean = false,
        cached: Boolean = false,
    ): KmpDatabaseScaffolder.KmpDatabaseScaffoldResult {
        val platform = workspaceConfigResolver.resolveClient(projectRoot).platform
        return when (platform) {
            Platform.ANDROID -> androidDatabaseScaffolder.scaffoldDatabase(
                projectRoot = projectRoot,
                sqlFile = sqlFile,
                moduleName = moduleName,
                dryRun = dryRun,
                repo = repo,
                cached = cached,
            )
            Platform.KMP, Platform.KMP_ANDROID -> kmpDatabaseScaffolder.scaffoldDatabase(
                projectRoot = projectRoot,
                sqlFile = sqlFile,
                moduleName = moduleName,
                dryRun = dryRun,
                repo = repo,
                cached = cached,
            )
            else -> error(
                "client gen database requires workspace project 'client' with platform kmp, kmp_android, or android; got $platform",
            )
        }
    }
}
