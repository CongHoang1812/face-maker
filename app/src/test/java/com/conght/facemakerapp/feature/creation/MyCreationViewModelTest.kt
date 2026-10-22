package com.conght.facemakerapp.feature.creation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.repository.CreationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.io.File

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
        override suspend fun download(ids: List<Long>): Result<List<File>> {
            downloaded += ids
            return Result.success(emptyList())
        }
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = ""
        override suspend fun importImage(uri: Uri) = ""
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repo: FakeRepo, handle: SavedStateHandle = SavedStateHandle(mapOf("tab" to "CHARACTER"))) =
        MyCreationViewModel(repo, handle).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
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
