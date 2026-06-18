# egs-engine 第二阶段优化计划（P1/P2 全量任务）

> 分支 `refactor/cli-and-architecture` 已完成全部 P0（8 提交，CLI 错误模型 + 命令迁移 + Koin 静默 + script run/lint fix + task 移除 + TemplatePackageRewriter 合并 + ProcessBuilder 统一）。
> 本文档列出剩余 **14 项 P1 + 6 项 P2** 任务，按执行顺序分 5 批，供接力执行。

---

## 前置信息（给接力 AI）

### 项目位置与构建命令
```
项目根目录: /Volumes/Expend/egs/egs-engine
工作分支: refactor/cli-and-architecture（基于 dev）
构建: ./gradlew compileKotlin
测试: ./gradlew test
静态: ./gradlew detektCheck spotlessCheck
格式: ./gradlew spotlessApply
打包: ./gradlew shadowJar
冒烟: java -jar app/build/libs/app-all.jar --help
```

### 已建立的 P0 基础设施（不要破坏）
- `EgsCliCommand`（feature/base/presentation/EgsCliCommand.kt）：所有命令基类，catch Throwable → ProgramResult(非零)。子类实现 `runCommand()`，不要写自己的 try/catch。
- `CliError`（feature/base/presentation/CliError.kt）：UsageError(exit2) / GenerationError / UnsupportedFeature(exit3) / GenericError(exit1)。
- `CommandExecutor`（feature/base/command/CommandExecutor.kt）：suspend execute(command, workDir, env)，分开读 stdout/stderr。同步调用用 `runBlocking { }`。
- `TemplatePackageRewriter`（feature/scaffold/data/TemplatePackageRewriter.kt）：统一实现，提供 `rewrite()`(forward) + `rewriteReverse()`(sync-back)。
- `TemplateRenameRecipes`：ANDROID_CLIENT / KMP_CLIENT / BACKEND / ADMIN + recipeFor() + detectFromPath()。
- Koin 日志默认 NONE（EGS_DEBUG=true 开启）。
- detekt.yml：scaffold/analyzer/script 复杂度豁免（代码生成器 P2 重构），base/init 等仍约束。
- 每次代码修改后立即 `git add + commit`（用户要求），每批结束跑 `./gradlew test detektCheck spotlessCheck shadowJar` 验证。

### 约束
- 不主动改 golden 输出（除非明确优化生成内容）。
- 保持 `scripts/dev.sh` 和旧 `create ...` 命令兼容。
- `@CliktCommand` 注解在 Clikt 5.0.2 不能用于继承 EgsCliCommand 的子类（编译报 "Illegal annotation class"）。help 通过构造器参数或 init 块传递。

---

## 批次 1：快速收益（5 项，每项 1-2 文件）

### C-02 统一参数命名

**问题**：部分命令缺 `-p`/`-m` 短选项；`client gen prefs` 有 typo 别名 `--feilds`。

**改法**：
1. grep 所有 `option("--project"` 无 `-p` 的，补 `-p`。
2. grep `option("--module"` 无 `-m` 的，补 `-m`。
3. `ClientGenPrefsCommand` 的 `--feilds` 选项加 `hidden = true`，保留兼容但 help 不展示；确保 `--fields` 是主选项。

**涉及文件**：`feature/scaffold/presentation/*Command.kt`（grep 定位）。

**验证**：`./gradlew test detektCheck spotlessCheck`。

---

### F-01 增强 command 命令（timeout + 流式）

**问题**：`ShellCommand` 缺 timeout；输出等命令结束一次性返回（大输出延迟高）。

**改法**：
1. `CommandExecutor.execute` 加 `timeoutMs: Long? = null` 参数。非 null 时用 `process.waitFor(timeout, TimeUnit.MILLISECONDS)`，超时 `destroyForcibly()` + 返回 exit code -1。
2. `ShellCommand` 加 `--timeout <秒>` option，传给 CommandExecutor。
3. 流式（可选，较大）：CommandExecutor 加逐行回调或 Flow<String>。如时间不够，仅做 timeout。

**涉及文件**：`feature/base/command/CommandExecutor.kt`、`feature/command/presentation/CommandCli.kt`。

