package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 创建页面命令 - 支持交互式和命令行两种模式
 *
 * 方式1: 交互式
 *   egs create page
 *   → 选择模块
 *   → 输入页面名称
 *   → 选择 UseCase
 *
 * 方式2: 命令行参数
 *   egs create page --module home --name Profile --api GetUserPostsUseCase --api GetUserLevelUseCase
 */
class CreatePageCommand : CliktCommand(name = "page"), KoinComponent {

    private val pageScaffolder: PageScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    private val module by option(
        "-m", "--module",
        help = "目标模块名称，例如: home"
    )

    private val pageName by option(
        "-n", "--name",
        help = "页面名称，例如: Profile"
    )

    private val apis by option(
        "-a", "--api",
        help = "关联的 UseCase，可多次使用"
    ).multiple()

    private val projectPath by option(
        "-p", "--project",
        help = "目标项目路径"
    ).default(".")

    private val dryRun by option(
        "--dry-run",
        help = "预览模式，不实际创建文件"
    ).flag()

    override fun run() {
        try {
            val projectRoot = ProjectRootResolver.resolve(projectPath)

            // 如果提供了所有必需参数，使用命令行模式
            if (module != null && pageName != null) {
                runCommandMode(projectRoot)
            } else {
                // 否则使用交互式模式
                runInteractiveMode(projectRoot)
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "参数错误"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("生成页面失败: ${e.message}"), err = true)
            if (System.getenv("EGS_DEBUG") == "true") {
                e.printStackTrace()
            }
        }
    }

    /**
     * 命令行模式
     */
    private fun runCommandMode(projectRoot: java.io.File) {
        val targetModule = module!!
        val name = pageName!!

        // 验证模块存在
        val modules = useCaseScanner.listModules(projectRoot)
        require(modules.contains(targetModule)) {
            "模块 '$targetModule' 不存在。可用模块: ${modules.joinToString(", ")}"
        }

        // 查找 UseCase
        val allUseCases = useCaseScanner.scanByModule(projectRoot, targetModule)
        val selectedUseCases = if (apis.isNotEmpty()) {
            apis.map { apiName ->
                allUseCases.find { it.name == apiName || it.name == "${apiName}UseCase" }
                    ?: throw IllegalArgumentException("UseCase '$apiName' 在模块 '$targetModule' 中未找到")
            }
        } else {
            emptyList()
        }

        // 执行生成
        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = targetModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
        )

        printResult(result)
    }

    /**
     * 交互式模式
     */
    private fun runInteractiveMode(projectRoot: java.io.File) {
        echo(CliFormatter.formatInfo("🚀 创建新页面"))
        echo()

        // 1. 选择模块
        val modules = useCaseScanner.listModules(projectRoot)
        if (modules.isEmpty()) {
            throw IllegalArgumentException("未找到任何功能模块，请先创建模块")
        }

        echo("📦 选择所属模块:")
        modules.forEachIndexed { index, m ->
            echo("  [$index] $m")
        }
        echo()

        print("> 请输入模块编号: ")
        val moduleIndex = readlnOrNull()?.toIntOrNull()
            ?: throw IllegalArgumentException("无效的模块选择")
        require(moduleIndex in modules.indices) { "无效的模块编号" }

        val selectedModule = modules[moduleIndex]
        echo()

        // 2. 输入页面名称
        print("> 输入页面名称 (如: Profile): ")
        val name = readlnOrNull()?.trim()
            ?: throw IllegalArgumentException("页面名称不能为空")
        require(name.isNotBlank()) { "页面名称不能为空" }
        require(name.first().isLetter() && name.all { it.isLetterOrDigit() }) {
            "页面名称必须以字母开头，只能包含字母和数字（支持 camelCase 如 taskDetail）"
        }
        echo()

        // 3. 选择 UseCase
        val availableUseCases = useCaseScanner.scanByModule(projectRoot, selectedModule)
        val selectedUseCases = if (availableUseCases.isNotEmpty()) {
            selectUseCasesInteractively(availableUseCases)
        } else {
            echo("⚠️  模块 '$selectedModule' 中未找到 UseCase")
            emptyList()
        }

        // 4. 确认生成
        echo()
        echo("📋 生成信息:")
        echo("   模块: $selectedModule")
        echo("   页面: $name")
        echo("   UseCase: ${if (selectedUseCases.isEmpty()) "无" else selectedUseCases.joinToString(", ") { it.name }}")
        echo()

        print("> 确认生成? [Y/n]: ")
        val confirm = readLine()?.trim()?.lowercase() ?: "y"

        if (confirm == "n" || confirm == "no") {
            echo(CliFormatter.formatInfo("已取消"))
            return
        }

        // 5. 执行生成
        echo()
        echo(CliFormatter.formatInfo("⏳ 正在生成..."))

        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = selectedModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
        )

        printResult(result)
    }

    /**
     * 交互式选择 UseCase
     */
    private fun selectUseCasesInteractively(useCases: List<UseCaseInfo>): List<UseCaseInfo> {
        echo("🔌 选择需要关联的 UseCase (多选，用逗号分隔):")
        useCases.forEachIndexed { index, uc ->
            echo("  [$index] ${uc.name}")
        }
        echo("  [a] 全部选中")
        echo("  [回车] 跳过")
        echo()

        print("> 选择 (如: 0,2 或 a): ")
        val input = readLine()?.trim() ?: ""

        return when {
            input.isEmpty() -> emptyList()
            input == "a" || input == "all" -> useCases
            else -> {
                val indices = input.split(",", " ")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in useCases.indices }
                indices.map { useCases[it] }
            }
        }
    }

    /**
     * 打印结果
     */
    private fun printResult(result: com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult) {
        echo()

        if (result.dryRun) {
            echo(CliFormatter.formatInfo("🔍 Dry run - 将要创建以下文件:"))
            echo()
            result.files.forEach { file ->
                echo("   CREATE: ${file.path}")
            }
            echo()
            echo(CliFormatter.formatInfo("(使用 --dry-run 参数预览，实际运行时不加此参数)"))
        } else {
            echo(CliFormatter.formatSuccess("✅ 页面创建成功!"))
            echo()
            echo("📁 生成的文件:")
            result.files.forEach { file ->
                echo("   ${CliFormatter.green("CREATE")} ${file.path}")
            }
            echo()
            echo("📝 下一步:")
            echo("   1. 在布局文件中添加 UI 组件")
            echo("   2. 在 renderState() 方法中绑定数据")
            echo("   3. 在 handleEffect() 方法中处理副作用")
            echo("   4. 在 Navigation Graph 中添加页面路由")
        }
    }
}
