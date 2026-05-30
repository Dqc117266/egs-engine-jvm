# egs-engine

Kotlin/JVM 命令行工具（Clikt），入口类 `com.dqc.egsengine.AppKt`，根命令名为 `egs-engine`。当前版本见 `AppVersion.NAME`（与 `--version` 输出一致）。

## 构建与运行

在项目根目录 `egs-engine/` 下：

```bash
./gradlew :app:run
./gradlew :app:run --args="--help"
./gradlew :app:run --args="analyze --json ."
```

打包可执行 Fat JAR（Shadow 插件）：

```bash
./gradlew :app:shadowJar
java -jar app/build/libs/app-all.jar --help
```

---

## 开发与测试

所有命令均在 `egs-engine/` 目录下执行，除非特别说明。

### 日常三步（改完代码最常用）

```bash
# 1. 编译（改完立刻跑，约 10 秒）
./gradlew :feature:scaffold:compileKotlin :feature:template-engine:compileKotlin :app:compileKotlin --no-daemon

# 2. 测试（提交前跑，约 15 秒，151 个测试）
./gradlew :feature:scaffold:test --no-daemon

# 3. 提交同步
git add -A && git commit -m "fix: 描述你的改动" && git push
```

成功标志：编译输出 `BUILD SUCCESSFUL`；测试输出 `151 passing`。

### 按修改类型选命令

#### 改了 Kotlin 业务逻辑（非模板）

```bash
# 只编译动过的模块
./gradlew :feature:scaffold:compileKotlin --no-daemon

# 跑相关测试（示例：PageScaffolder）
./gradlew :feature:scaffold:test \
  --tests "com.dqc.egsengine.feature.scaffold.domain.PageScaffolderTemplateEngineTest" \
  --no-daemon
```

#### 改了 FTL 模板（`feature/template-engine/src/main/resources/templates/*.ftl`）

**在 egs-engine 内（golden 快照）：**

```bash
# 跑 golden，对比生成产物与期望
./gradlew :feature:scaffold:test \
  --tests "com.dqc.egsengine.feature.scaffold.golden.*" \
  --no-daemon

# 改动符合预期时，刷新 golden 基线
./gradlew :feature:scaffold:test -Degs.golden.update=true --no-daemon

# 复核快照 diff
git diff feature/scaffold/src/test/resources/golden/
```

**在 monorepo 根目录（含 demo-app，推荐边改边看）：**

```bash
cd ..   # 进入 egs 根目录（与 egs-engine 同级）

./scripts/dev.sh watch              # 常驻：改 FTL 存盘即重跑 golden
./scripts/dev.sh golden             # 跑一次 golden
./scripts/dev.sh golden --update    # 刷新基线后 git diff 复核
```

`dev.sh` 会自动设置 `EGS_TEMPLATE_ROOT` 与 `EGS_DEBUG=true`。详见 [docs/DEV_WORKFLOW.md](docs/DEV_WORKFLOW.md)。

#### 改了生成逻辑，想在 demo-app 看真实落地文件

在 monorepo 根目录：

```bash
./scripts/dev.sh preview client create module home --dry-run   # 预览，不写文件
./scripts/dev.sh regen client create module home               # 生成 + git diff
./scripts/dev.sh regen client --verify create module home      # 生成 + 编译验证
./scripts/dev.sh reset client                                  # 还原 demo-app/client
```

#### 改了 Swagger / API 生成

```bash
# 默认 KotlinPoet 生成器
./gradlew :app:run --args="create api task --swagger ./swagger.json --project ../demo-app/client --dry-run"

# 改用 FreeMarker 生成器
EGS_SWAGGER_GENERATOR=ftl ./gradlew :app:run --args="create api task --swagger ./swagger.json --project ../demo-app/client --dry-run"
```

#### 改了 CLI 命令

```bash
./gradlew :app:run --args="--help"
./gradlew :app:run --args="create module foo --project ../demo-app/client --dry-run"
./gradlew :app:run --args="template sync-back --from ../demo-app/client --paths core-base --dry-run"
```

