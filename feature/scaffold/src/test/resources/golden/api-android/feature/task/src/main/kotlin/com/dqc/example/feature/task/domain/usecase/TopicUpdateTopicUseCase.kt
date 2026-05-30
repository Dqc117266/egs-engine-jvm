package com.dqc.example.feature.task.domain.usecase

import com.dqc.example.feature.base.domain.result.Result
import com.dqc.example.feature.task.domain.model.TopicSaveReqVO
import com.dqc.example.feature.task.domain.repository.TaskRepository

internal class TopicUpdateTopicUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(body: TopicSaveReqVO): Result<Boolean> {
        return repository.topicUpdateTopic(body)
    }
}
