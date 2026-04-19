/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template.model

/** Single use-case parameter for page templates. */
data class PageUseCaseParamModel(
    val name: String,
    val type: String,
    /** Fully qualified Kotlin type (for imports). */
    val kotlinType: String,
    /** Short type for Contract / ViewModel parameters ([contractImports] cover FQNs). */
    val kotlinTypeContractRef: String,
    val placeholderValue: String,
)

/** Use case metadata for Contract / ViewModel / Screen generation. */
data class PageUseCaseModel(
    val name: String,
    val camelName: String,
    val packageName: String,
    val returnType: String?,
    val parameters: List<PageUseCaseParamModel>,
    /** Simple name without UseCase suffix for intents. */
    val intentName: String,
    /** e.g. handleTopicUpdateTopic */
    val handlerName: String,
)

/** Extra state field on Contract.State from use case return types. */
data class PageStateFieldModel(
    val name: String,
    /** Fully qualified type used for import collection (primitives stay short: [Long], [List]). */
    val typeFqn: String,
    /** Short type for generated [State] properties ([contractImports] cover domain types). */
    val typeContractRef: String,
    val nullable: Boolean,
    /** When [nullable] is false, Kotlin default expression (e.g. [emptyList()], [0], [DEFAULT_FIRST_PAGE]). */
    val defaultLiteral: String? = null,
)

/** Nested intent type under Contract.Intent. */
data class PageIntentInnerModel(
    val simpleName: String,
    /** True when the intent has no parameters (object vs data class in generated code). */
    val emptyParams: Boolean,
    val params: List<PageUseCaseParamModel>,
)

/** ViewModel handler for one use case. */
data class PageUseCaseHandlerModel(
    val intentSimpleName: String,
    val handlerName: String,
    val useCaseCamel: String,
    val hasParams: Boolean,
    val paramPassArgs: String,
    val showLoading: Boolean,
    /** True when [UseCaseInfo.returnType] is a network/API [Result] (not Flow, not plain Unit/T). */
    val resultBased: Boolean,
    /** True when return type is Flow / StateFlow / etc. */
    val flowBased: Boolean,
    /**
     * True for generated Room `Update*UseCase` with `Unit` return and a single `entity: *Entity` param:
     * after success, the entity is copied into [unitEchoStatePropertyName] for Compose.
     */
    val unitEntityEchoToState: Boolean = false,
    /** State property name (e.g. `updatedUserSession`); meaningful when [unitEntityEchoToState]. */
    val unitEchoStatePropertyName: String = "",
    /** Parameter name to assign from (always `entity` when [unitEntityEchoToState]). */
    val unitEchoParamName: String = "",
    /**
     * True when the use case returns a plain value (e.g. prefs `Get*UseCase` → [String]) — not [Result], not Flow, not Unit echo.
     * Generated handler assigns `val ret = …()` then [copy] [directStatePropertyName] = ret.
     */
    val directReturnToState: Boolean = false,
    /** State property name; same as use-case camelName (e.g. `getUserId`). */
    val directStatePropertyName: String = "",
    /** True when return type is `Result<PageResult<T>>` (offset pagination). */
    val pagedBased: Boolean = false,
    /** True when return type is `Flow<PagingData<T>>`. */
    val pagedFlowBased: Boolean = false,
    val pageParam: String = "page",
    val pageSizeParam: String = "pageSize",
    val pagedItemTypeFqn: String = "",
    val pagedItemTypeContractRef: String = "",
    val flowPagedItemTypeFqn: String = "",
    val flowPagedItemTypeContractRef: String = "",
)

/** Freemarker root model for android page templates. */
data class PageTemplateModel(
    val pascalName: String,
    val camelName: String,
    val layoutSnakeName: String,
    val modulePackage: String,
    val screenPkg: String,
    val screenDirPkg: String,
    val modelPackage: String,
    val resultPackage: String,
    val uiContractPackage: String,
    val baseClasses: BaseClassPackagesModel,
    val basePackage: String?,
    val useCases: List<PageUseCaseModel>,
    val hasUseCases: Boolean,
    val hasBaseViewModel: Boolean,
    val baseViewModelIsAndroidX: Boolean,
    val baseViewModelImport: String?,
    val baseViewModelSimpleName: String?,
    val stateFields: List<PageStateFieldModel>,
    /** Imports for domain types referenced in [PageStateFieldModel.typeContractRef]. */
    val contractImports: List<String>,
    val intentInners: List<PageIntentInnerModel>,
    val useCaseHandlers: List<PageUseCaseHandlerModel>,
    /** When false, ViewModel template omits `import …Result` (plain/Flow-only handlers). */
    val hasResultBasedHandler: Boolean,
    val pagingOption: String = "auto",
    val hasPagedOffset: Boolean = false,
    val hasPagedFlow: Boolean = false,
    val pagedItemTypeContractRef: String = "",
    val pagedItemTypeFqn: String = "",
    val flowPagedItemTypeContractRef: String = "",
    val defaultPageSize: Int = 20,
    val primaryPagedArgList: String = "",
    val primaryPagedUseCaseCamel: String = "",
    /** FQN for import / FTL: `${uiContractPackage}.PagingListState` or `template.core.base.ui.PagingListState`. */
    val pagingListStateInterfaceFqn: String = "",
    /** FQN for `PageResult` used in generated ViewModel fetch mapping. */
    val pageResultClassFqn: String = "",
    /** Short item type for `PagingListState<…>` and `runPagedLoad<…>`. */
    val pagedStateItemContractRef: String = "",
    /** Simple inner page DTO for comments (e.g. `PageResultAppAiChatSessionRespVO`). */
    val pagedConcreteInnerContractRef: String = "",
    /** Comma-separated non-page args with placeholders (documentation / merge); may be empty. */
    val primaryPagedNonPageArgList: String = "",
)
