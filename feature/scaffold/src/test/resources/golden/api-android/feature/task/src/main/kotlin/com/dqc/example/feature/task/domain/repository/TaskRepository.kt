package com.dqc.example.feature.task.domain.repository

import com.dqc.example.feature.base.domain.result.Result
import com.dqc.example.feature.task.domain.model.TopicSaveReqVO

internal interface TaskRepository {
    suspend fun topicUpdateTopic(body: TopicSaveReqVO): Result<Boolean>

}
