package com.dqc.egsengine.feature.init.data

import com.dqc.egsengine.feature.init.domain.BaseClassScanner
import com.dqc.egsengine.feature.init.domain.model.BaseClassInfo
import com.dqc.egsengine.feature.init.domain.model.BaseClassKind
import org.slf4j.LoggerFactory
import java.io.File

class BaseClassScannerImpl : BaseClassScanner {
    private val logger = LoggerFactory.getLogger(BaseClassScannerImpl::class.java)

    private val kotlinBaseClassPattern = Regex(
        """(?:abstract|open)\s+class\s+(Base\w+)""",
    )
    private val javaBaseClassPattern = Regex(
        """(?:public\s+)?abstract\s+class\s+(Base\w+)""",
    )
    private val packagePattern = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)

    override fun scan(projectRoot: File, modules: List<String>): List<BaseClassInfo> {
        val results = mutableListOf<BaseClassInfo>()

        for (modulePath in modules) {
            val relDir = modulePath.removePrefix(":").replace(':', File.separatorChar)
            val moduleDir = projectRoot.resolve(relDir)
            if (!moduleDir.isDirectory) continue

            for (sourceRoot in GradleSourceRoots.orderedKotlinRoots(moduleDir)) {
                scanDirectory(sourceRoot, sourceRoot, modulePath, projectRoot, results)
            }
            for (sourceRoot in GradleSourceRoots.orderedJavaRoots(moduleDir)) {
                scanDirectory(sourceRoot, sourceRoot, modulePath, projectRoot, results)
            }
        }

        val deduped = results.distinctBy { "${it.packageName}.${it.name}" }
        logger.info("Found ${deduped.size} base classes across ${modules.size} modules")
        return deduped
    }

    private fun scanDirectory(
        dir: File,
        sourceRoot: File,
        modulePath: String,
        projectRoot: File,
        results: MutableList<BaseClassInfo>,
    ) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, sourceRoot, modulePath, projectRoot, results)
            } else if (file.extension == "kt" || file.extension == "java") {
                scanFile(file, sourceRoot, modulePath, projectRoot, results)
            }
        }
    }

    private fun scanFile(
        file: File,
        sourceRoot: File,
        modulePath: String,
        projectRoot: File,
        results: MutableList<BaseClassInfo>,
    ) {
        val content = file.readText()
        val packageName = packagePattern.find(content)?.groupValues?.get(1) ?: ""

        val pattern = if (file.extension == "kt") kotlinBaseClassPattern else javaBaseClassPattern
        for (match in pattern.findAll(content)) {
            val className = match.groupValues[1]
            val kind = if (match.value.trimStart().startsWith("abstract")) {
                BaseClassKind.ABSTRACT_CLASS
            } else {
                BaseClassKind.OPEN_CLASS
            }

            results.add(
                BaseClassInfo(
                    name = className,
                    packageName = packageName,
                    module = modulePath,
                    filePath = file.relativeTo(projectRoot).path,
                    kind = kind,
                ),
            )
            logger.debug("Found base class: $className in $modulePath")
        }
    }
}
