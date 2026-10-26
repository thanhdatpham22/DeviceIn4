package com.devicein4.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorHistoryTest {
    @Test fun retainsOnlyTenSecondsOfEvents() {
        val old = listOf(SensorPoint(1L, listOf(1f)), SensorPoint(10_000_000_001L, listOf(2f)))
        val result = appendSensorPoint(old, SensorPoint(15_000_000_001L, listOf(3f)))
        assertEquals(listOf(2f, 3f), result.map { it.values.single() })
    }

    @Test fun capsHighRateHistoryWithoutChangingChannelValues() {
        var history = emptyList<SensorPoint>()
        repeat(200) { index -> history = appendSensorPoint(history, SensorPoint(index.toLong(), listOf(index.toFloat(), -1f, 2f, 3f))) }
        assertEquals(120, history.size)
        assertEquals(80L, history.first().timestamp)
        assertEquals(listOf(199f, -1f, 2f, 3f), history.last().values)
    }

    @Test fun scalarAndExtraChannelsAreNotInventedAxes() {
        assertEquals("C1", sensorChannelName(5, 0))
        assertEquals("X", sensorChannelName(1, 0))
        assertEquals("Z", sensorChannelName(4, 2))
        assertEquals("W", sensorChannelName(11, 3))
        assertEquals("C6", sensorChannelName(14, 5))
        assertTrue(sensorChannelName(3, 0) != "X")
    }
}
