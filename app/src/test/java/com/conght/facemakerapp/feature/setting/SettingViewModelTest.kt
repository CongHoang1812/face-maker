package com.conght.facemakerapp.feature.setting

import com.conght.facemakerapp.testutil.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {

    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app is not rated by default`() = runTest {
        assertFalse(SettingViewModel(settings).appRated.first())
    }

    @Test
    fun `rating is saved and still applies when setting is reopened`() = runTest {
        SettingViewModel(settings).markAppRated()

        assertTrue(settings.appRated.value)
        assertTrue(SettingViewModel(settings).appRated.first())
    }
}
