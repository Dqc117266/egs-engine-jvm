# KMP 项目与 egs-engine

## `egs init` 与 `baseClasses`

初始化时会扫描每个 Gradle 模块下所有 `src/<sourceSet>/kotlin`（以及 `java`）目录，因此 **KMP 的 `commonMain` / `androidMain` 等** 中的 `abstract class Base*` / `open class Base*` 会写入 `.egs/config.json` 的 `baseClasses`（例如 `egs-kmp-template` 里 `:core-base:ui` 的 `template.core.base.ui.BaseViewModel`）。

根包 `basePackage` 会从 `feature/**/src/*/kotlin` 下的目录结构推断（优先 `commonMain`），例如 `org.mifos.feature.*` → `org.mifos`。若 core 模块使用另一根包（如 `template.core.*`），可通过 `scaffoldOverrides.basePackage` 与 `baseViewModelFqn` 对齐脚手架生成。

## 页面脚手架（`PageScaffolder`）与 Compose/KMP

页面脚手架生成的是 **Android Fragment + Data Binding XML + Koin** 风格的代码路径（`feature/<name>/src/main/...`），面向传统单模块 Android feature 布局。

**Compose Multiplatform / 纯 KMP feature**（如 `src/commonMain` 下的 `Screen`）结构不同：没有 `BaseFragment` 时引擎**不会生成 Fragment**，`BaseViewModel` 若来自共享模块则可用于生成 ViewModel/Contract，但整体仍偏 Android 模板。为 CMP 项目生成页面时，请优先在模板内手写 Compose 页面，或后续为引擎增加 CMP 专用脚手架；不要将当前页面脚手架默认当作 CMP 的标准流程。

## 可选配置 `scaffoldOverrides`

若自动扫描仍找不到基类（命名特殊、多模块重复等），可在 `.egs/config.json` 中增加：

```json
"scaffoldOverrides": {
  "baseViewModelFqn": "template.core.base.ui.BaseViewModel",
  "baseFragmentFqn": null,
  "basePackage": "org.mifos"
}
```

- `baseViewModelFqn` / `baseFragmentFqn`：完整限定类名，优先于 `baseClasses` 列表中的同名项。
- `basePackage`：若设置，则覆盖检测到的 `basePackage`，用于模块包名与 `Result` 等类型的包推断。

旧版 JSON 可省略 `scaffoldOverrides`，行为与之前兼容。
