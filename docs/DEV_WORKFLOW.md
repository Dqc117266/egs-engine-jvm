# egs-engine 本地调试工作流

本文档说明如何在 monorepo（`egs-engine` + `egs-*-template` + `demo-app`）中快速调试模板与代码生成，避免「改生成物 → 手动回写 GitHub 模板」的长链路。

## TL;DR：分层快速反馈循环

把验证分成 4 层，越底层越快、越自动。日常编辑只盯 L0/L1，跑 app、视觉快照按需触发。

| 层 | 命令 | 速度 | 作用 |
|----|------|------|------|
| L0 Golden 快照 | `./scripts/dev.sh watch` | 毫秒 | 改 FTL/引擎源码自动重跑 golden + 单测，打印产物 diff |
| L1 真实 regen | `./scripts/dev.sh regen client create module foo` | 秒 | 重置沙箱 → 生成 → `git diff`，`--full` 看全量、`--verify` 顺带编译 |
| L2 admin 跑起来 | `./scripts/dev.sh admin-dev` | 按需 | 常驻 Vite dev server，改动即 HMR |
| L3 视觉快照 | `./scripts/dev.sh admin-snap` | 按需 | Playwright 截图对比，`--update` 刷新基线 |

- 改一个 FTL / 引擎逻辑：开一个 `dev.sh watch` 终端，存盘即看 golden diff。
- 要看真实文件落地到项目：`dev.sh regen <stack> <egs args...>`，看 `git diff`；`dev.sh reset <stack>` 一键还原沙箱。
- 改 admin：`dev.sh admin-dev` 常驻看 HMR；UI 回归用 `dev.sh admin-snap` 兜底。

## 原则

1. **模板源码是真相源**：优先直接改 `egs-*-template` 或 `feature/template-engine/.../templates/*.ftl`
2. **demo-app 是持久沙箱**：日常只在 demo-app 上验证 CLI，不必反复 `new project`
3. **GitHub 只做发布**：本地 sibling 模板仓库即开发真相源

## 环境变量

| 变量 | 用途 |
|------|------|
| `EGS_TEMPLATE_ROOT` | FreeMarker 模板最高优先级目录（开发时指向 `feature/template-engine/src/main/resources/templates`） |
| `EGS_DEBUG=true` | `create page/screen` 失败时打印堆栈 |
| `EGS_ENGINE_GIT_PROTOCOL` | `new project` 克隆协议（ssh/https） |

## 快速命令

### 运行 CLI（无需打 jar）

```bash
cd egs-engine
./gradlew :app:run --args="create module foo --project ../demo-app/client --dry-run"
```

### monorepo 开发脚本

```bash
# 在 egs 根目录
# —— 快速反馈循环 ——
./scripts/dev.sh watch                                  # 持续构建 golden + 单测（改源码自动重跑）
./scripts/dev.sh golden                                 # 跑一次 golden 快照测试
./scripts/dev.sh golden --update                        # 刷新 golden 期望语料，然后看 git diff
./scripts/dev.sh regen client create module foo         # 重置→生成→git diff
./scripts/dev.sh regen client --full --verify create module foo  # 全量 diff + 编译
./scripts/dev.sh reset client                           # 还原 demo-app/client 到基线
./scripts/dev.sh all create module foo                  # 三端依次 regen
./scripts/dev.sh admin-dev                              # admin 常驻 Vite dev server（HMR）
./scripts/dev.sh admin-snap                             # admin 视觉快照对比

# —— CLI / 模板 ——
./scripts/dev.sh preview client create module foo
./scripts/dev.sh apply client create module foo
./scripts/dev.sh verify client :feature:foo:compileKotlinMetadata
./scripts/dev.sh run create module foo --dry-run
./scripts/dev.sh sync-back client --paths core-base,build-logic --dry-run
./scripts/dev.sh promote-ftl client
./scripts/dev.sh test
```

### Golden 快照（产物回归）

`create module / page / api` 的产物以提交在 `feature/scaffold/src/test/resources/golden/` 的「期望树」为基准（覆盖 ANDROID/KMP × module/page/api）。任何模板/引擎改动若改变产物，golden 测试会逐行打印 diff 并失败：

```bash
./scripts/dev.sh watch          # 边改边看（推荐）
./scripts/dev.sh golden         # 一次性校验
./scripts/dev.sh golden --update   # 改动符合预期时刷新基线，再 git diff 复核
```

`watch`/`golden` 会自动取消 `EGS_TEMPLATE_ROOT`，让 golden 始终对比 bundled（= 源码）模板，结果可复现。CI（`.github/workflows/ci.yml`）也会跑这套测试。

### demo-app 沙箱（git 基线）

`demo-app` 已是一个独立 git 仓库，初始提交即「干净基线」。`regen` 会先把目标子项目重置到基线再生成，并用 `git diff` 展示真实落地的文件；`reset` 用 `git checkout + clean` 还原（不会动 `node_modules`/`build` 等忽略文件）。

