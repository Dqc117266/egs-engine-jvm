${warningGenerated}
package ${generatePackage}.data.datasource.httpclient

import ${generatePackage}.data.datasource.httpclient.model.${entityPascal}RemoteItemHttpModel
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

open class ${entityPascal}HttpClient(
    private val restClient: RestClient,
    private val baseUrl: String,
) {
    open fun fetchById(id: Long): ${entityPascal}RemoteItemHttpModel? {
        val root = baseUrl.trim().trimEnd('/')
        if (root.isEmpty()) return null
        return try {
            restClient
                .get()
                .uri("${'$'}root/api/${restPath}/{id}", id)
                .retrieve()
                .body(${entityPascal}RemoteItemHttpModel::class.java)
        } catch (_: RestClientException) {
            null
        }
    }
}
