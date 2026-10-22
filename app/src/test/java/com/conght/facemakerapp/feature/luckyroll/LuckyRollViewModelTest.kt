package com.conght.facemakerapp.feature.luckyroll

import androidx.lifecycle.SavedStateHandle
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.repository.LuckyRollRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LuckyRollViewModelTest {

    private val result = LuckyRollResult(
        imageUrl = "img",
        config = CharacterConfig("t", mapOf("hair" to PartSelection("h1", "red"))),
    )

    /** Chưa có API: Loading rồi treo mãi. */
    private val pendingRepo = object : LuckyRollRepository {
        override fun roll(): Flow<Resource<LuckyRollResult>> = flow {
            emit(Resource.Loading)
            awaitCancellation()
        }
    }

    private val successRepo = object : LuckyRollRepository {
        override fun roll(): Flow<Resource<LuckyRollResult>> = flowOf(Resource.Success(result))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repo: LuckyRollRepository, handle: SavedStateHandle = SavedStateHandle()) =
        LuckyRollViewModel(repo, Gson(), handle, onlineMonitor).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        }

    @Test
    fun idleUntilRandom_thenLoadingForeverWithoutApi() = runTest {
        val vm = viewModel(pendingRepo)
        assertEquals(LuckyRollUiState.Idle, vm.state.value)
        vm.roll()
        assertEquals(LuckyRollUiState.Rolled(Resource.Loading), vm.state.value)
    }

    @Test
    fun success_configJsonRoundTrips_andRollSurvivesRecreate() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(successRepo, handle)
        vm.roll()
        assertEquals(LuckyRollUiState.Rolled(Resource.Success(result)), vm.state.value)
        assertEquals(result.config, Gson().fromJson(vm.configJson(result), CharacterConfig::class.java))

        val recreated = viewModel(successRepo, handle)
        assertEquals(LuckyRollUiState.Rolled(Resource.Success(result)), recreated.state.value)
    }
}

private val onlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(true) }
