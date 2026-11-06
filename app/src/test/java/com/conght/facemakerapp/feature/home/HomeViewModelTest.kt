package com.fantasy.oc.maker.customize.feature.home

import com.fantasy.oc.maker.customize.core.common.NetworkMonitor
import com.fantasy.oc.maker.customize.testutil.FakeSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val settings = FakeSettingsRepository()
    private val network = object : NetworkMonitor {
        override val isOnline = MutableStateFlow(true)
    }

    private fun viewModel() = HomeViewModel(network, settings)

    @Test
    fun `back exits first time and shows rate second time, counted across app launches`() = runTest {
        assertFalse(viewModel().shouldShowRateOnBack())
        assertTrue(viewModel().shouldShowRateOnBack())
        assertFalse(viewModel().shouldShowRateOnBack())
        assertTrue(viewModel().shouldShowRateOnBack())
    }

    @Test
    fun `back always exits once app is rated`() = runTest {
        viewModel().markAppRated()

        repeat(4) { assertFalse(viewModel().shouldShowRateOnBack()) }
        assertEquals(0, settings.homeBackCount)
    }
}
