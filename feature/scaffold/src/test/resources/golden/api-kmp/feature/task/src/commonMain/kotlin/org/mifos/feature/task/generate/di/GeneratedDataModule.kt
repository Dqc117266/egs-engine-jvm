package org.mifos.feature.task.generate.di

import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.mifos.feature.task.data.repository.TaskApiRepositorySupport
import org.mifos.feature.task.data.datasource.api.service.createTaskApiService

internal val generatedDataModule = module {
    single { get<Ktorfit>().createTaskApiService() }

    singleOf(::TaskApiRepositorySupport)

    // egs-gen:database-begin

    // egs-gen:database-end

    // egs-gen:prefs-begin

    // egs-gen:prefs-end
}
