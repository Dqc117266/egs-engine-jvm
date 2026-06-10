/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

/**
 * Android Swagger codegen: packages under `feature.<module>.generate` (mirrors [KmpSwaggerGeneratorContext] layout).
 * Inherits Retrofit / [ModuleTemplate] result wrapper behavior from [SwaggerGeneratorContext].
 */
class AndroidSwaggerGeneratorContext(
    template: ModuleTemplate,
) : SwaggerGeneratorContext(template, generateLayout = true)
