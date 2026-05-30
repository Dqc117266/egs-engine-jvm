package com.dqc.example.feature.task.domain.repository

import com.dqc.example.feature.base.data.retrofit.toResult
import com.dqc.example.feature.base.domain.result.Result
import com.dqc.example.feature.task.data.datasource.api.model.toData
import com.dqc.example.feature.task.data.datasource.api.service.TaskRetrofitService
import com.dqc.example.feature.task.domain.model.TopicSaveReqVO
import com.dqc.example.feature.task.domain.repository.TaskRepository

internal class TaskRepositoryImpl(
    private val service: TaskRetrofitService,
) : TaskRepository {
    override suspend fun topicUpdateTopic(

        body: TopicSaveReqVO
    ): Result<Boolean> {
        return service.topicUpdateTopic(body.toData()).toResult()
    }

}
