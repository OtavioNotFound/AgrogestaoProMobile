package com.agrogestao.pro.data.reminders

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.domain.TaskReminderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderPreferencesTest {
    private lateinit var preferences: TaskReminderPreferences

    @Before
    fun setUp() {
        preferences = TaskReminderPreferences(ApplicationProvider.getApplicationContext())
        preferences.clearForTests()
    }

    @After
    fun tearDown() {
        preferences.clearForTests()
    }

    @Test
    fun settingsAndDeliveryDedupeAreIsolatedByAccount() = runBlocking {
        val first = TaskReminderSettings(enabled = true, daysBefore = 1, hourOfDay = 7)
        val second = TaskReminderSettings(enabled = true, daysBefore = 3, hourOfDay = 18)
        assertTrue(preferences.save("owner-1", first))
        assertTrue(preferences.save("owner-2", second))

        assertEquals(first, preferences.observe("owner-1").first())
        assertEquals(second, preferences.read("owner-2"))
        assertTrue(preferences.markDeliveredIfNew("owner-1", "task-1", "signature-1"))
        assertFalse(preferences.markDeliveredIfNew("owner-1", "task-1", "signature-1"))
        assertTrue(preferences.markDeliveredIfNew("owner-2", "task-1", "signature-1"))
        assertTrue(preferences.markDeliveredIfNew("owner-1", "task-1", "signature-2"))

        val repeatedSave = async { preferences.observe("owner-1").take(2).toList() }
        delay(100)
        assertTrue(preferences.save("owner-1", first))
        assertEquals(listOf(first, first), repeatedSave.await())
    }
}