### 编译

```bash
./gradlew :feature:scaffold:compileKotlin :feature:template-engine:compileKotlin :app:compileKotlin --no-daemon
```

### 运行测试

```bash
# 运行 scaffold 模块全部测试
./gradlew :feature:scaffold:test --no-daemon

# 仅运行某个测试类
./gradlew :feature:scaffold:test --tests "com.dqc.egsengine.feature.scaffold.golden.GoldenModuleTest" --no-daemon

# 仅运行匹配的测试方法（支持通配）
./gradlew :feature:scaffold:test --tests "com.dqc.egsengine.feature.scaffold.golden.GoldenModuleTest.kmp*" --no-daemon
```

### Golden 快照

生成器输出与 `feature/scaffold/src/test/resources/golden/` 下的快照逐字节比对。模板/生成逻辑有意变更后，用以下命令刷新快照，再 review `git diff`：

```bash
./gradlew :feature:scaffold:test -Degs.golden.update=true --no-daemon
./gradlew :feature:scaffold:test -Degs.golden.dir=/abs/dir --no-daemon   # 覆盖 golden 根目录
```

### 解决合并冲突

```bash
git status
# 逐个文件解决冲突标记后：
git add -A
./gradlew :feature:scaffold:test --no-daemon
git commit
```

### 速记

| 场景 | 命令 |
|------|------|
| 改 Kotlin / 引擎逻辑 | `./gradlew :feature:scaffold:test --no-daemon` |
| 改 FTL 模板 | `./gradlew :feature:scaffold:test --tests "…golden.*" --no-daemon` |
| 模板改动符合预期 | `./gradlew :feature:scaffold:test -Degs.golden.update=true --no-daemon` |
| 在 demo-app 验证 | `../scripts/dev.sh regen client create module xxx` |
| 提交前 | 编译 + 全量测试 + `git push` |

---

## 全局参数

| 选项 | 说明 |
|------|------|
| `--version` / `-v` | 打印版本并退出（不进入子命令时生效） |

---

## 环境变量

| 变量 | 用途 |
|------|------|
| `GITHUB_TOKEN` | `create project` 在 `--auth token` 时可替代 `--token` |
| `EGS_ENGINE_GIT_PROTOCOL` | `new project` 中与 `--protocol` 配合，控制克隆 URL（ssh/https） |
| `EGS_DEBUG` | 设为 `true` 时，`create screen` / `create page` 异常会打印堆栈 |
| `EGS_TEMPLATE_ROOT` | 指向本地 `templates/` 目录，覆盖内置 FTL 模板（开发调试用，优先级高于 `<project>/.egs/templates`） |
| `EGS_SWAGGER_GENERATOR` | `create api` 生成器选择：默认/`kotlinpoet` 用 KotlinPoet，`ftl`/`freemarker` 走 FreeMarker |

---

## 命令与输入一览

下列命令路径均相对于根命令 `egs-engine`（若通过 Gradle 运行，放在 `--args="..."` 中）。

### `analyze` — 分析 Gradle 项目

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 项目路径 | `.` | 要分析的目录 |
| 选项 | `--verbose` / `-v` | 关 | 输出模块详情 |
| 选项 | `--json` | 关 | 以 JSON 输出 |

---

### `command` — 在子进程中执行 shell 命令

| 类型 | 名称 | 必填 | 说明 |
|------|------|------|------|
| 选项 | `--dir` / `-d` | 否 | 工作目录 |
| 位置参数（多个） | shell 片段 | 是 | 至少一段；多段会拼接成一个字符串执行 |

---

### `task` — 简单任务队列

**`task list`** — 无额外参数，列出待执行任务。

**`task add`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 位置参数 | 任务名 | 必填 |
| 位置参数（多个） | 命令列表 | 零个或多个字符串 |

**`task cancel`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 位置参数 | 任务 ID | 必填 |

---

### `script` — 脚本加载/校验（执行链路见代码 TODO）

**`script run <path>`** — 脚本文件路径（必填）。

