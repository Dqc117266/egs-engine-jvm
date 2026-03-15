package com.ajd.egsengine.feature.task.presentation

import com.ajd.egsengine.feature.base.presentation.CliFormatter
import com.ajd.egsengine.feature.task.domain.TaskScheduler
import com.ajd.egsengine.feature.task.domain.model.AutomationTask
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class TaskCommand : CliktCommand(name = "task") {
    init {
        subcommands(TaskList(), TaskAdd(), TaskCancel())
    }

    override fun run() = Unit
}

private class TaskList : CliktCommand(name = "list"), KoinComponent {

    private val taskScheduler: TaskScheduler by inject()

    override fun run() {
        val tasks = taskScheduler.getPendingTasks()
        if (tasks.isEmpty()) {
            echo(CliFormatter.formatInfo("No pending tasks"))
            return
        }

        val table = CliFormatter.formatTable(
            headers = listOf("ID", "Name", "Status", "Commands"),
            rows = tasks.map { listOf(it.id, it.name, it.status.name, it.commands.size.toString()) },
        )
        echo(table)
    }
}

private class TaskAdd : CliktCommand(name = "add"), KoinComponent {

    private val taskScheduler: TaskScheduler by inject()
    private val taskName by argument()
    private val commands by argument().multiple()

    override fun run() {
        val task = AutomationTask(
            id = UUID.randomUUID().toString().take(8),
            name = taskName,
            commands = commands,
        )

        taskScheduler.scheduleTask(task)
        echo(CliFormatter.formatSuccess("Task '${task.name}' added with ID: ${task.id}"))
    }
}

private class TaskCancel : CliktCommand(name = "cancel"), KoinComponent {

    private val taskScheduler: TaskScheduler by inject()
    private val taskId by argument()

    override fun run() {
        val success = taskScheduler.cancelTask(taskId)
        if (success) {
            echo(CliFormatter.formatSuccess("Task '$taskId' cancelled"))
        } else {
            echo(CliFormatter.formatError("Failed to cancel task: $taskId"), err = true)
        }
    }
}
