package com.fantasy.oc.maker.customize.feature.language

import androidx.lifecycle.SavedStateHandle
import com.fantasy.oc.maker.customize.core.common.AppLanguage
import com.fantasy.oc.maker.customize.testutil.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {

    private val settings = FakeSettingsRepository()

    private fun firstOpenViewModel() = LanguageViewModel(
        settings,
        SavedStateHandle(mapOf(LanguageFragment.ARG_FROM_SETTING to false)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first open has no selection`() {
        assertNull(firstOpenViewModel().selected.value)
    }

    @Test
    fun `selecting updates selection`() {
        val viewModel = firstOpenViewModel()
        viewModel.select(AppLanguage.FRENCH)
        assertEquals(AppLanguage.FRENCH, viewModel.selected.value)
    }

    @Test
    fun `confirm without selection saves nothing`() {
        firstOpenViewModel().confirm()
        assertNull(settings.languageCode.value)
    }

    @Test
    fun `confirm saves selected language`() {
        val viewModel = firstOpenViewModel()
        viewModel.select(AppLanguage.FRENCH)
        viewModel.confirm()
        assertEquals(AppLanguage.FRENCH.code, settings.languageCode.value)
    }
}
