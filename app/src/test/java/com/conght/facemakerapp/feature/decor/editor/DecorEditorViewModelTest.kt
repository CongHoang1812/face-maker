package com.conght.facemakerapp.feature.decor.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.DecorRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DecorEditorViewModelTest {

    private val decorRepository = object : DecorRepository {
        override fun packs(type: DecorType): Flow<Resource<List<DecorPack>>> = flowOf(Resource.Loading)
    }

    private val creationRepository = object : CreationRepository {
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long): Creation? = null
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 1L
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<List<File>> = Result.success(emptyList())
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = "/cache/$fileName"
        override suspend fun importImage(uri: Uri) = "/cache/imports/1"
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        DecorEditorViewModel(decorRepository, creationRepository, Gson(), handle)

    @Test
    fun background_colorThenNone() {
        val vm = viewModel()
        vm.setBackgroundColor(0xFFFF0000.toInt())
        assertEquals(0xFFFF0000.toInt(), vm.data.value.background.color)
        assertTrue(vm.data.value.isDirty)
        vm.setBackgroundNone()
        assertTrue(vm.data.value.background.isNone)
    }

    @Test
    fun submitText_addsNewText_thenUpdatesSelected() {
        val vm = viewModel()
        vm.submitText("  Hi  ")
        val added = vm.data.value.selected!!
        assertEquals(ElementType.TEXT, added.type)
        assertEquals("Hi", added.text)

        vm.submitText("Yo")
        assertEquals(1, vm.data.value.elements.size)
        assertEquals("Yo", vm.data.value.selected!!.text)

        vm.submitText("   ") // rỗng: bỏ qua
        assertEquals("Yo", vm.data.value.selected!!.text)
    }

    @Test
    fun none_deletesSelectedOfSameType_otherwiseDeselects() {
        val vm = viewModel()
        vm.addSticker("s1")
        vm.noneFor(ElementType.SPEECH) // khác loại → chỉ bỏ chọn
        assertNull(vm.data.value.selectedId)
        assertEquals(1, vm.data.value.elements.size)

        vm.select(vm.data.value.elements.first().id)
        vm.noneFor(ElementType.STICKER)
        assertTrue(vm.data.value.elements.isEmpty())
    }

    @Test
    fun transform_clamped_flip_delete() {
        val vm = viewModel()
        vm.addSpeech("b1", "Hello")
        val id = vm.data.value.selectedId!!
        vm.transform(id, x = 2f, y = -1f, scale = 10f, rotation = 370f)
        val e = vm.data.value.selected!!
        assertEquals(1f, e.x)
        assertEquals(0f, e.y)
        assertEquals(4f, e.scale)
        assertEquals(10f, e.rotation)
        vm.flip(id)
        assertTrue(vm.data.value.selected!!.flipped)
        vm.delete(id)
        assertNull(vm.data.value.selectedId)
    }

    @Test
    fun fontAndColor_applyToSelectedTextAndBecomeDefault() {
        val vm = viewModel()
        vm.submitText("Hi")
        vm.setFont(DecorFonts.BANGERS.id)
        vm.setTextColor(0xFF00FF00.toInt())
        val e = vm.data.value.selected!!
        assertEquals(DecorFonts.BANGERS.id, e.fontId)
        assertEquals(0xFF00FF00.toInt(), e.color)
        assertEquals(DecorFonts.BANGERS.id, vm.data.value.fontId)
    }

    @Test
    fun tabsAndSelection_doNotMarkDirty_andStateSurvivesRecreate() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.selectMainTab(EditorTab.SPEECH)
        first.selectSubTab(2)
        assertFalse(first.data.value.isDirty)
        first.addSticker("s1")

        val second = viewModel(handle)
        assertEquals(EditorTab.SPEECH, second.data.value.mainTab)
        assertEquals(2, second.data.value.subTab)
        assertEquals(1, second.data.value.elements.size)
        assertTrue(second.data.value.isDirty)
    }
}
