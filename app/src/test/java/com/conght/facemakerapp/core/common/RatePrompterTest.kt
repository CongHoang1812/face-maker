package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.testutil.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatePrompterTest {

    private var day = 100L

    @Test
    fun afterSave_onlyFromSecondSave_once() = runTest {
        val settings = FakeSettingsRepository(saveCount = 1)
        val prompter = RatePrompter(settings) { day }
        assertFalse(prompter.shouldPromptAfterSave())

        settings.incrementCreationSaveCount()
        assertTrue(prompter.shouldPromptAfterSave())
        day++
        assertFalse(prompter.shouldPromptAfterSave()) // chỉ 1 lần
    }

    @Test
    fun homeExit_once_andAtMostOncePerDay() = runTest {
        val settings = FakeSettingsRepository(saveCount = 2)
        val prompter = RatePrompter(settings) { day }
        assertTrue(prompter.shouldPromptOnHomeExit())
        assertFalse(prompter.shouldPromptOnHomeExit())
        // cùng ngày đã nhắc → không nhắc sau khi lưu nữa
        assertFalse(prompter.shouldPromptAfterSave())
        day++
        assertTrue(prompter.shouldPromptAfterSave())
    }

    @Test
    fun neverPrompt_whenAlreadyRated() = runTest {
        val prompter = RatePrompter(FakeSettingsRepository(rated = true, saveCount = 5)) { day }
        assertFalse(prompter.shouldPromptOnHomeExit())
        assertFalse(prompter.shouldPromptAfterSave())
    }
}
