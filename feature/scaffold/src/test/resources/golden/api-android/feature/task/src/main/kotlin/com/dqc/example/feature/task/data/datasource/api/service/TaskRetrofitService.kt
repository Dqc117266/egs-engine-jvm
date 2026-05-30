package com.dqc.example.feature.task.data.datasource.api.service

import com.dqc.example.feature.base.data.retrofit.ApiResult
import com.dqc.example.feature.base.data.retrofit.CommonResult
import com.dqc.example.feature.task.data.datasource.api.model.TopicSaveReqVOApiModel
import retrofit2.http.Body
import retrofit2.http.PUT

internal interface TaskRetrofitService {
    @PUT("/admin-api/ai/topic/update")
    suspend fun topicUpdateTopic(

        @Body body: TopicSaveReqVOApiModel
    ): ApiResult<CommonResult<Boolean>>

}
