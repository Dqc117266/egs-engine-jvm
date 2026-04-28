${warningGenerated}
package ${generatePackage}.api.controller

import ${generatePackage}.api.dto.Create${entityPascal}Request
import ${generatePackage}.api.dto.${entityPascal}Response
import ${generatePackage}.api.dto.Update${entityPascal}Request
import ${generatePackage}.data.mapper.Generated${entityPascal}EntityMapper
import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.usecase.Count${entityPascal}UseCase
import ${generatePackage}.domain.usecase.Create${entityPascal}UseCase
import ${generatePackage}.domain.usecase.Delete${entityPascal}ByIdUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}AllUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}ByIdUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}PageUseCase
import ${generatePackage}.domain.usecase.Update${entityPascal}UseCase
import ${sharedRoot}.common.api.R
import ${sharedRoot}.common.exception.NotFoundException
import ${sharedRoot}.common.pagination.PageResult
import ${sharedRoot}.infrastructure.pagination.toPageResult
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/api/${restPath}")
open class Generated${entityPascal}Controller(
    private val create${entityPascal}: Create${entityPascal}UseCase,
    private val update${entityPascal}: Update${entityPascal}UseCase,
    private val delete${entityPascal}ById: Delete${entityPascal}ByIdUseCase,
    private val get${entityPascal}ById: Get${entityPascal}ByIdUseCase,
    private val get${entityPascal}Page: Get${entityPascal}PageUseCase,
    private val get${entityPascal}All: Get${entityPascal}AllUseCase,
    private val count${entityPascal}: Count${entityPascal}UseCase,
    private val mapper: Generated${entityPascal}EntityMapper,
) {

    @PostMapping
    open fun create(
        @Valid @RequestBody body: Create${entityPascal}Request,
    ): R<${entityPascal}Response> {
        val toCreate =
            ${entityPascal}(
                ${pkProp} = 0L,
<#list businessNonPkColumns as col>
                ${col.kotlinName} = body.${col.kotlinName}<#if col.kotlinName == "status" && col.kotlinType == "Int"> ?: 1</#if><#if col_has_next>,</#if>
</#list>
            )
        val saved = create${entityPascal}(toCreate)
        return R.ok(mapper.toResponse(saved))
    }

    @PutMapping("/{id}")
    open fun update(
        @PathVariable id: Long,
        @Valid @RequestBody body: Update${entityPascal}Request,
    ): R<${entityPascal}Response> {
        val current =
            get${entityPascal}ById(id) ?: throw NotFoundException("${entityPascal} not found: ${'$'}id")
        val merged =
            current.copy(
<#list businessNonPkColumns as col>
                ${col.kotlinName} = body.${col.kotlinName} ?: current.${col.kotlinName}<#if col_has_next>,</#if>
</#list>
            )
        val saved = update${entityPascal}(merged)
        return R.ok(mapper.toResponse(saved))
    }

    @DeleteMapping("/{id}")
    open fun delete(
        @PathVariable id: Long,
    ): R<Unit> {
        if (!delete${entityPascal}ById(id)) {
            throw NotFoundException("${entityPascal} not found: ${'$'}id")
        }
        return R.ok()
    }

    @GetMapping("/{id}")
    open fun getById(
        @PathVariable id: Long,
    ): R<${entityPascal}Response> {
        val item = get${entityPascal}ById(id) ?: throw NotFoundException("${entityPascal} not found: ${'$'}id")
        return R.ok(mapper.toResponse(item))
    }

    @GetMapping
    open fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): R<PageResult<${entityPascal}Response>> = listPage(page, size)

    @GetMapping("/list")
    open fun listPath(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): R<PageResult<${entityPascal}Response>> = listPage(page, size)

    private fun listPage(
        page: Int,
        size: Int,
    ): R<PageResult<${entityPascal}Response>> {
        val pageable = PageRequest.of(page, size.coerceIn(1, 200), Sort.by(Sort.Direction.DESC, "${pkProp}"))
        val slice = get${entityPascal}Page(pageable).map { mapper.toResponse(it) }
        return R.ok(slice.toPageResult())
    }

    @GetMapping("/all")
    open fun all(): R<List<${entityPascal}Response>> =
        R.ok(get${entityPascal}All().map { mapper.toResponse(it) })

    @GetMapping("/count")
    open fun count(): R<Long> = R.ok(count${entityPascal}())
}
