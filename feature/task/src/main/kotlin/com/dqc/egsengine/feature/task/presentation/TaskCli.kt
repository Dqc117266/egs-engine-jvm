package com.dqc.egsengine.feature.task.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.task.domain.TaskScheduler
import com.dqc.egsengine.feature.task.domain.model.AutomationTask
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.koin.core.component.inject
import java.util.UUID

class TaskCommand : EgsCliCommand(name = "task") {
    init {
        subcommands(TaskList(), TaskAdd(), TaskCancel())
    }

    override fun runCommand() = Unit
}

private class TaskList : EgsCliCommand(name = "list") {

    private val taskScheduler: TaskScheduler by inject()

    override fun runCommand() {
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

private class TaskAdd : EgsCliCommand(name = "add") {

    private val taskScheduler: TaskScheduler by inject()
    private val taskName by argument()
    private val commands by argument().multiple()

    override fun runCommand() {
        val task = AutomationTask(
            id = UUID.randomUUID().toString().take(8),
            name = taskName,
            commands = commands,
        )

        taskScheduler.scheduleTask(task)
        echo(CliFormatter.formatSuccess("Task '${task.name}' added with ID: ${task.id}"))
    }
}

private class TaskCancel : EgsCliCommand(name = "cancel") {

    private val taskScheduler: TaskScheduler by inject()
    private val taskId by argument()

    override fun runCommand() {
        val success = taskScheduler.cancelTask(taskId)
        if (success) {
            echo(CliFormatter.formatSuccess("Task '$taskId' cancelled"))
        } else {
            require(false) { "Failed to cancel task: $taskId" }
        }
    }
}
