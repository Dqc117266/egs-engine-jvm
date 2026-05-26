${warningGenerated}
package ${generatePackage}.data.datasource.cache.key

object ${entityPascal}CacheKeys {
    const val PREFIX: String = "egs:feature:${featureSegment}:"

    fun entityKey(id: Long): String = "${'$'}{PREFIX}${'$'}id"
}
