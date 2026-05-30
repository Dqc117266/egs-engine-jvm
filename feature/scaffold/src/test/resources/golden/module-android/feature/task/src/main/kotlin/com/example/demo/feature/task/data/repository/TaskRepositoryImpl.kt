package com.example.demo.feature.task.data.repository

/*
 * egs-codegen: scaffold-repository-impl -- after `client api sync` this class extends Generated*RepositorySupport.
 * Add // egs-sync:freeze on its own line to prevent api sync from overwriting this file.
 */
import com.example.demo.feature.task.domain.repository.TaskRepository
import com.example.demo.feature.base.domain.result.Result

internal class TaskRepositoryImpl(
) : TaskRepository {

    override suspend fun getData(): Result<String> {
        TODO("Not yet implemented")
    }
}
