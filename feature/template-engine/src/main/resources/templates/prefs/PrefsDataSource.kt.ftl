/*
 * Extend this template when running `egs client prefs <feature> --fields` / `--key`.
 * Wire [TypedPreferenceStore] with namespace [${featurePascal}PrefsKeys.NAMESPACE].
 */
package ${packageName}.generate.data.datasource.preferences

import template.core.base.preferences.TypedPreferenceStore

internal class ${featurePascal}PreferencesDataSource(
    private val store: TypedPreferenceStore,
) {
    // region Scalar / Snapshot accessors (generator expands from --fields / --key)
}
