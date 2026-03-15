package com.ajd.egsengine.feature.task.domain.model

data class AutomationTask(
    val id: String,
    val name: String,
    val description: String = "",
    val commands: List<String> = emptyList(),
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
}
