# egs-engine 本地调试工作流

本文档说明如何在 monorepo（`egs-engine` + `egs-*-template` + `demo-app`）中快速调试模板与代码生成，避免「改生成物 → 手动回写 GitHub 模板」的长链路。

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
./scripts/dev.sh preview client create module foo
./scripts/dev.sh apply client create module foo
./scripts/dev.sh verify client :feature:foo:compileKotlinMetadata
./scripts/dev.sh run create module foo --dry-run
./scripts/dev.sh sync-back client --paths core-base,build-logic --dry-run
./scripts/dev.sh promote-ftl client
./scripts/dev.sh test
```

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

```text
改模板源码 → dev.sh preview ... → dev.sh apply ... → dev.sh verify ... → template sync-back / promote-ftl → smoke.sh
```

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
