package com.dqc.egsengine.feature.scaffold.data.generator.vue3

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Creates Vue3 feature module skeletons:
 * - api/<module>.ts
 * - views/<module>/index.vue
 * - stores/<module>.ts
 * - types/<module>.ts
 * - router/modules/<module>.ts
 *
 * Skeleton -- concrete .vue/.ts template generation to be implemented later.
 */
class Vue3ModuleGenerator : PlatformModuleGenerator {

    private val logger = LoggerFactory.getLogger(Vue3ModuleGenerator::class.java)

    override val platform: Platform = Platform.VUE3

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        val srcBase = "src"

        files.add(GeneratedFile("$srcBase/api/$moduleName.ts", generateApiStub(moduleName)))
        files.add(GeneratedFile("$srcBase/views/$moduleName/index.vue", generateViewStub(moduleName)))
        files.add(GeneratedFile("$srcBase/stores/$moduleName.ts", generateStoreStub(moduleName)))
        files.add(GeneratedFile("$srcBase/types/$moduleName.ts", generateTypesStub(moduleName)))
        files.add(GeneratedFile("$srcBase/router/modules/$moduleName.ts", generateRouterStub(moduleName)))
        files.add(
            GeneratedFile(
                "$srcBase/sql/sys_menu_snippet_$moduleName.sql",
                generateSysMenuInsertSnippet(moduleName),
            ),
        )

        return files
    }

    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        // projectRoot is already the sub-project root (resolved by caller in scaffoldForProject)
        val created = mutableListOf<File>()

        for (entry in preview(projectRoot, moduleName, config)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()
            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }
            created.add(file)
            logger.debug("Created: {}", entry.path)
        }

        logger.info("Generated {} files for Vue3 module '{}'", created.size, moduleName)
        return created
    }

    override fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        logger.info("Vue3 router auto-registration for '{}' -- not yet implemented", moduleName)
    }

    private fun generateApiStub(moduleName: String): String = buildString {
        val pascal = moduleName.toPascal()
        appendLine("import request from '@/utils/request'")
        appendLine()
        appendLine("// TODO: Implement $pascal API methods")
        appendLine("export function get${pascal}List(params?: any) {")
        appendLine("  return request.get('/api/$moduleName', { params })")
        appendLine("}")
    }

    private fun generateViewStub(moduleName: String): String = buildString {
        val pascal = moduleName.toPascal()
        appendLine("<template>")
        appendLine("  <div class=\"${moduleName}-container\">")
        appendLine("    <!-- TODO: Implement $pascal view -->")
        appendLine("    <h1>$pascal</h1>")
        appendLine("  </div>")
        appendLine("</template>")
        appendLine()
        appendLine("<script setup lang=\"ts\">")
        appendLine("// TODO: Implement $pascal logic")
        appendLine("</script>")
    }

    private fun generateStoreStub(moduleName: String): String = buildString {
        val pascal = moduleName.toPascal()
        appendLine("import { defineStore } from 'pinia'")
        appendLine()
        appendLine("export const use${pascal}Store = defineStore('$moduleName', {")
        appendLine("  state: () => ({")
        appendLine("    // TODO: Define state")
        appendLine("  }),")
        appendLine("  actions: {")
        appendLine("    // TODO: Define actions")
        appendLine("  },")
        appendLine("})")
    }

    private fun generateTypesStub(moduleName: String): String = buildString {
        val pascal = moduleName.toPascal()
        appendLine("// TODO: Define $pascal types")
        appendLine("export interface ${pascal}Item {")
        appendLine("  id: number")
        appendLine("}")
    }

    private fun generateRouterStub(moduleName: String): String = buildString {
        val pascal = moduleName.toPascal()
        appendLine("import type { RouteRecordRaw } from 'vue-router'")
        appendLine()
        appendLine("const ${moduleName}Routes: RouteRecordRaw[] = [")
        appendLine("  {")
        appendLine("    path: '/$moduleName',")
        appendLine("    name: '$pascal',")
        appendLine("    component: () => import('@/views/$moduleName/index.vue'),")
        appendLine("  },")
        appendLine("]")
        appendLine()
        appendLine("export default ${moduleName}Routes")
    }

    private fun String.toPascal(): String =
        split("-", "_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }

    /**
     * Flyway-ready snippet for `egs-server-template` `sys_menus` (see V5__sys_menu.sql).
     * Merge into a new migration; set [parent_id] if the menu should sit under a directory row.
     */
    private fun generateSysMenuInsertSnippet(moduleName: String): String {
        val pascal = moduleName.toPascal()
        val title = pascal.replaceFirstChar { it.titlecase() }
        val routeName = "${pascal}Admin"
        val perm = "$moduleName:list"
        return buildString {
            appendLine("-- EGS Engine: copy into egs-server-template db/migration as part of a new Flyway script.")
            appendLine("INSERT INTO sys_menus (")
            appendLine("    parent_id, menu_name, order_num, path, component, route_name,")
            appendLine("    menu_type, visible, status, perms, icon")
            appendLine(") VALUES (")
            appendLine("    NULL,")
            appendLine("    '$title',")
            appendLine("    100,")
            appendLine("    '$moduleName',")
            appendLine("    '$moduleName/index',")
            appendLine("    '$routeName',")
            appendLine("    'C', TRUE, '0', '$perm', 'Document');")
            appendLine()
            appendLine("-- Add matching permissions to `permissions` / `role_permissions` in the same migration if needed.")
        }
    }
}
