package com.dqc.example.feature.task.data.datasource.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dqc.example.feature.task.domain.model.TopicSaveReqVO

@Serializable
data class TopicSaveReqVOApiModel(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
)

internal fun TopicSaveReqVOApiModel.toDomain(): TopicSaveReqVO = TopicSaveReqVO(
    id = this.id,
    name = this.name,
)

internal fun TopicSaveReqVO.toData(): TopicSaveReqVOApiModel = TopicSaveReqVOApiModel(
    id = this.id,
    name = this.name,
)