**验证**：`./gradlew :feature:command:test`。

---

### F-03 token clone 统一 HTTP header

**问题**：`ProjectTemplateCloner` 用 `GitHubCloneUrlPolicy.embedHttpsToken`（token 嵌 URL），token 可能出现在进程参数/日志。

**改法**：
1. `ProjectTemplateCloner.cloneAndCustomize` 不用 `embedHttpsToken`，改为像 `CreateProjectCommand` 那样用 env `GIT_HTTP_EXTRAHEADER: "Authorization: Basic <base64(user:token)>"`。
2. `CommandExecutor.execute` 已支持 environment 参数。
3. `GitHubCloneUrlPolicy.embedHttpsToken` 废弃或删除（grep 确认无其它调用方）。

**涉及文件**：`feature/scaffold/data/ProjectTemplateCloner.kt`、`feature/scaffold/data/GitHubCloneUrlPolicy.kt`。

**验证**：`./gradlew test`（如有 clone 相关 test）。

---

### P-02 清理乱码注释

**问题**：部分文件含 `¡ª`、`��` 等乱码（历史编码问题）。

**改法**：
1. `grep -rPn '[\x80-\xFF]{2,}' feature/ app/ --include='*.kt' | grep -v build` 定位。
2. 逐个修复为正确 UTF-8（根据上下文判断原意：通常是中文注释或破折号 `—`）。

**涉及文件**：grep 定位（预计 5-15 处）。

**验证**：`./gradlew spotlessCheck`。

---

### P-05 konsist-test 扩展

**问题**：Konsist 缺"presentation 不直接依赖 data"规则。

**改法**：在 konsist-test 加规则：
```kotlin
@ArchTest
val presentationMustNotDependOnData: ArchRule = noClasses()
    .that().resideInAPackage("..presentation..")
    .should().dependOnClassesThat().resideInAPackage("..data..")
```
（参考 egs-kmp-template 的 ModuleIsolationTest 写法。用精确包前缀 `com.egs.server`... 不，这里是 `com.dqc.egsengine`。）

**涉及文件**：`konsist-test/src/test/kotlin/**/*.kt`（新建或追加规则文件）。

**验证**：`./gradlew :konsist-test:test`。

---

## 批次 2：中等改动（4 项）

### C-03 补全 help 文案

**问题**：`egs-engine --help` 只列命令名，无说明。

**改法**：
1. `EgsCliCommand` 加 `help: String = ""` 构造参数（传给 CliktCommand）。但 Clikt 5 的 CliktCommand 构造器只有 name——需研究 Clikt 5 传递 help 的方式（可能 `override fun help(context...) = "..."` 或 `init { this.help = "..." }`）。
2. 每个 EgsCliCommand 子类传 help 字符串。
3. 根命令 `EgsEngineCli` 加 epilog 示例。

**注意**：先验证 Clikt 5.0.2 的 help 传递机制（`@CliktCommand` 注解已确认不行；试构造器 `CliktCommand(name=..., help=...)`——如果 5.0.2 不支持 help 参数，用 `command.context { help = "..." }` 或 override）。

**涉及文件**：`EgsCliCommand.kt`、所有 `*Command.kt`、`App.kt`。

**验证**：`java -jar app/build/libs/app-all.jar --help` 看输出。

---

### C-04 统一 dry-run 输出

**问题**：各命令 `--dry-run` 输出格式不一致。

**改法**：
1. 新建 `feature/base/presentation/DryRunFormatter.kt`：
   ```kotlin
   object DryRunFormatter {
       fun format(rootPath: String, files: List<String>, format: OutputFormat): String
       enum class OutputFormat { TEXT, JSON }
   }
   ```
2. 各命令 dry-run 分支统一调 `DryRunFormatter.format(...)`。
3. 加全局 `--format text|json` option（或每命令加）。

**涉及文件**：新建 `DryRunFormatter.kt`，改各 `*Command.kt` 的 dryRun 分支。

**验证**：`./gradlew test`。

---

### F-02 new project 非交互模式

**问题**：`NewProjectCommand` 缺参数时 `readLine()` 交互提示，CI 无法用。

