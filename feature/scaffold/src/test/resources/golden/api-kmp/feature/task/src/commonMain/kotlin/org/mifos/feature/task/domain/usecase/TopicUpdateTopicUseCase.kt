package org.mifos.feature.task.domain.usecase

import org.mifos.feature.task.domain.model.TopicSaveReqVO
import org.mifos.feature.task.domain.repository.TaskRepository
import template.core.base.network.domain.Result

internal class TopicUpdateTopicUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(body: TopicSaveReqVO): Result<Boolean> {
        return repository.topicUpdateTopic(body)
    }
}
