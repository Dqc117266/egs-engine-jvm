<#--
  KMP prefs: TypedPreferenceStore datasource (optional override).
  See KmpPreferencesScaffolder for generated bodies.
-->
package ${prefsPackage}

import kotlinx.coroutines.flow.Flow
import ${modelPackage}.${modulePascal}PrefsKeys
import template.core.base.preferences.TypedPreferenceStore

internal class ${modulePascal}PreferencesDataSource(
    private val store: TypedPreferenceStore,
) {
    // egs-gen:prefs-scalar-datasource-begin
    // egs-gen:prefs-scalar-datasource-end

    // egs-gen:prefs-snapshot-datasource-begin
    // egs-gen:prefs-snapshot-datasource-end
}
