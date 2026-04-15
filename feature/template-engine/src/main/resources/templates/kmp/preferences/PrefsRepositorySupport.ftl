<#-- Delegates PrefsRepository to PreferencesDataSource -->
package ${dataRepositoryPackage}

import ${prefsPackage}.${modulePascal}PreferencesDataSource
import ${domainRepositoryPackage}.${modulePascal}PrefsRepository

internal class Generated${modulePascal}PrefsRepositorySupport(
    private val prefs: ${modulePascal}PreferencesDataSource,
) : ${modulePascal}PrefsRepository {
    // egs-gen:prefs-support-begin
    // egs-gen:prefs-support-end
}
