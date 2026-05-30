package org.mifos.feature.task.domain.repository

import org.mifos.feature.task.data.datasource.api.model.toData
import org.mifos.feature.task.data.datasource.api.service.TaskApiService
import org.mifos.feature.task.domain.model.TopicSaveReqVO
import org.mifos.feature.task.domain.repository.TaskRepository
import template.core.base.network.data.toResult
import template.core.base.network.domain.Result

internal class TaskApiRepositorySupport(
    private val service: TaskApiService,
) : TaskRepository {
    override suspend fun topicUpdateTopic(

        body: TopicSaveReqVO
    ): Result<Boolean> {
        return service.topicUpdateTopic(body.toData()).toResult()
    }

}
