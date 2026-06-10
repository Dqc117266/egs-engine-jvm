/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template.di

import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import com.dqc.egsengine.template.prefs.PrefsTemplateRenderer
import org.koin.dsl.module

val featureTemplateEngineModule =
    module {
        single { TemplateRegistry() }
        single { TemplateEngine(registry = get()) }
        single { PrefsTemplateRenderer(engine = get()) }
    }
