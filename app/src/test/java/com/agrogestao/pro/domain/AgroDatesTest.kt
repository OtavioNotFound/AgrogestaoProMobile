package com.agrogestao.pro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgroDatesTest {
    @Test
    fun `date picker values are stored as ISO`() {
        assertEquals("2026-07-09", toIsoDate(2026, 6, 9))
    }

    @Test
    fun `ISO dates are displayed in Brazilian format`() {
        assertEquals("09/07/2026", formatDateForDisplay("2026-07-09"))
    }

    @Test
    fun `legacy labels remain readable`() {
        assertEquals("Hoje", formatDateForDisplay("Hoje"))
        assertEquals("Em 3 meses", formatDateForDisplay("Em 3 meses"))
    }

    @Test
    fun `invalid ISO date does not produce picker parts`() {
        assertNull(isoDateParts("2026-02-31"))
        assertNull(isoDateParts("2026-02-20abc"))
    }

    @Test
    fun `harvest cannot precede crop start`() {
        assertTrue(isIsoDateOnOrAfter("2026-10-01", "2026-07-01"))
        assertTrue(isIsoDateOnOrAfter("2026-07-01", "2026-07-01"))
        assertFalse(isIsoDateOnOrAfter("2026-06-30", "2026-07-01"))
    }
}
