package org.mifos.feature.task.data.datasource.api.service

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.PUT
import org.mifos.feature.task.data.datasource.api.model.TopicSaveReqVOApiModel
import template.core.base.network.data.ApiResult
import template.core.base.network.data.CommonResult

internal interface TaskApiService {
    @PUT("/admin-api/ai/topic/update")
    suspend fun topicUpdateTopic(

        @Body body: TopicSaveReqVOApiModel
    ): ApiResult<CommonResult<Boolean>>

}