### admin：HMR + 视觉快照

```bash
./scripts/dev.sh admin-dev      # 常驻 Vite dev server，改 Vue/模板即 HMR
# 一次性准备 Playwright 浏览器（仅首次）：
( cd demo-app/admin && npx playwright install chromium )
./scripts/dev.sh admin-snap            # 对关键页面截图并和基线对比
./scripts/dev.sh admin-snap --update   # UI 改动符合预期时刷新基线
```

视觉基线提交在 `demo-app/admin/tests/`，配置见 `demo-app/admin/playwright.config.ts`。

### Git 模板反向同步

在 demo-app 里改完、验证通过后，回灌到本地模板仓库：

```bash
./gradlew :app:run --args="template sync-back --from ../demo-app/client --paths core-base --dry-run"
./gradlew :app:run --args="template sync-back --from ../demo-app/client --paths core-base"
```

`--to` 可省略（从 `demo-app/.egs/workspace.json` 的 `templateUrl` 读取）。

### FreeMarker 热迭代

**方式 A**：项目级覆盖（无需重新编译 engine）

```bash
mkdir -p demo-app/client/.egs/templates/android/module
cp egs-engine/feature/template-engine/src/main/resources/templates/android/module/ViewModel.kt.ftl \
   demo-app/client/.egs/templates/android/module/
# 编辑 demo-app/client/.egs/templates/... 后运行 create 命令验证
```

**方式 B**：开发时设置 `EGS_TEMPLATE_ROOT`

```bash
export EGS_TEMPLATE_ROOT=/Volumes/Expend/egs/egs-engine/feature/template-engine/src/main/resources/templates
./gradlew :app:run --args="create module foo --project ../demo-app/client --dry-run"
```

验证通过后晋升到 bundled 模板：

```bash
./gradlew :app:run --args="template promote-ftl --project ../demo-app/client --dry-run"
```

## 推荐日常循环

FTL / 引擎逻辑（最快）：

```text
改模板/引擎源码 → dev.sh watch（golden 自动 diff）→ dev.sh regen <stack> ...（看真实 git diff）→ template sync-back / promote-ftl
```

admin（Vue）：

```text
改 admin 代码/模板 → dev.sh admin-dev（HMR 实时看）→ dev.sh admin-snap（视觉回归）→ sync-back
```

旧的 `test-compose-*.sh` 与 `build-and-test.sh`（指向已不存在的 sibling `kango` 目录）已删除：产物回归改用 golden 快照，本地跑 CLI 用 `dev.sh run/regen`，打 jar 用 `./gradlew :app:shadowJar`（如需复制到 kango 仍可用 `build-to-kango.sh`）。

## 两条调试路径

### 完整项目模板（`egs-*-template`）

优先在 `demo-app/<client|backend|admin>` 里复现和验证，验证后用 `sync-back` 回灌到本地模板仓库：

```bash
./scripts/dev.sh sync-back client --paths core-base,build-logic --dry-run
./scripts/dev.sh sync-back client --paths core-base,build-logic
```

本地 sibling 模板仓库是开发真相源，GitHub 模板仓库只在发布时推送。

### 重复代码生成模板（FTL）

`create module`、`create page` 和 `create api` 已走 `feature/template-engine` 的 FreeMarker 模板，开发时会按以下优先级解析：

1. `EGS_TEMPLATE_ROOT`
2. `<project>/.egs/templates/`
3. `~/.egs/templates/`
4. JAR 内置 `templates/`

最快循环：

```bash
./scripts/dev.sh ftl-edit kmp/module/ViewModel.kt.ftl client
./scripts/dev.sh preview client create module debugFeature
./scripts/dev.sh ftl-edit kmp/page/PageScreen.kt.ftl client
./scripts/dev.sh preview client create page --module profile --name DebugPage
./scripts/dev.sh ftl-edit android/swagger/UseCase.kt.ftl client
./scripts/dev.sh preview client create api task --swagger ./swagger.json
./scripts/dev.sh apply client create module debugFeature
./scripts/dev.sh verify client :feature:debugFeature:compileKotlinMetadata
./scripts/dev.sh promote-ftl client --dry-run
./scripts/dev.sh promote-ftl client
```

若直接改 bundled 模板源码，可跳过 `ftl-edit/promote-ftl`：

```bash
export EGS_TEMPLATE_ROOT=/Volumes/Expend/egs/egs-engine/feature/template-engine/src/main/resources/templates
./scripts/dev.sh preview client create module debugFeature
```

`create api` 默认使用 FreeMarker。若某个 swagger 场景需要临时回退旧 KotlinPoet 生成器：

```bash
EGS_SWAGGER_GENERATOR=kotlinpoet ./scripts/dev.sh preview client create api task --swagger ./swagger.json
```

## 冒烟测试

```bash
./scripts/smoke.sh
```
