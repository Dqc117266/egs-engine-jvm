package com.example.demo.feature.task.domain.repository

import com.example.demo.feature.base.domain.result.Result

internal interface TaskRepository {
    suspend fun getData(): Result<String>
}
