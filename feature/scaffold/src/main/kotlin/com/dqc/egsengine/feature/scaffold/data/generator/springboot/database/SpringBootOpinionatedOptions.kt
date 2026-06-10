/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot.database

/**
 * Defaults for Spring Boot table¡úCRUD codegen (`backend gen database`).
 * Matches the `egs-server-template/feature/demo` stack unless flags disable pieces.
 */
data class SpringBootOpinionatedOptions(
    val auditColumns: Boolean = true,
    val softDelete: Boolean = true,
    val statusEnum: Boolean = true,
)
