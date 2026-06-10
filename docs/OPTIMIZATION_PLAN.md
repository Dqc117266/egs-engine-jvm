# egs-engine 终极优化规划

> 基于 2026-06-09 深度全量审计（架构 / 代码质量 / 测试 / CI·CD / 安全 / 性能 / DevEx）
> 当前评分 **68/100** → 目标评分 **95/100**

---

## 📊 评分卡（当前 → 目标）

| 维度 | 当前 | 目标 | 权重 | 说明 |
|------|------|------|------|------|
| 架构与模块化 | 75 | 95 | 20% | 模块划分好，但存在关键代码重复 |
| 代码质量与一致性 | 70 | 90 | 15% | 有 2 个运行时崩溃 stub、21 个 TODO |
| 测试覆盖与质量 | 55 | 85 | 15% | scaffold 模块测试优秀，但 7 个模块零覆盖 |
| 安全性 | 75 | 90 | 10% | 无硬编码密钥，但 git token 明文嵌入 URL |
| CI/CD 与 DevOps | 50 | 90 | 15% | 仅 1 个 CI workflow，无发布自动化 |
| 性能与构建速度 | 70 | 90 | 10% | 文件 I/O 未使用 Dispatchers.IO |
| 文档与开发者体验 | 65 | 95 | 15% | 无 CLAUDE.md、无 API 文档、无贡献指南 |

---

## 🗂️ 全局优化清单（7 大类 · 35 项）

### 一、架构与代码重复（A 类 · 6 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| A-01 | **P0** | 消除 `TemplatePackageRewriter` 双胞胎（70% 重复） | 中 |
| A-02 | **P0** | 消除 `SwaggerCodeGenerator` vs `KmpSwaggerCodeGenerator`（80% 重复） | 中 |
| A-03 | **P0** | 消除 `SwaggerGeneratorContext` vs `KmpSwaggerGeneratorContext`（75% 重复） | 中 |
| A-04 | **P1** | 提取 Command 基类：统一 6+ 个 Command 的错误处理样板 | 小 |
| A-05 | **P1** | 统一 `ProcessBuilder` 调用：3 处独立实现 → 共用 `CommandExecutor` | 小 |
| A-06 | **P2** | 修复 `template-engine` 包名不一致（`com.dqc.egsengine.template` → `feature.templateengine`） | 小 |

---

### 二、运行时崩溃与未实现功能（B 类 · 5 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| B-01 | **P0** | `SpringBootApiGenerator`：TODO() 崩溃 → 实现或降级为明确错误消息 | 小 |
| B-02 | **P0** | `Vue3ApiGenerator`：TODO() 崩溃 → 实现或降级为明确错误消息 | 小 |
| B-03 | **P1** | `ScriptCli`：TODO 未连接 CommandService → 实现或移除命令 | 小 |
| B-04 | **P1** | `CreateUseCaseScaffolder`：生成的 stub 代码含 `TODO("Not yet implemented")` → 改为合理默认实现 | 小 |
| B-05 | **P2** | `appModule` 空的 Koin 模块 → 移除或添加实际绑定 | 小 |

---

### 三、测试覆盖与质量（C 类 · 7 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| C-01 | **P0** | `feature:analyzer` 补充测试：GradleProjectScanner / BuildFileParser | 中 |
| C-02 | **P0** | `feature:template-engine` 补充测试：TemplateEngine 4 级覆盖链 / FreeMarker 渲染 | 中 |
| C-03 | **P1** | `feature:init` 补充测试：WorkspaceConfig 读写 / BaseClassScanner / EgsConfig 解析 | 中 |
| C-04 | **P1** | `feature:base` 补充测试：CliFormatter / ProjectRootResolver / CommandExecutor | 小 |
| C-05 | **P1** | CI 补齐 `feature:init:test`（当前只跑 scaffold + template-engine + konsist） | 小 |
| C-06 | **P2** | 启用代码覆盖率（Kover / JaCoCo）并接入 CI | 小 |
| C-07 | **P2** | 扩展 Konsist 架构测试：覆盖更多命名约定和层间依赖规则 | 小 |

---

