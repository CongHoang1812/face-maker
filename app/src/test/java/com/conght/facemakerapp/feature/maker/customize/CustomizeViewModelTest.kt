package com.fantasy.oc.maker.customize.feature.maker.customize

import android.graphics.Bitmap
import app.cash.turbine.test
import com.fantasy.oc.maker.customize.core.common.NetworkMonitor
import com.fantasy.oc.maker.customize.core.common.Resource
import com.fantasy.oc.maker.customize.data.model.CharacterConfig
import com.fantasy.oc.maker.customize.data.model.CharacterTemplate
import com.fantasy.oc.maker.customize.data.model.CharacterTransform
import com.fantasy.oc.maker.customize.data.model.Creation
import com.fantasy.oc.maker.customize.data.model.CreationType
import com.fantasy.oc.maker.customize.data.model.PartCategory
import com.fantasy.oc.maker.customize.data.model.PartColor
import com.fantasy.oc.maker.customize.data.model.PartItem
import com.fantasy.oc.maker.customize.data.model.PartSelection
import com.fantasy.oc.maker.customize.data.model.TemplateDetail
import com.fantasy.oc.maker.customize.data.repository.CreationRepository
import com.fantasy.oc.maker.customize.data.repository.MakerRepository
import com.fantasy.oc.maker.customize.feature.maker.move.MoveLimits
import com.google.gson.Gson
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomizeViewModelTest {

    private val red = PartColor("red", "#FF0000", "url/red")
    private val blue = PartColor("blue", "#0000FF", "url/blue")
    private val hair = PartCategory(
        id = "hair", name = "Hair", iconUrl = "", zIndex = 1, isRequired = false,
        items = listOf(PartItem("h1", "t/h1", listOf(red, blue)), PartItem("h2", "t/h2", listOf(blue))),
    )
    private val body = PartCategory(
        id = "body", name = "Body", iconUrl = "", zIndex = 0, isRequired = true,
        items = listOf(PartItem("b1", "t/b1", emptyList()), PartItem("b2", "t/b2", emptyList())),
    )
    private val default = CharacterConfig(
        templateId = "t", selections = mapOf("hair" to PartSelection(null, null), "body" to PartSelection("b1", null)),
    )
    private val detail = TemplateDetail("t", listOf(hair, body), default)

    private var randomSelection: PartSelection? = null

    private val makerRepository = object : MakerRepository {
        override fun templates(): Flow<Resource<List<CharacterTemplate>>> = emptyFlow()
        override fun templateDetail(id: String) = flowOf<Resource<TemplateDetail>>(Resource.Success(detail))
        override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? = randomSelection
        override suspend fun randomizeCharacter(templateId: String): CharacterConfig? = null
    }

    private val saved = mutableListOf<CharacterConfig?>()
    private val creationRepository = object : CreationRepository {
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long): Creation? = null
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?): Long {
            saved += config
            return 1
        }
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<Unit> = Result.success(Unit)
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = "/cache/$fileName"
        override suspend fun importImage(uri: android.net.Uri) = "/cache/import"
    }

    private val gson = Gson()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(
        handle: SavedStateHandle = SavedStateHandle(mapOf("templateId" to "t")),
        network: NetworkMonitor = onlineMonitor,
    ) =
        CustomizeViewModel(makerRepository, creationRepository, gson, handle, network).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        }

    @Test
    fun loadsDefaultConfigAndFirstCategory() = runTest {
        val vm = viewModel()
        val state = vm.state.value
        assertTrue(state.isReady)
        assertEquals(default, state.config)
        assertEquals("hair", state.selectedCategoryId)
        assertFalse(state.isDirty)
    }

    @Test
    fun selectItem_picksFirstColor_thenKeepsColorIfAvailable() = runTest {
        val vm = viewModel()
        vm.selectItem("h1")
        assertEquals(PartSelection("h1", "red"), vm.state.value.selection)
        vm.selectColor("blue")
        vm.selectItem("h2") // h2 có blue -> giữ blue
        assertEquals(PartSelection("h2", "blue"), vm.state.value.selection)
        assertTrue(vm.state.value.isDirty)
    }

    @Test
    fun none_onOptionalCategory_clears_onRequiredCategory_resetsToDefault() = runTest {
        val vm = viewModel()
        vm.selectItem("h1")
        vm.selectItem(null)
        assertNull(vm.state.value.selection?.itemId)

        vm.selectCategory("body")
        vm.selectItem("b2")
        vm.selectItem(null)
        assertEquals(PartSelection("b1", null), vm.state.value.selection)
    }

    @Test
    fun transform_flipZoomClamp_andReset() = runTest {
        val vm = viewModel()
        vm.selectItem("h1")
        vm.flip()
        repeat(30) { vm.zoom(0.1f) }
        val config = vm.state.value.config!!
        assertTrue(config.transform.flipped)
        assertEquals(2f, config.partTransform("hair").scale, 0.0001f)

        vm.reset()
        assertEquals(default, vm.state.value.config)
        assertFalse(vm.state.value.isDirty)
    }

    @Test
    fun moveAndZoom_onlyAffectSelectedPart() = runTest {
        val vm = viewModel()
        vm.selectCategory("body")
        vm.move(10f, 20f)
        vm.zoom(0.5f)

        val config = vm.state.value.config!!
        assertEquals(CharacterTransform(offsetX = 10f, offsetY = 20f, scale = 1.5f), config.partTransform("body"))
        assertEquals(CharacterTransform(), config.partTransform("hair"))
        assertEquals(CharacterTransform(), config.transform)
    }

    @Test
    fun move_onNonePart_doesNothing() = runTest {
        val vm = viewModel() // hair đang là None
        vm.move(10f, 20f)
        assertEquals(default, vm.state.value.config)
        assertFalse(vm.state.value.isDirty)
    }

    @Test
    fun move_whenFlipped_invertsHorizontalDirection() = runTest {
        val vm = viewModel()
        vm.selectCategory("body")
        vm.flip()
        vm.move(10f, 20f)
        val transform = vm.state.value.config!!.partTransform("body")
        assertEquals(-10f, transform.offsetX, 0.0001f)
        assertEquals(20f, transform.offsetY, 0.0001f)
    }

    @Test
    fun canMovePart_falseOnNone_trueWhenItemSelected() = runTest {
        val vm = viewModel() // hair đang là None
        assertFalse(vm.state.value.canMovePart)
        vm.selectItem("h1")
        assertTrue(vm.state.value.canMovePart)
        vm.selectItem(null)
        assertFalse(vm.state.value.canMovePart)
    }

    @Test
    fun moveLimits_disableOnlyDirectionAtLimit() = runTest {
        val vm = viewModel()
        vm.selectCategory("body")
        assertEquals(MoveLimits(), vm.state.value.moveLimits)

        repeat(30) { vm.move(10f, 0f) }
        assertEquals(MoveLimits(canMoveRight = false), vm.state.value.moveLimits)

        vm.move(-10f, 0f)
        assertEquals(MoveLimits(), vm.state.value.moveLimits)
    }

    @Test
    fun moveLimits_whenFlipped_followScreenDirection() = runTest {
        val vm = viewModel()
        vm.selectCategory("body")
        vm.flip()
        repeat(30) { vm.move(10f, 0f) } // sang phải trên màn hình
        assertEquals(MoveLimits(canMoveRight = false), vm.state.value.moveLimits)
    }

    @Test
    fun moveLimits_disableZoomAtBothEnds() = runTest {
        val vm = viewModel()
        vm.selectCategory("body")
        repeat(30) { vm.zoom(0.1f) }
        assertEquals(MoveLimits(canZoomIn = false), vm.state.value.moveLimits)
        repeat(30) { vm.zoom(-0.1f) }
        assertEquals(MoveLimits(canZoomOut = false), vm.state.value.moveLimits)
    }

    @Test
    fun randomCategory_appliesSelection_andAsksToScrollToIt() = runTest {
        val vm = viewModel()
        val random = PartSelection("h2", "blue")
        randomSelection = random
        vm.events.test {
            vm.randomCategory()
            assertEquals(CustomizeEvent.ScrollToSelection(random), awaitItem())
        }
        assertEquals(random, vm.state.value.selection)
    }

    @Test
    fun configSavedBeforePartTransforms_stillReadable() {
        val old = gson.fromJson("""{"templateId":"t","selections":{}}""", CharacterConfig::class.java)
        assertEquals(CharacterTransform(), old.partTransform("hair"))
    }

    @Test
    fun selectionSurvivesRecreateWithSameSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("templateId" to "t"))
        val first = viewModel(handle)
        first.selectItem("h1")
        first.setPanel(PanelMode.MOVE)

        val second = viewModel(handle)
        assertEquals(PartSelection("h1", "red"), second.state.value.selection)
        assertEquals(PanelMode.MOVE, second.state.value.panel)
        assertTrue(second.state.value.isDirty)
    }
    @Test
    fun offline_partItemNoneTabAndDice_areBlockedWithNoInternet() = runTest {
        randomSelection = PartSelection("h2", "blue")
        val vm = viewModel(network = offlineMonitor)
        val before = vm.state.value
        vm.events.test {
            vm.runWhenOnline { selectItem("h1") }
            assertEquals(CustomizeEvent.NoInternet, awaitItem())
            vm.runWhenOnline { selectItem(null) }
            assertEquals(CustomizeEvent.NoInternet, awaitItem())
            vm.runWhenOnline { selectCategory("body") }
            assertEquals(CustomizeEvent.NoInternet, awaitItem())
            vm.runWhenOnline { randomCategory() }
            assertEquals(CustomizeEvent.NoInternet, awaitItem())
            vm.runWhenOnline { requestRandomAll() }
            assertEquals(CustomizeEvent.NoInternet, awaitItem())
        }
        assertEquals(before.config, vm.state.value.config)
        assertEquals(before.selectedCategoryId, vm.state.value.selectedCategoryId)
    }

    @Test
    fun online_randomAllButton_asksForConfirm() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.runWhenOnline { requestRandomAll() }
            assertEquals(CustomizeEvent.ConfirmRandomAll, awaitItem())
        }
    }
}

private val offlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(false) }


private val onlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(true) }
