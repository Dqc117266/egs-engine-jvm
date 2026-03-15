package com.ajd.egsengine.feature.task.domain

import com.ajd.egsengine.feature.task.data.TaskRepository
import com.ajd.egsengine.feature.task.domain.model.AutomationTask
import com.ajd.egsengine.feature.task.domain.model.TaskStatus
import org.slf4j.LoggerFactory

class TaskScheduler(
    private val taskRepository: TaskRepository,
) {
    private val logger = LoggerFactory.getLogger(TaskScheduler::class.java)

    fun scheduleTask(task: AutomationTask) {
        taskRepository.addTask(task)
        logger.info("Task scheduled: ${task.name}")
    }

    fun cancelTask(taskId: String): Boolean {
        val task = taskRepository.getTask(taskId) ?: return false
        if (task.status == TaskStatus.RUNNING) {
            logger.warn("Cannot cancel a running task: $taskId")
            return false
        }
        taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
        return true
    }

    fun getPendingTasks(): List<AutomationTask> =
        taskRepository.getAllTasks().filter { it.status == TaskStatus.PENDING }

    fun getTaskStatus(taskId: String): TaskStatus? =
        taskRepository.getTask(taskId)?.status
}
