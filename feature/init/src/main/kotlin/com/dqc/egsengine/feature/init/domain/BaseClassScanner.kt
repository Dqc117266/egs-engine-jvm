package com.dqc.egsengine.feature.init.domain

import com.dqc.egsengine.feature.init.domain.model.BaseClassInfo
import java.io.File

interface BaseClassScanner {
    fun scan(projectRoot: File, modules: List<String>): List<BaseClassInfo>
}
