package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.scaffold.data.PageGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

/**
 * Full-tree golden snapshot of `create page` output for ANDROID and KMP projects.
 * Catches any unintended change in the wired `android/page` and `kmp/page` FTL templates.
 */
class GoldenPageTest {

    @Test
    fun `android page golden`() {
        val files = PageGenerator().preview(tempRoot(), pageTemplate("com.dqc.example"), "ANDROID")
        GoldenSnapshot.verify("page-android", files.map { it.path to it.content })
    }

    @Test
    fun `kmp page golden`() {
        val files = PageGenerator().preview(tempRoot(), pageTemplate("org.mifos"), "KMP")
        GoldenSnapshot.verify("page-kmp", files.map { it.path to it.content })
    }

    private fun tempRoot() = createTempDirectory("golden-page").toFile()

    private fun pageTemplate(basePackage: String): PageTemplate {
        val modulePackage = "$basePackage.feature.task"
        return PageTemplate(
            pageName = "TaskList",
            moduleName = "task",
            modulePackage = modulePackage,
            useCases = listOf(
                UseCaseInfo(
                    name = "TopicUpdateTopicUseCase",
                    packageName = "$modulePackage.domain.usecase",
                    path = "$modulePackage.domain.usecase.TopicUpdateTopicUseCase",
                    returnType = "Result<Boolean>",
                    parameters = listOf(UseCaseParam(name = "topicId", type = "Long")),
                ),
            ),
            basePackage = basePackage,
            baseClassPackages = BaseClassPackages(
                baseViewModel = "$basePackage.feature.base.presentation.viewmodel.BaseViewModel",
                resultClass = "$basePackage.feature.base.domain.result.Result",
            ),
        )
    }
}
