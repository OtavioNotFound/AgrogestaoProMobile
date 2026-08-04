package com.agrogestao.pro.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncPolicyTest {

    @Test
    fun newerOrEqualRemoteChangeWins() {
        assertTrue(shouldApplyRemoteChange(localTimestamp = 100, remoteTimestamp = 101))
        assertTrue(shouldApplyRemoteChange(localTimestamp = 100, remoteTimestamp = 100))
        assertFalse(shouldApplyRemoteChange(localTimestamp = 100, remoteTimestamp = 99))
        assertFalse(shouldApplyRemoteChange(localTimestamp = 0, remoteTimestamp = 0))
    }

    @Test
    fun localTimestampAlwaysMovesForward() {
        assertEquals(501L, nextLocalTimestamp(previous = 500, now = 400))
        assertEquals(600L, nextLocalTimestamp(previous = 500, now = 600))
    }

    @Test
    fun cloudTimestampRoundTripsAndAcceptsPostgresPrecision() {
        val timestamp = 1_785_684_923_456L
        assertEquals(timestamp, parseCloudTimestamp(formatCloudTimestamp(timestamp)))
        assertEquals(
            parseCloudTimestamp("2026-08-02T15:35:23.123Z"),
            parseCloudTimestamp("2026-08-02T12:35:23.123999-03:00")
        )
    }
}
