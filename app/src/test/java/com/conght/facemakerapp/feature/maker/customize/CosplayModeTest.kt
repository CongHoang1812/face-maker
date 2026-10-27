package com.conght.facemakerapp.feature.maker.customize

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.model.PartCategory
import com.conght.facemakerapp.data.model.PartItem
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.MakerRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CosplayModeTest {

    private val dispatcher = StandardTestDispatcher()
    private val gson = Gson()

    private fun category(id: String) = PartCategory(
        id, id, "", 0, false, listOf(PartItem("${id}_1", "", emptyList()), PartItem("${id}_2", "", emptyList())),
    )

    private val detail = TemplateDetail(
        templateId = "t",
        categories = listOf(category("hair"), category("hat")),
        defaultConfig = CharacterConfig("t", mapOf("hair" to PartSelection(null, null), "hat" to PartSelection(null, null))),
    )

    /** Đáp án: hair_1 + hat = None → cấu hình mặc định (toàn None) được 0% (None vs None không tính). */
    private val challenge = CosplayChallenge(
        id = "c", referenceImageUrl = "ref", templateId = "t",
        answer = CharacterConfig("t", mapOf("hair" to PartSelection("hair_1", null), "hat" to PartSelection(null, null))),
    )

    private val makerRepository = object : MakerRepository {
        override fun templates(): Flow<Resource<List<CharacterTemplate>>> = emptyFlow()
        override fun templateDetail(id: String) = flowOf<Resource<TemplateDetail>>(Resource.Success(detail))
        override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? = null
        override suspend fun randomizeCharacter(templateId: String): CharacterConfig? = null
    }

    private val creationRepository = object : CreationRepository {
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long): Creation? = null
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 1L
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<List<File>> = Result.success(emptyList())
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = "/cache/$fileName"
        override suspend fun importImage(uri: Uri) = ""
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun handle() = SavedStateHandle(
        mapOf(
            CustomizeFragment.ARG_MODE to CustomizeMode.COSPLAY.name,
            CustomizeFragment.ARG_CHALLENGE_JSON to gson.toJson(challenge),
        ),
    )

    private fun TestScope.viewModel(handle: SavedStateHandle) =
        CustomizeViewModel(makerRepository, creationRepository, gson, handle, onlineMonitor).also { vm ->
            backgroundScope.launch { vm.state.collect {} }
            runCurrent()
        }

    @Test
    fun done_scoresAgainstAnswer() = runTest(dispatcher) {
        val vm = viewModel(handle())
        assertEquals("ref", vm.state.value.cosplay?.referenceImageUrl)
        assertNull(vm.state.value.cosplay?.scorePercent)

        vm.selectItem("hair_1") // giờ khớp cả 2 category
        runCurrent()
        vm.finishCosplay()
        runCurrent()
        assertEquals(100, vm.state.value.cosplay?.scorePercent)
    }

    @Test
    fun timer_countsDown_andAutoSubmitsAtZero() = runTest(dispatcher) {
        val h = handle()
        val vm = viewModel(h)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(10 * 60 - 5, vm.state.value.cosplay?.remainingSeconds)
        assertEquals(10 * 60 - 5, h.get<Int>("cosplay_remaining")) // giữ được khi recreate

        advanceTimeBy(10 * 60 * 1_000L)
        runCurrent()
        assertEquals(0, vm.state.value.cosplay?.remainingSeconds)
        assertEquals(0, vm.state.value.cosplay?.scorePercent)
    }
}

private val onlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(true) }