**`script list [directory]`** — 目录默认为 `.`。

**`script validate <path>`** — 脚本文件路径（必填）。

---

### `init` — 初始化 `.egs/config.json`

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 项目路径 | `.` | 会解析工作区/Gradle 根目录 |

---

### `create` — 在现有工程中脚手架

**`create project`**（`CreateProjectCommand`）

| 类型 | 名称 | 默认值 / 行为 | 说明 |
|------|------|----------------|------|
| 位置参数 | 项目名 | 可选；缺省时交互输入 | 须匹配 `^[A-Za-z][A-Za-z0-9_-]*$` |
| 选项 | `--package` | 交互，默认 `com.dqc.example` | 基础包名 |
| 选项 | `--type` | `android` | 目前仅支持 `android` |
| 选项 | `--output` / `-o` | `.` | 输出父目录 |
| 选项 | `--template` | `git@github.com:Dqc117266/egs-android-template.git` | 模板 Git URL |
| 选项 | `--auth` | `none` | `none` / `login` / `token` |
| 选项 | `--token` | 可来自 `GITHUB_TOKEN` | `--auth token` 时必填其一 |
| 选项 | `--username` | `x-access-token` | HTTPS Token 克隆用户名段 |

**`create module <name>`**

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 模块名 | 必填 | feature 模块名 |
| 选项 | `--project` / `-p` | `.` | 工程根 |
| 选项 | `--package` | — | 自定义包名 |
| 选项 | `--dry-run` | 关 | 仅预览 |

**`create api <moduleName>`**

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 目标 feature 模块名 | 必填 | 如 `home` |
| 选项 | `--swagger` / `-s` | `""`（须显式提供） | Swagger/OpenAPI JSON 的 URL 或文件路径 |
| 选项 | `--project` / `-p` | `.` | 工程根 |
| 选项 | `--package` | — | 基础包覆盖 |
| 选项 | `--dry-run` | 关 | 仅预览 |

**`create screen <NAME>`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 位置参数 | Screen 名 | 必填；字母开头，仅字母数字 |
| 选项 | `-m` / `--module` | 若提供则走**非交互**模式；缺省则交互选模块等 |
| 选项 | `-u` / `--usecase` | 逗号分隔 UseCase 名 |
| 选项 | `-r` / `--route` | 导航路径 |
| 选项 | `-p` / `--params` | `name:Type` 逗号分隔 |
| 选项 | `--project` | 默认 `.` |
| 选项 | `--dry-run` | 预览 |

交互模式额外从标准输入读取：模块索引、UseCase 选择、路由、是否确认等。

**`create page`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 选项 | `-m` / `--module` | 与 `-n`/`--name` 同时提供时走**非交互**模式 |
| 选项 | `-n` / `--name` | 页面名 |
| 选项 | `-a` / `--api` | 可重复，指定多个 UseCase |
| 选项 | `-p` / `--project` | 默认 `.` |
| 选项 | `--dry-run` | 预览 |

否则进入交互：选模块、输入页面名、选 UseCase、确认。

---

### `new project` — 多仓工作区（`.egs/workspace.json`）

| 类型 | 名称 | 默认值 / 行为 | 说明 |
|------|------|----------------|------|
| 位置参数 | 项目名 | 可选；缺省交互 | 命名规则同 `create project` |
| 选项 | `--package` | 交互，默认 `com.dqc.example` | 基础包名 |
| 选项 | `--client` | 交互默认 `kmp` | `kmp` / `android` / `none` |
| 选项 | `--backend` | 交互默认 `Y` | `true`/`false`/`y`/`n` 等 |
| 选项 | `--web` | 交互默认 `Y` | 同上 |
| 选项 | `--output` / `-o` | `.` | 输出父目录 |
| 选项 | `--token` | — | GitHub HTTPS 克隆 PAT |
| 选项 | `--username` | `x-access-token` | Token 用户名 |
| 选项 | `--protocol` | 可配合 `EGS_ENGINE_GIT_PROTOCOL` | `ssh` / `https` |
| 选项 | `--client-template` | 内置 KMP/Android URL | 覆盖客户端模板 |
| 选项 | `--backend-template` | 内置后端 URL | 覆盖后端模板 |
| 选项 | `--web-template` | 内置管理端 URL | 覆盖 Web 模板 |
| 选项 | `--dry-run` | 关 | 仅预览 |

