package com.agrogestao.pro.data.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDisplayModePreferencesTest {
    private lateinit var preferences: AppDisplayModePreferences

    @Before
    fun setUp() {
        preferences = AppDisplayModePreferences(ApplicationProvider.getApplicationContext())
        preferences.clearForTests()
    }

    @After
    fun tearDown() {
        preferences.clearForTests()
    }

    @Test
    fun simpleModeIsDisabledByDefault() {
        assertFalse(preferences.read("owner-a"))
        assertFalse(preferences.read(""))
    }

    @Test
    fun preferenceIsPersistedAndIsolatedByOwner() {
        assertTrue(preferences.save("owner-a", true))

        assertTrue(preferences.read("owner-a"))
        assertFalse(preferences.read("owner-b"))

        assertTrue(preferences.save("owner-a", false))
        assertFalse(preferences.read("owner-a"))
    }
}
