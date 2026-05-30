package org.mifos.feature.task.domain.repository

import org.mifos.feature.task.domain.model.TopicSaveReqVO
import template.core.base.network.domain.Result

internal interface TaskRepository {
    suspend fun topicUpdateTopic(body: TopicSaveReqVO): Result<Boolean>

}
