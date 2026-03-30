package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

class XmlTemplateGenerator(private val template: ModuleTemplate) {

    fun generateAndroidManifest(): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<manifest xmlns:android="http://schemas.android.com/apk/res/android">""")
        appendLine("""</manifest>""")
    }
}
