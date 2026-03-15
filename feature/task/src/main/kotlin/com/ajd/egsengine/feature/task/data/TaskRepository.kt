package com.ajd.egsengine.feature.task.data

import com.ajd.egsengine.feature.task.domain.model.AutomationTask
import com.ajd.egsengine.feature.task.domain.model.TaskStatus
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

interface TaskRepository {
    fun addTask(task: AutomationTask)
    fun getTask(id: String): AutomationTask?
    fun getAllTasks(): List<AutomationTask>
    fun updateTaskStatus(id: String, status: TaskStatus)
    fun removeTask(id: String)
}

class TaskRepositoryImpl : TaskRepository {
    private val logger = LoggerFactory.getLogger(TaskRepositoryImpl::class.java)
    private val tasks = ConcurrentHashMap<String, AutomationTask>()

    override fun addTask(task: AutomationTask) {
        tasks[task.id] = task
        logger.debug("Task added: ${task.id} - ${task.name}")
    }

    override fun getTask(id: String): AutomationTask? = tasks[id]

    override fun getAllTasks(): List<AutomationTask> = tasks.values.toList()

    override fun updateTaskStatus(id: String, status: TaskStatus) {
        tasks.computeIfPresent(id) { _, task -> task.copy(status = status) }
    }

    override fun removeTask(id: String) {
        tasks.remove(id)
        logger.debug("Task removed: $id")
    }
}
