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
    /** Resolved Kotlin type for generated sources. */
    val kotlinType: String,
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
    val typeFqn: String,
    val nullable: Boolean,
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
    val intentInners: List<PageIntentInnerModel>,
    val useCaseHandlers: List<PageUseCaseHandlerModel>,
)