---

### `backend module create <name>` — 后端 feature 模块

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 模块名 | 必填 | |
| 选项 | `--project` / `-p` | `.` | 工作区根 |
| 选项 | `--dry-run` | 关 | 预览 |

`projectKey` 固定为 `backend`（见实现）。

---

### `client` — 客户端 KMP 侧脚手架

**`client module create <name>`**

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 模块名 | 必填 | |
| 选项 | `--project` / `-p` | `.` | 工作区根 |
| 选项 | `--dry-run` | 关 | 预览 |

**`client gen database <sql-file>`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 位置参数 | SQL 文件路径 | DDL（CREATE TABLE）；相对路径相对当前工作目录 |
| 选项 | `--module` / `-m` | **必填**，目标 `feature/<module>` |
| 选项 | `--project` / `-p` | 默认 `.` |
| 选项 | `--dry-run` | 预览 |
| 选项 | `--repo` | 生成 Repository 层 |
| 选项 | `--cached` | 与 API 同步并存时：cache-aside + 映射；隐含 `--repo` |

**`client gen prefs`**

| 类型 | 名称 | 说明 |
|------|------|------|
| 选项 | `--module` / `-m` | **必填** |
| 选项 | `--fields` / `--feilds`（拼写兼容） | **必填**；`name:type` 逗号分隔；类型：`String`、`Boolean`/`bool`、`Int`、`Long` |
| 选项 | `--key` / `-k` | 可选；单字段或多字段时的逻辑键 |
| 选项 | `--project` / `-p` | 默认 `.` |
| 选项 | `--dry-run` | 预览 |
| 选项 | `--force` | 覆盖快照/重复键（MVP 说明见 help） |

**`client api sync`**

模块解析优先级：`--client-module` / `--backend-module` > `--module` > 位置参数 `<module>`（前后端同名时）。

| 类型 | 名称 | 说明 |
|------|------|------|
| 位置参数 | `module` | 可选；与 `--module` 等二选一组合 |
| 选项 | `--module` / `-m` | 客户端与后端模块同名 |
| 选项 | `--client-module` | 仅客户端模块名 |
| 选项 | `--backend-module` | 仅后端模块名 |
| 选项 | `--swagger` / `-s` | 覆盖 Swagger JSON URL |
| 选项 | `--project` / `-p` | 默认 `.` |
| 选项 | `--dry-run` | 预览 |

---

### `web crud gen <name>` — 管理端 Vue3 CRUD

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 位置参数 | 模块名 | 必填 | 对应 `admin` 工程内生成 |
| 选项 | `--project` / `-p` | `.` | 工作区根 |
| 选项 | `--dry-run` | 关 | 预览 |

`projectKey` 固定为 `admin`（见实现）。

---

### `lint fix`

| 类型 | 名称 | 默认值 | 说明 |
|------|------|--------|------|
| 选项 | `--project` / `-p` | `.` | 当前为占位，输出 “not yet implemented” |

---

## 子命令树（速查）

```
egs-engine [--version|-v]
├── analyze [path] [--verbose] [--json]
├── command [--dir|-d <dir>] <shell...>
├── task list | add <name> <cmd>... | cancel <id>
├── script run <path> | list [dir] | validate <path>
├── init [projectPath]
├── create
│   ├── project
│   ├── module <name>
│   ├── api <moduleName>
│   ├── screen <NAME>
│   └── page
├── new project
├── backend module create <name>
├── client
│   ├── module create <name>
│   ├── gen database <sql> | ...
│   ├── gen prefs ...
│   └── api sync ...
├── web crud gen <name>
└── lint fix
```

更细的参数默认值与交互行为见上文各表。