### 四、代码质量与一致性（D 类 · 6 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| D-01 | **P1** | 消除 `printStackTrace()` 生产代码 → 使用 SLF4J logger | 小 |
| D-02 | **P1** | SwaggerParser / DdlParser 大量 `return null` → 考虑 sealed class / Result 类型 | 中 |
| D-03 | **P1** | 硬编码 GitHub 模板 URL → 外部化到配置文件或 AppConfig | 小 |
| D-04 | **P2** | `gradle.properties` 中 `apiTokenDebug=debug-token` / `apiTokenRelease=release-token` 占位符 → 文档说明或移除 | 小 |
| D-05 | **P2** | 添加 `@ParameterizedTest` 覆盖 generator 的多输入场景 | 中 |
| D-06 | **P2** | 统一 file I/O 调用使用 `withContext(Dispatchers.IO)` | 中 |

---

### 五、CI/CD 与 DevOps（E 类 · 5 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| E-01 | **P0** | 添加发布自动化：GitHub Release + Shadow JAR 上传 | 中 |
| E-02 | **P1** | 启用 Dependabot：Gradle + GitHub Actions 依赖更新 | 小 |
| E-03 | **P1** | CI 添加 Shadow JAR 构建验证 job | 小 |
| E-04 | **P2** | CI 添加全量测试 job（不仅是 scaffold + template-engine） | 小 |
| E-05 | **P2** | 添加 Release Drafter / 自动 CHANGELOG 生成 | 小 |

---

### 六、性能与构建（F 类 · 3 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| F-01 | **P1** | 40+ 处阻塞文件 I/O → `Dispatchers.IO` 包装 | 中 |
| F-02 | **P2** | 启用 Gradle Configuration Cache（当前关闭） | 中 |
| F-03 | **P2** | Gradle 构建微调：`kotlin.daemon.jvmargs` + `UseStringDeduplication` | 小 |

---

### 七、文档与开发者体验（G 类 · 3 项）

| 编号 | 优先级 | 标题 | 预估 |
|------|--------|------|------|
| G-01 | **P0** | 创建 `CLAUDE.md`：项目结构、架构规则、开发命令、模块依赖图 | 中 |
| G-02 | **P1** | 创建 `CONTRIBUTING.md`：贡献流程、代码规范、PR 检查清单 | 小 |
| G-03 | **P2** | 为 public API 补充 KDoc（scaffold domain 层、template-engine、CLI commands） | 中 |

---

## 📋 详细执行计划

---

### A-01：消除 TemplatePackageRewriter 双胞胎

**问题：** 两个路径下存在 `TemplatePackageRewriter`：
- `feature/scaffold/src/main/.../data/TemplatePackageRewriter.kt`（306 行）
- `feature/scaffold/src/main/.../data/template/TemplatePackageRewriter.kt`（226 行）

两者共享 ~70% 相同代码：`rewriteTextFiles`、`relocatePackageDirectories`、`moveDirectoryWithMerge`、`cleanupEmptyDirectories`、`isLikelyTextFile`、`readUtf8TextOrNull`、`BINARY_EXTENSIONS`。仅公共 API 不同（`rewrite` vs `rewriteForward`/`rewriteReverse`）。

**验收标准：**
- [ ] 提取共享逻辑到 `BasePackageRewriter` 抽象类或 `PackageRewriterInternal` 工具对象
- [ ] 两个重写器仅保留各自的差异化逻辑
- [ ] 所有现有测试通过（Golden Snapshot 不变）
- [ ] 新增单元测试覆盖 `SKIP_DIR_NAMES` 过滤逻辑（仅 `template/` 变体有）
- [ ] `./gradlew :feature:scaffold:test` 通过

---

### A-02：消除 SwaggerCodeGenerator 重复

**问题：** `SwaggerCodeGenerator`（178 行，Android）和 `KmpSwaggerCodeGenerator`（204 行，KMP）有 ~80% 结构重叠。共享方法：`isCommonResultWrapper`、`collectRequestSchemaNames`、`unwrapResponseBody`，以及整个编排流程。仅 source set 路径（`main` vs `commonMain`）和生成器类型不同。

**验收标准：**
- [ ] 提取共享编排逻辑到 `BaseSwaggerCodeGenerator` 抽象类
- [ ] Android 和 KMP 子类仅实现差异化部分（source set 路径、生成器工厂方法）
- [ ] 共享工具方法提取到 `SwaggerCodegenHelper` 对象
- [ ] Golden Snapshot 测试不变
- [ ] `./gradlew :feature:scaffold:test` 通过

