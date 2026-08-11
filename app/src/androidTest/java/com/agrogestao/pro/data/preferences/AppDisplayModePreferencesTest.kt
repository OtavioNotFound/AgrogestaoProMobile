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
    fun simpleModeIsRecommendedByDefaultForANewOwner() {
        assertTrue(preferences.read("owner-a"))
        assertTrue(preferences.readLoginChoice())
        assertFalse(preferences.read(""))
    }

    @Test
    fun preferenceIsPersistedAndIsolatedByOwner() {
        assertTrue(preferences.save("owner-a", true))

        assertTrue(preferences.read("owner-a"))
        assertTrue(preferences.read("owner-b"))

        assertTrue(preferences.save("owner-a", false))
        assertFalse(preferences.read("owner-a"))
    }

    @Test
    fun loginChoiceOnlyAppliesAfterSuccessfulAuthentication() {
        assertTrue(preferences.saveLoginChoice(true))
        assertFalse(preferences.applyStagedLoginChoice("owner-a"))
        assertTrue(preferences.read("owner-a"))

        assertTrue(preferences.stageLoginChoice(true))
        assertTrue(preferences.applyStagedLoginChoice("owner-a"))
        assertTrue(preferences.read("owner-a"))
        assertFalse(preferences.applyStagedLoginChoice("owner-b"))
        assertTrue(preferences.read("owner-b"))

        assertTrue(preferences.stageLoginChoice(false))
        assertTrue(preferences.applyStagedLoginChoice("owner-a"))
        assertFalse(preferences.read("owner-a"))
    }

    @Test
    fun failedAuthenticationCanDiscardStagedChoice() {
        assertTrue(preferences.stageLoginChoice(true))
        assertTrue(preferences.clearStagedLoginChoice())
        assertFalse(preferences.applyStagedLoginChoice("owner-a"))
        assertTrue(preferences.read("owner-a"))
    }
}
