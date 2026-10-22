package com.conght.facemakerapp.feature.success

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.conght.facemakerapp.core.common.RatePrompter
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.testutil.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SuccessViewModelTest {

    private class FakeRepo(private val fail: Boolean = false) : CreationRepository {
        val downloaded = mutableListOf<List<Long>>()
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long) = Creation(id, CreationType.DESIGN, "/files/creations/$id.png", null, 0)
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 1L
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<List<File>> {
            downloaded += ids
            return if (fail) Result.failure(IllegalStateException()) else Result.success(listOf(File("/files/downloads/a.png")))
        }
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = ""
        override suspend fun importImage(uri: Uri) = ""
    }

    private fun prompter() = RatePrompter(FakeSettingsRepository()) { 0L }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun handle(source: String = "MAKER", stars: Int = 0) = SavedStateHandle(
        mapOf(SuccessFragment.ARG_CREATION_ID to 7L, SuccessFragment.ARG_SOURCE to source, SuccessFragment.ARG_STARS to stars),
    )

    @Test
    fun readsArgsAndLoadsCreation() {
        val vm = SuccessViewModel(FakeRepo(), prompter(), handle(source = "COSPLAY", stars = 9))
        assertEquals(SuccessFragment.Source.COSPLAY, vm.source)
        assertEquals(3, vm.stars)
        assertEquals("/files/creations/7.png", vm.creation.value?.imagePath)
    }

    @Test
    fun download_success_and_failure_emitEvents() = runTest {
        val ok = FakeRepo()
        val vm = SuccessViewModel(ok, prompter(), handle())
        vm.events.test {
            vm.download()
            assertEquals(SuccessEvent.Downloaded, awaitItem())
        }
        assertEquals(listOf(listOf(7L)), ok.downloaded)

        val failing = SuccessViewModel(FakeRepo(fail = true), prompter(), handle())
        failing.events.test {
            failing.download()
            assertEquals(SuccessEvent.DownloadFailed, awaitItem())
        }
    }

    @Test
    fun missingId_doesNotDownload() {
        val repo = FakeRepo()
        SuccessViewModel(repo, prompter(), SavedStateHandle()).download()
        assertEquals(emptyList<List<Long>>(), repo.downloaded)
    }
}