---

### A-03：消除 SwaggerGeneratorContext 重复

**问题：** `SwaggerGeneratorContext`（274 行）和 `KmpSwaggerGeneratorContext`（287 行）共享 ~75% 代码。12 个方法完全相同（`resolveType`、`importsForType`、`toDomainExpression`、`repositoryResponseMapExpression` 等），仅 HTTP 客户端（Retrofit vs Ktorfit）和分页策略不同。

**验收标准：**
- [ ] 提取共享方法到 `BaseSwaggerGeneratorContext` 抽象类
- [ ] 子类仅保留 HTTP 客户端相关差异（import 路径、注解类型、返回类型包装）
- [ ] 共享方法添加单元测试（类型解析、import 推导、表达式生成）
- [ ] Golden Snapshot 测试不变
- [ ] `./gradlew :feature:scaffold:test` 通过

---

### A-04：提取 Command 基类

**问题：** 6+ 个 Command 类中重复相同的错误处理模式：
```kotlin
try {
    // 业务逻辑
} catch (e: IllegalArgumentException) {
    echoError(e.message ?: "Invalid argument")
} catch (e: Exception) {
    echoError("Unexpected error: ${e.message}")
}
```

出现在：`InitCommand`、`CreateProjectCommand`、`NewProjectCommand`、`WebCommand`、`TemplateCommand`、`CreateScreenCommand`、`CreatePageCommand`。

**验收标准：**
- [ ] 提取 `EgsCliCommand` 抽象基类，继承 `CliktCommand`
- [ ] 提供 `protected fun <T> runWithHandler(block: () -> T): T?` 或类似封装
- [ ] 统一错误消息格式（颜色、前缀、是否打印 stacktrace）
- [ ] 所有 Command 迁移到新基类
- [ ] `./gradlew test` 通过

---

### A-05：统一 ProcessBuilder 调用

**问题：** `ProcessBuilder` 在 3 处独立使用：
1. `ProjectTemplateCloner.exec()` — 私有方法
2. `CreateProjectCommand` — 内联 2 个调用点
3. `CommandExecutor.execute()` — 正确的共享实现

前两处绕过了共享的 `CommandExecutor`，无法统一日志、超时、错误处理。

**验收标准：**
- [ ] `ProjectTemplateCloner.exec()` 改为调用 `CommandExecutor`
- [ ] `CreateProjectCommand` 内联 ProcessBuilder 调用改为使用 `CommandService`
- [ ] 验证 `git clone` / `git init` 等操作行为不变
- [ ] `./gradlew test` 通过

---

### A-06：修复 template-engine 包名

**问题：** `template-engine` 模块使用 `com.dqc.egsengine.template`，而所有其他 feature 模块使用 `com.dqc.egsengine.feature.<name>`。

**验收标准：**
- [ ] `com.dqc.egsengine.template` → `com.dqc.egsengine.feature.templateengine`（或 `feature.template_engine`）
- [ ] 所有 import 更新
- [ ] Konsist 测试更新（如果包含该模块）
- [ ] `./gradlew check` 通过

---

### B-01：修复 SpringBootApiGenerator 崩溃

**问题：** `SpringBootApiGenerator.kt` 第 29 行 `TODO("Spring Boot API generation from Swagger not yet implemented")` 会在运行时抛出 `NotImplementedError` 崩溃。

**验收标准（二选一）：**
- **实现**：完成 Spring Boot Swagger codegen（利用已有的 FTL 模板 + SwaggerParser）
- **降级**：改为 `throw UnsupportedOperationException("Spring Boot API generation is not yet available. Use `create module` with --type=springboot instead.")` + 在 CLI 层提供友好提示
- [ ] 不再有运行时 `TODO()` 崩溃

---

### B-02：修复 Vue3ApiGenerator 崩溃

**问题：** 同 B-01，`Vue3ApiGenerator.kt` 第 29 行 `TODO()` 崩溃。

**验收标准：**
- 同 B-01：实现或降级为友好错误
- [ ] 不再有运行时 `TODO()` 崩溃

---

### B-03：ScriptCli 未实现功能

**问题：** `ScriptCli.kt` 第 33 行 `// TODO: Execute script commands through CommandService` — 脚本执行功能未连接。