**改法**：
1. 加 `--yes` / `--non-interactive` flag。
2. 设了则用默认值（projectName 从目录名、package 从 projectName、模板 URL 从配置）不 prompt。
3. 模板 URL 改配置驱动（`--template-url` 或 `egs.properties`）。

**涉及文件**：`NewProjectCommand.kt`、`NewProjectInputResolver.kt`。

**验证**：`./gradlew test`。

---

### A-06 集中 File I/O

**问题**：散落的 `file.readText()/writeText()/mkdirs()`，无原子写、无路径边界检查。

**改法**：
1. 新建 `feature/base/file/FileOperations.kt`：
   ```kotlin
   object FileOperations {
       fun writeTextAtomic(file: File, text: String)  // 写 temp + rename
       fun writeText(file: File, text: String) = withContext(Dispatchers.IO) { ... }
       fun readText(file: File): String = withContext(Dispatchers.IO) { file.readText() }
       fun ensureWithin(root: File, path: File)  // 路径边界检查
   }
   ```
2. 逐步替换各 generator/scaffolder 的直接 file I/O（可分批，先改高风险的）。

**涉及文件**：新建 `FileOperations.kt`，逐步改 `feature/scaffold/data/**/*.kt`。

**验证**：`./gradlew test`。

---

## 批次 3：命令收敛（1 项，需设计决策）

### C-01 命令收敛：create project → new project 代理

**问题**：`create project` 只支持 android（Template.ANDROID_URL），用独立 exec；`new project` 是多端 workspace。

**决策点**：
- 方案 A：`CreateProjectCommand.runCommand()` 内部构造 `NewProjectCommand` 并代理（传 `--client android --backend false --web false`）。需处理单端 android vs workspace 结构差异。
- 方案 B：直接删 `create project`，README/scripts 改用 `new project`。
- **建议**：方案 A（保持兼容），但如果 new project 的 workspace 结构与单端 android 不等价（settings.gradle、目录布局），可能需要 new project 支持"单端模式"。

**前置调查**：
1. 读 `NewProjectCommand.runCommand()` 全貌（~125 行 runCommand，CyclomaticComplexMethod 22），理解它创建的 workspace 结构。
2. 对比 `CreateProjectCommand` 创建的单端 android 结构。
3. 判断能否代理（结构等价？或 new project 加 `--single` 模式？）。

**涉及文件**：`CreateProjectCommand.kt`、`NewProjectCommand.kt`、`scripts/dev.sh`。

**验证**：`java -jar app/build/libs/app-all.jar create project testapp --package com.example.testapp` 生成的结构与之前一致。

---

## 批次 4：Swagger 平台抽象（3 项，大工程，建议独立任务）

### A-02 / A-03 / A-07 抽取 Swagger Android/KMP 共享逻辑

**问题**：6 个 Swagger 文件 ~2400 行，~75% 重复：
```
SwaggerCodeGenerator(199)      vs KmpSwaggerCodeGenerator(220)
SwaggerGeneratorContext(296)   vs KmpSwaggerGeneratorContext(283) + AndroidSwaggerGeneratorContext(16)
SwaggerTemplateRenderer(532)   vs KmpSwaggerTemplateRenderer(445)
```

**改法**（goal P1 原文）：
> 把 generator/context 的类型解析、import、响应解包提到 base/helper，平台只保留 Retrofit/Ktorfit 差异。

**步骤**：
1. **A-03 先做**：抽 `BaseSwaggerGeneratorContext`（类型解析 + import 生成 + 响应解包公共逻辑）。`SwaggerGeneratorContext` 和 `KmpSwaggerGeneratorContext` 继承它，只 override 差异（Retrofit annotation vs Ktorfit annotation）。
2. **A-07**：统一 `SwaggerCodeGenerator` interface。Android/KMP 实现调 BaseContext。
3. **A-02**：抽 `SwaggerTemplateHelper`（模板渲染公共逻辑）。`SwaggerTemplateRenderer`/`KmpSwaggerTemplateRenderer` 继承或委托。
4. 每步跑 golden test：`./scripts/dev.sh golden`（或 `./gradlew :feature:scaffold:test --tests '*Golden*'`），确保生成输出不变。

**风险**：golden snapshot 会捕获任何生成内容变化。必须逐步重构 + 每步验证 golden。

