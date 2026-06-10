# Claude Code — egs-engine

**Type:** Kotlin/JVM CLI tool (code generation engine)
**Version:** 0.0.1
**JVM:** 17 (toolchain 21 for Gradle daemon)

---

## Quick Reference

```bash
# Build
./gradlew shadowJar                    # Fat JAR → app/build/libs/

# Run
java -jar app/build/libs/egs-engine-*.jar --help

# Test
./gradlew test                         # All module tests
./gradlew :feature:scaffold:test       # Scaffold tests only

# Golden snapshot
./gradlew :feature:scaffold:test -Degs.golden.update=true   # Update goldens

# Code quality
./gradlew detektCheck spotlessCheck
./gradlew detektApply                 # Auto-fix detekt issues
./gradlew spotlessApply               # Auto-fix formatting
```

---

## Project Overview

egs-engine is a **multi-platform code scaffolding engine**. It generates boilerplate code for Android, KMP, Spring Boot, and Vue3 projects from templates and Swagger/OpenAPI specs.

**Tech stack:** Clikt (CLI) · Koin (DI) · FreeMarker (templates) · KotlinPoet (code gen) · Retrofit/OkHttp · SLF4J/Logback

---

## Architecture

```
egs-engine/
├── app/                    # Entry point (Clikt command tree + Koin init)
├── feature/
│   ├── common/             # AppConfig, ConfigRepository (shared)
│   ├── base/               # CommandExecutor, CliFormatter, ProjectRootResolver
│   ├── command/            # Shell command execution
│   ├── task/               # Task queue
│   ├── script/             # Script loading [NOT FULLY IMPLEMENTED]
│   ├── analyzer/           # Gradle project scanning (Tooling API)
│   ├── init/               # Project init, workspace config, base class scanning
│   ├── scaffold/           # ★ Core: code generation (Android/KMP/SpringBoot/Vue3)
│   └── template-engine/    # FreeMarker template rendering with 4-tier override
├── konsist-test/           # Architecture guard tests
├── build-logic/            # Gradle convention plugins
└── docs/                   # Documentation
```

### Module Dependencies (enforced by FeatureConventionPlugin)

```
feature:common          ← no feature deps
feature:base            ← depends on common
feature:* (all others)  ← depends on base + common
feature:scaffold        ← also depends on init + template-engine
```

### Layer Rules (per feature module)

```
presentation/   ← Clikt commands (CLI layer)
domain/         ← Business logic, models, interfaces
data/           ← Implementations: generators, parsers, scanners
di/             ← Koin module definitions
```

**Rules:**
- `domain` MUST NOT import from `data` or `presentation`
- `presentation` MUST NOT import from `data` directly (use `domain` interfaces)
- `data` classes use suffixes: `Repository`, `Impl`, `Generator`, `Parser`, `Scanner`, `Updater`

---

## Key Concepts

### Golden Snapshot Testing
Generated code is compared byte-exact against committed golden files in `src/test/resources/golden/`.
- **Verify** (default): fails on any diff
- **Update**: `-Degs.golden.update=true` rewrites goldens — always review with `git diff`

### Template Override Chain (4 tiers, highest priority first)
1. `EGS_TEMPLATE_ROOT` env variable
2. `<project>/.egs/templates/`
3. `~/.egs/templates/`
4. Classpath `templates/` (bundled defaults)

### FreeMarker Templates
89 `.ftl` files under `feature/template-engine/src/main/resources/templates/`:
- `android/` — module, page, swagger, database, prefs
- `kmp/` — module, page, swagger, database, preferences
- `springboot/` — database (JPA CRUD)
- `vue3/admin/` — TypeScript/Vue admin panel

### Code Generation Flow
1. Parse input (Swagger JSON / DDL SQL / CLI args)
2. Build domain model (entities, endpoints, fields)
3. Render via FreeMarker templates (4-tier override)
4. Post-process (package rewrites, DI module updates, navigation wiring)

---

## Important Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | All dependency versions |
| `detekt.yml` | Static analysis config (maxIssues: 0) |
| `gradle.properties` | Build config + debug/release API URLs |
| `feature/scaffold/src/test/resources/golden/` | Golden snapshot baselines |
| `feature/template-engine/src/main/resources/templates/` | FreeMarker templates |

---

## Conventions

- **Code style:** ktlint via Spotless (standard ruleset)
- **Static analysis:** Detekt with zero tolerance
- **Logging:** SLF4J — never use `printStackTrace()` or `println()`
- **Testing:** JUnit 5 + MockK + Kluent + Konsist
- **Commit messages:** Conventional commits (`feat:`, `fix:`, `refactor:`, etc.)

---

## Known Limitations

- `SpringBootApiGenerator` and `Vue3ApiGenerator` throw `UnsupportedOperationException` (not yet implemented)
- `ScriptCli` command is a skeleton — not connected to CommandService
- `template-engine` package uses `com.dqc.egsengine.template` instead of `feature.templateengine`
- Configuration cache is disabled (`org.gradle.configuration-cache=false`)