**验收标准（二选一）：**
- **实现**：将脚本命令通过 `CommandService` 执行
- **移除**：如果脚本功能不在近期路线图，移除 `ScriptCli` 命令以避免误导用户
- [ ] CLI help 中不出现未实现的功能（或标注为 `[EXPERIMENTAL]`）

---

### B-04：生成的 stub 代码安全化

**问题：** `CreateUseCaseScaffolder` 生成的 stub 方法包含 `TODO("Not yet implemented")`，用户调用即崩溃。

**验收标准：**
- [ ] 生成的 UseCase stub 方法改为有意义的默认实现：
  - 返回类型为 `Unit` → 空方法体
  - 返回类型为 `Result` → `Result.success(null)` 或 `Result.failure(NotImplementedError())`
  - 返回类型为 `Flow` → `flow { emit(defaultValue) }`
- [ ] 生成的代码中添加 `// TODO: Implement actual business logic` 注释引导用户
- [ ] Golden Snapshot 更新

---

### B-05：清理空的 appModule

**问题：** `app/src/main/.../di/AppModule.kt` 中 `val appModule = module { }` 是空的 Koin 模块。

**验收标准：**
- [ ] 如果无计划添加 app 级绑定：移除 `appModule` 及其注册
- [ ] 如果有计划：添加 TODO 注释说明预期用途
- [ ] `./gradlew :app:run` 验证功能不变

---

### C-01：feature:analyzer 测试

**问题：** `feature:analyzer`（8 个源文件）零测试。包含 `GradleProjectScanner` 和 `BuildFileParser` 等关键组件。

**验收标准：**
- [ ] `GradleProjectScannerTest`：验证模块发现逻辑（mock Gradle Tooling API）
- [ ] `BuildFileParserTest`：验证 `build.gradle.kts` 解析（插件检测、依赖提取）
- [ ] `ProjectAnalyzerTest`：集成测试验证项目类型检测
- [ ] 至少 15 个测试方法
- [ ] `./gradlew :feature:analyzer:test` 通过

---

### C-02：feature:template-engine 测试

**问题：** `feature:template-engine`（9 个源文件）仅 1 个测试（`TemplateRegistryTest`）。核心的 4 级模板覆盖链无测试。

**验收标准：**
- [ ] `TemplateEngineTest`：4 级覆盖链测试（classpath → home → project → env 优先级）
- [ ] `FreeMarkerRenderingTest`：基本模板渲染、变量替换、条件逻辑、循环
- [ ] `TemplateResolutionEdgeCasesTest`：缺失模板错误、空模板、循环 include
- [ ] 至少 12 个测试方法
- [ ] `./gradlew :feature:template-engine:test` 通过

---

### C-03：feature:init 测试

**问题：** `feature:init`（14 个源文件）仅 2 个测试，其中 1 个依赖外部项目（`BaseClassScannerKmpIntegrationTest` 用 `Assumptions.assumeTrue` 跳过）。

**验收标准：**
- [ ] `WorkspaceConfigResolverTest`：读写 `workspace.json`、字段合并、向后兼容
- [ ] `EgsConfigTest`：`config.json` 解析、scaffoldOverrides 合并
- [ ] `BaseClassScannerUnitTest`：不依赖外部项目的单元测试（mock 文件系统）
- [ ] 至少 15 个测试方法
- [ ] `./gradlew :feature:init:test` 通过

---

### C-04：feature:base 测试

**问题：** `feature:base`（4 个源文件）零测试。包含 `CliFormatter`（ANSI 表格/颜色）和 `ProjectRootResolver`。

**验收标准：**
- [ ] `CliFormatterTest`：表格渲染、颜色代码、空输入、长文本截断
- [ ] `ProjectRootResolverTest`：Gradle 项目检测、workspace 根解析、回退逻辑
- [ ] `CommandExecutorTest`：成功执行、超时、错误输出捕获
- [ ] 至少 10 个测试方法
- [ ] `./gradlew :feature:base:test` 通过

---

### C-05：CI 补齐 feature:init:test

**问题：** CI 只运行 `:feature:scaffold:test`、`:feature:template-engine:test`、`:konsist-test:test`，遗漏了 `:feature:init:test`（有 2 个测试文件）。

