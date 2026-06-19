/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.templateengine.model

/**
 * Swagger / OpenAPI generation uses FreeMarker with a root [Map] of variables
 * (lists of operations, schemas, import flags, etc.) built in `:feature:scaffold` by
 * [com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerTemplateModelFactory].
 *
 * Keeping the shape as a map allows the template set to evolve without changing this module.
 */
object SwaggerTemplateModel
