plugins {
<#if conventionPluginId??>
    id("${conventionPluginId}")
<#else>
    id("org.jetbrains.kotlin.jvm")
</#if>
}

<#if android && namespace??>
android {
    namespace = "${namespace}"
}
</#if>