**涉及文件**：`feature/scaffold/data/swagger/*Swagger*.kt`（8 文件）+ golden test。

**验证**：`./gradlew test`（含 golden snapshot 比对）+ 手动对比生成代码。

---

## 批次 5：长期/低优先（4 项）

### A-04 拆小 scaffold DI

**问题**：`AndroidDatabaseScaffolder` 构造器 13 个参数，`AndroidApiScaffolder` 等 8-10 个。Koin 绑定顺序脆弱。

**改法**：改为平台工厂/配置对象：
```kotlin
data class AndroidDatabaseScaffoldConfig(
    val ddlParser: DdlParser,
    val codeGenerator: AndroidDatabaseCodeGenerator,
    // ...打包成 1 个 config 对象
)
class AndroidDatabaseScaffolder(private val config: AndroidDatabaseScaffoldConfig)
```

**涉及文件**：`feature/scaffold/domain/*Scaffolder.kt`、`di/ScaffoldModule.kt`。

**验证**：`./gradlew test`。

---

### F-04 跨端 flow CRUD 命令

**问题**：无"一条命令完成 CRUD 全栈"入口。

**改法**：新增 `FlowCommand` 子命令 `crud`：
```
egs-engine flow crud --name todo --fields "title:String,done:Boolean" --ddl todo.sql
```
串联：后端 DDL → admin CRUD → client api sync。

**涉及文件**：`FlowCommand.kt`（加 CrudFlowCommand），串联 `BackendCommand`/`ClientCommand` 的 scaffolder。

**验证**：手动跑 + 检查生成文件。

---

### P-01 实现/隐藏 SpringBoot/Vue3 Swagger generator

**问题**：`SpringBootApiGenerator`/`Vue3ApiGenerator` 抛 UnsupportedOperationException。

**决策**：
- 隐藏（小）：从 `SwaggerApiScaffolder` 的可用平台移除，help 不展示。`UnsupportedOperationException` 保留作为防御。
- 实现（大）：SpringBoot JPA CRUD 生成 + Vue3 TypeScript admin 生成。需 FreeMarker 模板 + golden。

**建议**：先隐藏（标注 TODO），实现留独立任务。

**涉及文件**：`SpringBootApiGenerator.kt`、`Vue3ApiGenerator.kt`、`SwaggerApiScaffolder.kt`。

---

### P-03 template-engine 包名统一

**问题**：`template-engine` 模块用 `com.dqc.egsengine.template`（非 `feature.templateengine`）。

**改法**：IDE 重构 rename package（机械改 import），或保留并文档说明（CLAUDE.md 已记录）。

**涉及文件**：`feature/template-engine/src/**/*.kt`（全模块 import 改）。

**验证**：`./gradlew test`。

---

## 执行顺序与依赖

```
批次 1（快速收益，并行）:
  C-02 参数统一 ─┐
  F-01 timeout  ─┤
  F-03 token    ─┼─ 无互相依赖，可并行
  P-02 乱码     ─┤
  P-05 konsist  ─┘

批次 2（中等，串行或并行）:
  C-03 help 文案 ─── 依赖 C-02 完成（参数统一后补 help）
  C-04 dry-run  ─── 独立
  F-02 非交互   ─── 独立
  A-06 FileOps  ─── 独立

批次 3（命令收敛，需决策）:
  C-01 create→new ─── 先调查 NewProjectCommand 结构，再决定代理/删除

批次 4（Swagger 抽取，大工程）:
  A-03 Context 抽象 ─┐
  A-07 CodeGen 统一 ─┼─ 串行（A-03 先），每步 golden 验证
  A-02 Renderer 抽象─┘

批次 5（长期）:
  A-04 DI 拆分 ─── 独立
  F-04 flow crud ─── 独立
  P-01 SB/Vue3 ─── 先隐藏，实现留独立
  P-03 包名 ─── 机械改
```

---

## 每批完成后的回归验证

```bash
./gradlew test detektCheck spotlessCheck shadowJar
java -jar app/build/libs/app-all.jar --help          # 0 Koin 行
java -jar app/build/libs/app-all.jar analyze /nonexistent 2>/dev/null; echo $?  # exit 2
```

全部 PASS 才算批次完成。
