# Local Template Debug Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make egs-engine local template debugging fast by improving the existing dev script workflow and wiring module scaffolding to FreeMarker templates.

**Architecture:** Keep `demo-app` as the persistent sandbox and local sibling template repositories as the editable source for project templates. Route `create module` through `feature/template-engine` so `EGS_TEMPLATE_ROOT` and `<project>/.egs/templates` overrides affect generated code without rebuilding hard-coded KotlinPoet generators.

**Tech Stack:** Kotlin, Gradle, Clikt, Koin, FreeMarker, Bash.

---

### Task 1: Add Module Template Override Tests

**Files:**
- Test: `feature/scaffold/src/test/kotlin/com/dqc/egsengine/feature/scaffold/data/ModuleGeneratorTemplateEngineTest.kt`

- [ ] **Step 1: Write failing tests**

Add tests that assert `ModuleGenerator.preview()` uses project-local FTL overrides and that KMP projects render `kmp/module` templates into `src/commonMain/kotlin`.

- [ ] **Step 2: Run tests to verify failure**

Run: `./gradlew :feature:scaffold:test --tests '*ModuleGeneratorTemplateEngineTest'`

Expected: FAIL because `ModuleGenerator` still uses KotlinPoet and ignores project-local FTL overrides.

- [ ] **Step 3: Implement minimal FreeMarker-backed module rendering**

Modify `ModuleGenerator` to depend on `TemplateEngine`, build Android/KMP template models, render the module FTL files, and keep existing preview/generate semantics.

- [ ] **Step 4: Run tests to verify pass**

Run: `./gradlew :feature:scaffold:test --tests '*ModuleGeneratorTemplateEngineTest'`

Expected: PASS.

### Task 2: Wire Template Engine Dependency

**Files:**
- Modify: `feature/scaffold/build.gradle.kts`
- Modify: `feature/scaffold/src/main/kotlin/com/dqc/egsengine/feature/scaffold/di/ScaffoldModule.kt`

- [ ] **Step 1: Add dependency**

Add `implementation(projects.feature.templateEngine)` to scaffold.

- [ ] **Step 2: Register TemplateEngine**

Register `TemplateRegistry` and `TemplateEngine` in `featureScaffoldModule`, then inject it into `ModuleGenerator`.

- [ ] **Step 3: Run scaffold tests**

Run: `./gradlew :feature:scaffold:test`

Expected: PASS.

### Task 3: Improve Dev Script Workflow

**Files:**
- Modify: `../scripts/dev.sh`
- Modify: `docs/DEV_WORKFLOW.md`

- [ ] **Step 1: Add sandbox helpers**

Add `preview`, `apply`, and `verify` commands so local debugging does not require repeating `--project ../demo-app/<subproject>`.

- [ ] **Step 2: Document the daily loop**

Update the dev workflow with the faster commands and the project-template versus FTL-template paths.

- [ ] **Step 3: Run smoke verification**

Run: `./scripts/dev.sh test` and `./scripts/dev.sh preview client create module __smoke_test__`.

Expected: both commands succeed.
