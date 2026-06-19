/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.templateengine.di

import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import com.dqc.egsengine.feature.templateengine.prefs.PrefsTemplateRenderer
import org.koin.dsl.module

val featureTemplateEngineModule =
    module {
        single { TemplateRegistry() }
        single { TemplateEngine(registry = get()) }
        single { PrefsTemplateRenderer(engine = get()) }
    }
