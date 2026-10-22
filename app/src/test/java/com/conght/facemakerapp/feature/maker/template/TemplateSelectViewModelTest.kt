package com.conght.facemakerapp.feature.maker.template

import app.cash.turbine.test
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.repository.MakerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateSelectViewModelTest {

    private class FakeMakerRepository : MakerRepository {
        var fail = true
        var calls = 0
        val templates = listOf(CharacterTemplate("t1", "Template 1", "mock://color/#FFFFFF"))

        override fun templates(): Flow<Resource<List<CharacterTemplate>>> = resourceFlow {
            calls++
            if (fail) error("network") else templates
        }

        override fun templateDetail(id: String): Flow<Resource<TemplateDetail>> = emptyFlow()
        override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? = null
        override suspend fun randomizeCharacter(templateId: String): CharacterConfig? = null
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun errorThenRetry_emitsSuccess() = runTest {
        val repo = FakeMakerRepository()
        val viewModel = TemplateSelectViewModel(repo, onlineMonitor)

        viewModel.state.test {
            assertTrue(awaitItem() is Resource.Error)
            repo.fail = false
            viewModel.retry()
            // StateFlow có thể gộp Loading trung gian -> bỏ qua nếu có
            val next = awaitItem().let { if (it == Resource.Loading) awaitItem() else it }
            assertEquals(Resource.Success(repo.templates), next)
            assertEquals(2, repo.calls)
        }
    }
}

private val onlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(true) }
