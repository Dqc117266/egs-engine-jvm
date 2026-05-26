${warningGenerated}
package ${generatePackage}.data.datasource.cache

import ${generatePackage}.data.datasource.cache.model.${entityPascal}CacheModel
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

open class ${entityPascal}CacheDataSource(
    private val redis: RedisTemplate<String, Any>,
) {
    open fun get(key: String): ${entityPascal}CacheModel? {
        @Suppress("UNCHECKED_CAST")
        return redis.opsForValue().get(key) as? ${entityPascal}CacheModel
    }

    open fun put(
        key: String,
        value: ${entityPascal}CacheModel,
        ttl: Duration = DEFAULT_TTL,
    ) {
        redis.opsForValue().set(key, value, ttl)
    }

    open fun delete(key: String) {
        redis.delete(key)
    }

    private companion object {
        val DEFAULT_TTL: Duration = Duration.ofHours(1)
    }
}