**验收标准：**
- [ ] `.github/workflows/ci.yml` 的 test job 添加 `:feature:init:test`
- [ ] 或者改为运行 `./gradlew test`（所有模块的测试）
- [ ] CI 通过

---

### C-06：启用代码覆盖率

**验收标准：**
- [ ] 添加 [Kover](https://github.com/Kotlin/kotlinx-kover) 插件（Kotlin 原生覆盖率工具）
- [ ] 关键模块（`feature:scaffold`、`feature:template-engine`、`feature:init`）配置覆盖率
- [ ] CI 中新增 `./gradlew koverXmlReport` 步骤
- [ ] 覆盖率报告上传到 Codecov（如开源）
- [ ] 目标：核心模块行覆盖率 > 60%

---

### C-07：扩展 Konsist 架构测试

**问题：** 当前仅 3 条规则，且一条是自我引用的（检查 `Cli` 后缀的类有 `Cli` 后缀）。`feature:scaffold` 的 data 层大量类不遵循 `Repository|Runner|Loader|Impl` 后缀规则。

**验收标准：**
- [ ] 修复自引用的 presentation 测试（改为检查所有 CliktCommand 子类有 `Cli` 后缀）
- [ ] 放宽或重构 data 层命名规则：区分 `Generator`、`Parser`、`Scanner`、`Updater` 等合法后缀
- [ ] 新增规则：每个 feature 模块必须有 `di/<Name>Module.kt`
- [ ] 新增规则：`domain` 层不可导入 `presentation` 层
- [ ] 新增规则：`feature:common` 不被 `feature:base` 依赖（当前通过 Convention Plugin 隐式依赖）
- [ ] `./gradlew :konsist-test:test` 通过

---

### D-01：消除 printStackTrace()

**问题：** `CreateScreenCommand.kt` 第 124 行和 `CreatePageCommand.kt` 第 85 行在 `EGS_DEBUG` 环境变量下使用 `e.printStackTrace()`。项目已使用 SLF4J。

**验收标准：**
- [ ] `e.printStackTrace()` → `logger.error("Failed to ...", e)`（SLF4J 已在类中声明）
- [ ] 全局搜索确认无其他 `printStackTrace()` 调用

---

### D-02：SwaggerParser/DdlParser 返回类型改进

**问题：** `SwaggerParser` 和 `DdlParser` 大量使用 `return null` 表示解析失败（~30+ 处），调用方需要频繁判空，错误信息丢失。

**验收标准（渐进式）：**
- [ ] 新增 `sealed class ParseResult<T>` { `Success(data: T)`, `ParseError(message: String, line: Int?)` }
- [ ] 核心解析入口方法（`SwaggerParser.parse()`、`DdlParser.parse()`）改为返回 `ParseResult<T>`
- [ ] 内部辅助方法可保持 nullable（渐进迁移）
- [ ] CLI 层对 `ParseError` 提供友好的错误消息（含行号和上下文）
- [ ] 现有测试不变（返回值类型变更需要调整断言）

---

### D-03：外部化模板 URL

**问题：** `NewProjectTemplateUrls.kt` 硬编码 4 个 GitHub URL：
```kotlin
"https://github.com/Dqc117266/egs-kmp-template.git"
"https://github.com/Dqc117266/egs-android-template.git"
"https://github.com/Dqc117266/egs-server-template.git"
"https://github.com/Dqc117266/egs-admin-template.git"
```

**验收标准：**
- [ ] URL 移到 `AppConfig` 或独立的 `TemplateUrlsConfig`，从 `config.properties` 可配置
- [ ] 默认值与当前硬编码一致（向后兼容）
- [ ] CLI `config set template-url.<name>=<url>` 支持自定义
- [ ] 现有 `create project` 功能不变

---

### D-04：gradle.properties 占位符清理

**问题：** `apiTokenDebug=debug-token` 和 `apiTokenRelease=release-token` 是占位符，实际 token 通过环境变量覆盖。

**验收标准：**
- [ ] 如果生产代码确实使用这些属性：文档说明用途
- [ ] 如果仅用于示例：移除或改为空字符串默认值
- [ ] README 中补充配置说明

---

### D-05：参数化测试

**问题：** 多个 generator 测试可以受益于 `@ParameterizedTest`（如 DDL 类型映射、Swagger 类型解析、模板变量替换）。

**验收标准：**
- [ ] `DdlParserTest` 类型映射改为 `@ParameterizedTest`（覆盖所有 SQL 类型 → Kotlin 类型映射）
- [ ] `SwaggerParserTest` 类型解析改为参数化（`string` → `String`、`integer` → `Int` 等）
- [ ] `KmpPageTemplateSnippets` / `AndroidPageTemplateSnippets` 共享测试用例提取为参数化

---

### D-06：统一文件 I/O Dispatchers

**问题：** 40+ 处文件操作（`File.readText()`、`File.writeText()`、`File.readLines()`）直接在调用线程执行，仅 `CommandExecutor` 使用了 `Dispatchers.IO`。

**验收标准：**
- [ ] 提取 `FileOperations` 工具类，所有文件操作包装在 `withContext(Dispatchers.IO)` 中
- [ ] 关键热路径优先迁移：`TemplatePackageRewriter`、`SwaggerParser`、`ProjectTemplateCloner`
- [ ] 非关键路径标记 `// TODO: migrate to FileOperations`
- [ ] 现有测试不变

---

### E-01：发布自动化 ⚡ 最高价值

**问题：** 无发布流程。每次发布需手动构建 Shadow JAR、创建 GitHub Release、上传产物。

**验收标准：**
- [ ] 新增 `.github/workflows/release.yml`：
  - 触发：推送 `v*` tag 或手动 dispatch
  - 构建 Shadow JAR
  - 创建 GitHub Release（含自动生成的 CHANGELOG）
  - 上传 JAR 到 Release assets
- [ ] `./gradlew shadowJar` 产物命名规范：`egs-engine-{version}.jar`
- [ ] README 添加安装说明：`curl -L ... -o egs-engine.jar`
- [ ] 首次发布后验证下载可用

---

### E-02：启用 Dependabot

**验收标准：**
- [ ] 新增 `.github/dependabot.yml`：
  ```yaml
  version: 2
  updates:
    - package-ecosystem: "gradle"
      directory: "/"
      schedule: { interval: "weekly" }
    - package-ecosystem: "github-actions"
      directory: "/"
      schedule: { interval: "monthly" }
  ```
- [ ] 配置 auto-merge for minor/patch（需 branch protection 配合）

---

### E-03：CI Shadow JAR 构建验证

**问题：** CI 只跑 lint + test，不验证 Shadow JAR 能否成功构建。

**验收标准：**
- [ ] CI 新增 build job：
  ```yaml
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: ./gradlew shadowJar
      - uses: actions/upload-artifact@v4
        with:
          name: egs-engine.jar
          path: app/build/libs/*.jar
  ```
- [ ] Smoke test：`java -jar app/build/libs/*.jar --help` 验证可执行

---

### E-04：CI 全量测试

**问题：** CI test job 只跑 3 个模块的测试，遗漏了 `feature:init`、`feature:base`、`feature:common` 等。

**验收标准：**
- [ ] test job 改为 `./gradlew test`（运行所有模块测试）
- [ ] 或明确列出所有有测试的模块
- [ ] CI 通过

---

### E-05：自动 CHANGELOG

**验收标准：**
- [ ] 安装 [Release Drafter](https://github.com/release-drafter/release-drafter) GitHub App
- [ ] `.github/release-drafter.yml` 配置：基于 conventional commit 分类（feat/fix/refactor/docs）
- [ ] PR 合并时自动更新 draft release notes
- [ ] 发布时一键发布

---

### F-01：文件 I/O 性能优化

（详见 D-06，两者共享工作量）

**额外验收标准：**
- [ ] 大型项目树（100+ 文件）的 `create module` 操作测量耗时
- [ ] `withContext(Dispatchers.IO)` 后确认不阻塞 CLI 主线程

---

### F-02：启用 Gradle Configuration Cache

**问题：** `org.gradle.configuration-cache=false`，可能是 convention plugin 兼容性问题。

**验收标准：**
- [ ] 尝试启用 `org.gradle.configuration-cache=true`
- [ ] 修复所有 configuration cache 不兼容的 convention plugin 代码
- [ ] 验证 `./gradlew help` 第二次执行速度提升
- [ ] 如果某些任务不兼容：按任务粒度禁用（`configuration-cache.problems=warn` 过渡期）

---

### F-03：Gradle 构建微调

**验收标准：**
- [ ] `gradle.properties` 添加：
  ```properties
  kotlin.daemon.jvmargs=-Xmx2g -XX:+UseParallelGC -XX:+UseStringDeduplication
  ```
- [ ] 评估 build scan（`--scan`）发布到 Gradle scans 服务
- [ ] 添加 `org.gradle.configuration-cache.problems=warn` 过渡期

---

### G-01：创建 CLAUDE.md ⚡ 最高优先级

**问题：** 项目无 CLAUDE.md，Claude Code / AI 工具无法理解项目结构。

**验收标准：**
- [ ] 创建 `CLAUDE.md` 包含：
  - 项目概述（Kotlin CLI 代码生成引擎）
  - 模块列表与依赖图
  - 架构分层规则（data / domain / presentation / di）
  - 常用命令（build、test、golden update、detekt）
  - 关键配置说明（golden snapshot、template override chain）
  - 代码贡献规范
- [ ] 不超过 200 行（保持精简）

---

### G-02：创建 CONTRIBUTING.md

**验收标准：**
- [ ] 创建 `CONTRIBUTING.md` 包含：
  - 开发环境要求（JDK 17+、Gradle 9.x）
  - Fork & PR 流程
  - 代码规范（ktlint via Spotless、Detekt 规则）
  - PR 检查清单（测试通过、lint 通过、golden snapshot 更新说明）
  - commit 消息格式（conventional commits）
- [ ] README 中添加贡献指南链接

---

### G-03：公共 API KDoc

**验收标准：**
- [ ] `feature/scaffold/domain/` 所有 public 类和方法添加 KDoc
- [ ] `feature/template-engine/` 公共 API 添加 KDoc
- [ ] `feature/init/domain/` 公共模型类添加 KDoc
- [ ] CLI 命令的 `--help` 文本审查（与代码注释一致性）

---

## 🗓️ 推荐执行顺序（4 个阶段）

### Phase 1：崩溃修复 + 文档 + CI 基础（1 周）🔴

> 堵住运行时崩溃，建立开发者体验基线

| 编号 | 工作量 | 风险 |
|------|--------|------|
| **B-01** SpringBootApiGenerator 崩溃修复 | 2h | 低 |
| **B-02** Vue3ApiGenerator 崩溃修复 | 2h | 低 |
| **B-04** 生成 stub 代码安全化 | 1h | 低 |
| **G-01** 创建 CLAUDE.md | 3h | 低 |
| **E-03** CI Shadow JAR 构建验证 | 1h | 低 |
| **C-05** CI 补齐 feature:init:test | 0.5h | 低 |
| **D-01** 消除 printStackTrace() | 0.5h | 低 |

**Phase 1 完成后评分：68 → 76**

---

### Phase 2：代码重复消除 + 架构改进（2 周）🟡

> 消除最大的技术债务

| 编号 | 工作量 | 风险 |
|------|--------|------|
| **A-01** TemplatePackageRewriter 去重 | 4h | 中 |
| **A-02** SwaggerCodeGenerator 去重 | 6h | 中 |
| **A-03** SwaggerGeneratorContext 去重 | 4h | 中 |
| **A-04** Command 基类提取 | 3h | 低 |
| **A-05** ProcessBuilder 统一 | 2h | 低 |
| **B-03** ScriptCli 决策 | 2h | 低 |
| **B-05** 空 appModule 清理 | 0.5h | 低 |
| **D-03** 模板 URL 外部化 | 2h | 低 |

**Phase 2 完成后评分：76 → 85**

---

### Phase 3：测试覆盖 + CI/CD 完善（2 周）🟢

> 补齐测试，建立发布流程

| 编号 | 工作量 | 风险 |
|------|--------|------|
| **C-01** feature:analyzer 测试 | 6h | 中 |
| **C-02** feature:template-engine 测试 | 4h | 中 |
| **C-03** feature:init 测试 | 4h | 中 |
| **C-04** feature:base 测试 | 3h | 低 |
| **C-07** 扩展 Konsist 测试 | 3h | 低 |
| **E-01** 发布自动化 | 4h | 中 |
| **E-02** 启用 Dependabot | 1h | 低 |
| **E-05** 自动 CHANGELOG | 2h | 低 |

**Phase 3 完成后评分：85 → 92**

---

### Phase 4：打磨与优化（1 周）🔵

> 性能、代码质量、文档完善

| 编号 | 工作量 | 风险 |
|------|--------|------|
| **F-01** / **D-06** 文件 I/O Dispatchers | 6h | 中 |
| **D-02** Parser 返回类型改进 | 4h | 中 |
| **F-02** Configuration Cache 启用 | 4h | 中 |
| **F-03** Gradle 构建微调 | 1h | 低 |
| **C-06** 启用 Kover 覆盖率 | 2h | 低 |
| **D-05** 参数化测试 | 3h | 低 |
| **A-06** template-engine 包名统一 | 1h | 低 |
| **G-02** CONTRIBUTING.md | 2h | 低 |
| **G-03** 公共 API KDoc | 4h | 低 |
| **D-04** 占位符清理 | 0.5h | 低 |

**Phase 4 完成后评分：92 → 95**

---

## 📈 评分提升路径

```
68 ── Phase 1 ──→ 76 ── Phase 2 ──→ 85 ── Phase 3 ──→ 92 ── Phase 4 ──→ 95
     崩溃修复/文档      代码去重          测试/发布         性能/打磨
     (1 周)             (2 周)            (2 周)            (1 周)
```

**总预估工作量：~90 人时（约 2.2 个工作周）**

---

## ✅ 终极验收检查清单

```bash
# 1. 编译 + Shadow JAR
./gradlew shadowJar
java -jar app/build/libs/egs-engine-*.jar --help    # 应输出帮助信息

# 2. 全量测试
./gradlew test

# 3. 代码质量
./gradlew detektCheck spotlessCheck

# 4. 架构测试
./gradlew :konsist-test:test

# 5. Golden Snapshot 验证（无变更）
./gradlew :feature:scaffold:test
git diff feature/scaffold/src/test/resources/golden/  # 应无差异

# 6. 安全审计
grep -r "TODO()" --include="*.kt" feature/ src/       # 应无结果（运行时崩溃类）
grep -r "printStackTrace" --include="*.kt" .           # 应无结果
grep -r "Wizard\|password\|secret" --include="*.kt" .  # 应无硬编码密码

# 7. 覆盖率
./gradlew koverHtmlReport
open build/reports/kover/html/index.html              # 核心模块 > 60%

# 8. 文件完整性
test -f CLAUDE.md && echo "✅ CLAUDE.md exists"
test -f CONTRIBUTING.md && echo "✅ CONTRIBUTING.md exists"
test -f .github/dependabot.yml && echo "✅ Dependabot configured"
test -f .github/workflows/release.yml && echo "✅ Release workflow exists"
```

---

## 📊 项目现状数据总览

| 指标 | 数值 |
|------|------|
| 总模块数 | 12（11 + build-logic） |
| 主源文件数 | ~213 |
| 测试文件数 | 50 |
| Golden Snapshot 文件 | 45 |
| FreeMarker 模板 | 89 |
| 总代码行数 | ~31,773 |
| 测试覆盖率（文件比） | 23%（50/213） |
| 零测试模块数 | 7（+ build-logic） |
| TODO 数 | 21（含 2 运行时崩溃） |
| 代码重复对数 | 3（各 70-80% 重叠） |
| CI Workflows | 1 |
| 发布流程 | 无 |

---

## 🏆 核心优势（应保持）

优化不是全盘否定。以下方面 egs-engine 做得很好，执行优化时应**保护不破坏**：

1. **Golden Snapshot 测试框架** — 生产级质量，应作为其他模块的测试范式推广
2. **Convention Plugin 体系** — 模块间依赖自动注入、测试自动配置，设计精良
3. **4 级模板覆盖链** — `env > project > home > classpath`，灵活且向后兼容
4. **Clean Architecture 一致性** — data / domain / presentation / di 四层严格遵守
5. **依赖版本管理** — Version Catalog 集中管理，无散落的硬编码版本
6. **安全意识** — 无硬编码密钥，token 通过环境变量传入
7. **日志规范** — 156 处 SLF4J 使用，覆盖率极高

---

> **本规划包含 7 大类 35 项优化，按 4 阶段执行，预估 ~90 人时，评分从 68 提升至 95。**
> **核心策略：先堵崩溃 → 去重减债 → 补齐测试 → 完善发布 → 性能打磨。**
