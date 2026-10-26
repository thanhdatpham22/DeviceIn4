package com.devicein4.app

import org.junit.Assert.*
import org.junit.Test

class RadioCapabilitiesTest {
    @Test fun lteConnectionDoesNotRuleOutFiveGHardware() {
        val result = radioCapabilities(null, 13, null)
        assertEquals(true, result.lte)
        assertNull(result.nr)
    }

    @Test fun nsaDisplayConfirmsLteAndNrCapabilities() {
        val result = radioCapabilities(null, 13, 3)
        assertEquals(true, result.lte)
        assertEquals(true, result.nr)
        assertFalse(result.fromModem)
    }

    @Test fun noSimOrPermissionDoesNotMeanUnsupported() {
        val result = radioCapabilities(null, null, null)
        assertNull(result.lte)
        assertNull(result.nr)
        assertNull(radioCapabilities(0L, 0, null).nr)
    }

    @Test fun modemInventoryCanConfirmAbsenceOfNr() {
        val result = radioCapabilities(1L shl 12, null, null)
        assertEquals(true, result.lte)
        assertEquals(false, result.nr)
        assertTrue(result.fromModem)
        assertEquals(true, radioCapabilities((1L shl 12) or (1L shl 19), null, null).nr)
    }
}
