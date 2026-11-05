package com.conght.facemakerapp.feature.creation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.model.StickerExport
import com.conght.facemakerapp.data.model.StickerTarget
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.StickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyCreationViewModelTest {

    /** "Room" giả: StateFlow, xoá thì phát lại danh sách mới. */
    private class FakeRepo : CreationRepository {
        val db = MutableStateFlow(
            listOf(
                Creation(1, CreationType.CHARACTER, "/c/1.png", "{}", 1),
                Creation(2, CreationType.CHARACTER, "/c/2.png", "{}", 2),
                Creation(3, CreationType.DESIGN, "/c/3.png", null, 3),
            ),
        )
        val downloaded = mutableListOf<List<Long>>()

        override fun observe(type: CreationType): Flow<List<Creation>> = db.map { list -> list.filter { it.type == type } }
        override suspend fun get(id: Long) = db.value.firstOrNull { it.id == id }
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 0L
        override suspend fun delete(ids: List<Long>) {
            db.value = db.value.filterNot { it.id in ids }
        }
        override suspend fun download(ids: List<Long>): Result<Unit> {
            downloaded += ids
            return Result.success(Unit)
        }
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = ""
        override suspend fun importImage(uri: Uri) = ""
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeStickerRepo : StickerRepository {
        val calls = mutableListOf<Pair<StickerTarget, List<String>>>()
        var result: Result<StickerExport> = Result.success(StickerExport.Telegram(listOf("/s/1.webp"), listOf("😀")))

        override suspend fun export(target: StickerTarget, imagePaths: List<String>): Result<StickerExport> {
            calls += target to imagePaths
            return result
        }
    }

    private val stickers = FakeStickerRepo()

    private fun TestScope.viewModel(
        repo: FakeRepo,
        handle: SavedStateHandle = SavedStateHandle(mapOf("tab" to "CHARACTER")),
    ) =
        MyCreationViewModel(repo, stickers, handle).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        }

    @Test
    fun defaultTabIsCharacter() = runTest {
        val vm = viewModel(FakeRepo(), SavedStateHandle())
        assertEquals(CreationType.CHARACTER, vm.state.value.tab)
    }

    @Test
    fun whatsAppStickers_needAtLeastThree() = runTest {
        val vm = viewModel(FakeRepo())
        vm.startSelection(1)
        vm.exportStickers(StickerTarget.WHATSAPP)
        assertEquals(MyCreationEvent.InvalidWhatsAppStickerCount, vm.events.first())
        assertTrue(stickers.calls.isEmpty())
        assertFalse(vm.busy.value)
    }

    @Test
    fun telegramStickers_exportSelectedImages() = runTest {
        val vm = viewModel(FakeRepo())
        vm.startSelection(2)
        vm.exportStickers(StickerTarget.TELEGRAM)
        assertEquals(MyCreationEvent.StickersReady(stickers.result.getOrThrow()), vm.events.first())
        assertEquals(listOf(StickerTarget.TELEGRAM to listOf("/c/2.png")), stickers.calls)
        assertFalse(vm.busy.value)
    }

    @Test
    fun stickerExportFailure_sendsFailedEvent() = runTest {
        stickers.result = Result.failure(IllegalStateException("boom"))
        val vm = viewModel(FakeRepo())
        vm.startSelection(1)
        vm.exportStickers(StickerTarget.TELEGRAM)
        assertEquals(MyCreationEvent.StickerExportFailed, vm.events.first())
    }

    @Test
    fun tappingCurrentTab_exitsSelection() = runTest {
        val vm = viewModel(FakeRepo())
        vm.startSelection(1)
        vm.selectTab(CreationType.CHARACTER)
        assertFalse(vm.state.value.selectionMode)
        assertTrue(vm.state.value.selectedIds.isEmpty())
        assertEquals(CreationType.CHARACTER, vm.state.value.tab)
    }

    @Test
    fun tabFiltersItems_andSwitchingClearsSelection() = runTest {
        val vm = viewModel(FakeRepo())
        assertEquals(listOf(1L, 2L), vm.state.value.items!!.map { it.id })
        vm.startSelection(1)
        vm.selectTab(CreationType.DESIGN)
        assertEquals(listOf(3L), vm.state.value.items!!.map { it.id })
        assertFalse(vm.state.value.selectionMode)
        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test
    fun multiSelect_toggle_delete_download() = runTest {
        val repo = FakeRepo()
        val vm = viewModel(repo)
        vm.startSelection(1)
        vm.toggle(2)
        assertEquals(setOf(1L, 2L), vm.state.value.selectedIds)
        vm.toggle(1)
        assertEquals(setOf(2L), vm.state.value.selectedIds)

        vm.download(vm.state.value.selectedIds)
        assertEquals(listOf(listOf(2L)), repo.downloaded)

        vm.requestDelete(vm.state.value.selectedIds)
        vm.onDeleteConfirmed(true)
        assertEquals(listOf(1L), vm.state.value.items!!.map { it.id })
        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test
    fun deleteCancelled_keepsItems_andStateSurvivesRecreate() = runTest {
        val repo = FakeRepo()
        val handle = SavedStateHandle(mapOf("tab" to "CHARACTER"))
        val first = viewModel(repo, handle)
        first.startSelection(2)
        first.requestDelete(setOf(2L))

        val second = viewModel(repo, handle) // xoay màn hình khi dialog đang mở
        assertTrue(second.state.value.selectionMode)
        assertEquals(setOf(2L), second.state.value.selectedIds)
        second.onDeleteConfirmed(false)
        assertEquals(2, second.state.value.items!!.size)
    }
}
