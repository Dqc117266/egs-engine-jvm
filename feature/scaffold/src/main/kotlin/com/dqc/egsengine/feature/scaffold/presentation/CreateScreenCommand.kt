package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Create Screen 命令 - 完整版
 *
 * 支持参数:
 *   egs create screen Login \
 *     --module user \
 *     --usecase DefaultAi4043UseCase,TopicCreateTopicUseCase \
 *     --route "user/login" \
 *     --params "email:String,password:String"
 *
 * 简写:
 *   egs create screen Login -m user -u DefaultAi4043UseCase,TopicCreateTopicUseCase
 */
class CreateScreenCommand : CliktCommand(name = "screen"), KoinComponent {

    private val pageScaffolder: PageScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    // 位置参数：Screen 名称（必需）
    private val screenName by argument(
        name = "NAME",
        help = "Screen 名称，例如: Login, Profile, Settings"
    )

    // 目标模块（必需）
    private val module by option(
        "-m", "--module",
        help = "目标模块名称，例如: user, home, profile"
    )

    // 关联的 UseCase（逗号分隔）
    private val useCases by option(
        "-u", "--usecase",
        help = "关联的 UseCase，多个用逗号分隔，例如: GetUserUseCase,UpdateUserUseCase"
    )

    // 导航路由路径（可选）
    private val route by option(
        "-r", "--route",
        help = "导航路由路径，例如: user/login, profile/{id}"
    )

    // Screen 参数（可选，格式：name:Type,name2:Type）
    private val params by option(
        "-p", "--params",
        help = "Screen 参数，格式：name:Type，多个用逗号分隔，例如: email:String,password:String"
    )

    // 项目路径
    private val projectPath by option(
        "--project",
        help = "目标项目路径"
    ).default(".")

    // 预览模式
    private val dryRun by option(
        "--dry-run",
        help = "预览模式，不实际创建文件"
    ).flag()

    override fun run() {
        try {
            val projectRoot = ProjectRootResolver.resolve(projectPath)

            // 如果提供了所有必需参数，使用命令行模式
            if (module != null) {
                runCommandMode(projectRoot)
            } else {
                // 否则使用交互式模式
                runInteractiveMode(projectRoot)
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "参数错误"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("生成 Screen 失败: ${e.message}"), err = true)
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
        val name = validateScreenName(screenName)

        // 验证模块存在
        val modules = useCaseScanner.listModules(projectRoot)
        require(modules.contains(targetModule)) {
            "模块 '$targetModule' 不存在。可用模块: ${modules.joinToString(", ")}"
        }

        // 解析 UseCase
        val allUseCases = useCaseScanner.scanByModule(projectRoot, targetModule)
        val selectedUseCases = parseUseCases(useCases, allUseCases, targetModule)

        // 解析参数
        val screenParams = parseParams(params)

        // 显示生成信息
        echo()
        echo(CliFormatter.formatInfo("📋 生成信息:"))
        echo("   Screen 名称: $name")
        echo("   目标模块: $targetModule")
        echo("   UseCases: ${selectedUseCases.joinToString(", ") { it.name }.ifEmpty { "无" }}")
        if (route != null) echo("   导航路由: $route")
        if (screenParams.isNotEmpty()) echo("   参数: ${screenParams.joinToString(", ") { "${it.first}: ${it.second}" }}")
        echo()

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
        echo(CliFormatter.formatInfo("🚀 创建新 Screen"))
        echo()

        // 1. 确认 Screen 名称
        echo("📱 Screen 名称: $screenName")
        val name = validateScreenName(screenName)
        echo()

        // 2. 选择模块
        val modules = useCaseScanner.listModules(projectRoot)
        if (modules.isEmpty()) {
            throw IllegalArgumentException("未找到任何功能模块，请先创建模块")
        }

        echo("📦 选择目标模块:")
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

        // 3. 选择 UseCase（可选）
        val availableUseCases = useCaseScanner.scanByModule(projectRoot, selectedModule)
        val selectedUseCases = if (availableUseCases.isNotEmpty()) {
            selectUseCasesInteractively(availableUseCases)
        } else {
            echo("⚠️  模块 '$selectedModule' 中未找到 UseCase")
            emptyList()
        }

        // 4. 输入路由（可选）
        echo()
        print("> 输入导航路由路径 (可选，如: user/login): ")
        val routePath = readlnOrNull()?.trim()?.takeIf { it.isNotEmpty() }

        // 5. 确认生成
        echo()
        echo(CliFormatter.formatInfo("📋 生成信息:"))
        echo("   Screen 名称: $name")
        echo("   目标模块: $selectedModule")
        echo("   UseCases: ${selectedUseCases.joinToString(", ") { it.name }.ifEmpty { "无" }}")
        if (routePath != null) echo("   导航路由: $routePath")
        echo()

        print("> 确认生成? [Y/n]: ")
        val confirm = readLine()?.trim()?.lowercase() ?: "y"

        if (confirm == "n" || confirm == "no") {
            echo(CliFormatter.formatInfo("已取消"))
            return
        }

        // 6. 执行生成
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
     * 验证 Screen 名称
     */
    private fun validateScreenName(name: String): String {
        require(name.isNotBlank()) { "Screen 名称不能为空" }
        require(name.first().isLetter() && name.all { it.isLetterOrDigit() }) {
            "Screen 名称必须以字母开头，只能包含字母和数字（支持 camelCase/PascalCase）"
        }
        return name.replaceFirstChar { it.uppercase() }
    }

    /**
     * 解析 UseCase 字符串
     */
    private fun parseUseCases(
        useCaseStr: String?,
        allUseCases: List<UseCaseInfo>,
        moduleName: String
    ): List<UseCaseInfo> {
        if (useCaseStr.isNullOrBlank()) return emptyList()

        val names = useCaseStr.split(",").map { it.trim() }
        return names.map { name ->
            allUseCases.find { it.name == name || it.name == "${name}UseCase" }
                ?: throw IllegalArgumentException("UseCase '$name' 在模块 '$moduleName' 中未找到")
        }
    }

    /**
     * 解析参数字符串
     */
    private fun parseParams(paramsStr: String?): List<Pair<String, String>> {
        if (paramsStr.isNullOrBlank()) return emptyList()

        return paramsStr.split(",").map { param ->
            val parts = param.trim().split(":")
            require(parts.size == 2) { "参数格式错误: $param，应为 name:Type" }
            parts[0].trim() to parts[1].trim()
        }
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
            echo(CliFormatter.formatSuccess("✅ Screen 创建成功!"))
            echo()
            echo("📁 生成的文件:")
            result.files.forEach { file ->
                echo("   ${CliFormatter.green("CREATE")} ${file.path}")
            }
            echo()
            echo("📝 下一步:")
            echo("   1. 在 Screen 文件中添加 UI 组件")
            echo("   2. 在 Contract 中定义 State 数据")
            echo("   3. 在 ViewModel 中实现业务逻辑")
            echo("   4. 在 NavigationRoute 中添加路由")
        }
    }
}
