package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

class XmlTemplateGenerator(private val template: ModuleTemplate) {

    private val pascal = template.name.toPascalCase()
    private val pkg = template.packageName

    fun generateAndroidManifest(): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<manifest xmlns:android="http://schemas.android.com/apk/res/android">""")
        appendLine("""</manifest>""")
    }

    private fun String.toPascalCase(): String =
        split("-", "_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
}
