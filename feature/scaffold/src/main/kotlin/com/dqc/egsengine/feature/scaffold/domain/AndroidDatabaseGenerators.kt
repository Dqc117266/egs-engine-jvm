package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiDbRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDbOnlyRepositoryImplGenerator

data class AndroidDatabaseGenerators(
    val codeGenerator: AndroidDatabaseCodeGenerator,
    val entityMapperGenerator: AndroidDatabaseEntityMapperGenerator,
    val repositoryGenerator: AndroidDatabaseRepositoryGenerator,
    val useCaseGenerator: AndroidDatabaseUseCaseGenerator,
    val combinedRepositoryGenerator: AndroidCombinedRepositoryGenerator,
    val dbOnlyRepositoryImplGenerator: AndroidDbOnlyRepositoryImplGenerator,
    val apiDbRepositoryImplGenerator: AndroidApiDbRepositoryImplGenerator,
)
