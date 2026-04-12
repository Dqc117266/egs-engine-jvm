/*
 * Codegen: egs client prefs <feature> --fields / --key
 */
package ${packageName}.generate.data.datasource.preferences.model

internal object ${featurePascal}PrefsKeys {
    const val NAMESPACE: String = "${namespace}"

<#if scalars?has_content>
    object Scalar {
<#list scalars as s>
        const val ${s.constName}: String = "${s.storageKey}"
</#list>
    }
</#if>

<#if snapshots?has_content>
    object Snapshot {
<#list snapshots as snap>
        const val ${snap.keyConstName}: String = "${snap.objectKey}"
</#list>
    }
</#if>

<#if listQueryKeys?has_content>
    object ListQuery {
<#list listQueryKeys as k>
        const val ${k.constName}: String = "${k.storageKey}"
</#list>
    }
</#if>
}
