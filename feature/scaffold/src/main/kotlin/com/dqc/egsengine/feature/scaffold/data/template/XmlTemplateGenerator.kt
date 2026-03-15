package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

class XmlTemplateGenerator(private val template: ModuleTemplate) {

    private val pascal = template.name.toPascalCase()
    private val camel = pascal.replaceFirstChar { it.lowercase() }
    private val pkg = template.packageName

    fun generateLayout(): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<layout xmlns:android="http://schemas.android.com/apk/res/android">""")
        appendLine()
        appendLine("""    <data>""")
        appendLine("""    </data>""")
        appendLine()
        appendLine("""    <androidx.constraintlayout.widget.ConstraintLayout""")
        appendLine("""        android:layout_width="match_parent"""")
        appendLine("""        android:layout_height="match_parent">""")
        appendLine()
        appendLine("""    </androidx.constraintlayout.widget.ConstraintLayout>""")
        appendLine()
        appendLine("""</layout>""")
    }

    fun generateNavGraph(): String = buildString {
        val fragmentFqn = "$pkg.presentation.fragment.$camel.${pascal}Fragment"
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<navigation xmlns:android="http://schemas.android.com/apk/res/android"""")
        appendLine("""    xmlns:app="http://schemas.android.com/apk/res-auto"""")
        appendLine("""    android:id="@+id/${camel}NavGraph"""")
        appendLine("""    app:startDestination="@id/${camel}Fragment">""")
        appendLine()
        appendLine("""    <fragment""")
        appendLine("""        android:id="@+id/${camel}Fragment"""")
        appendLine("""        android:name="$fragmentFqn"""")
        appendLine("""        android:label="$pascal" />""")
        appendLine()
        appendLine("""</navigation>""")
    }

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
