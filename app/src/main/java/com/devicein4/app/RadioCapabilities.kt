package com.devicein4.app

import android.telephony.TelephonyManager

internal data class RadioCapabilities(val lte: Boolean?, val nr: Boolean?, val fromModem: Boolean)

/** Observing LTE cannot rule out NR: the device may be on Wi-Fi, lack a SIM or use LTE coverage. */
internal fun radioCapabilities(modemMask: Long?, networkType: Int?, displayOverride: Int?): RadioCapabilities {
    if (modemMask != null && modemMask != 0L) return RadioCapabilities(
        modemMask and TelephonyManager.NETWORK_TYPE_BITMASK_LTE != 0L,
        modemMask and TelephonyManager.NETWORK_TYPE_BITMASK_NR != 0L,
        true
    )
    val nsa = displayOverride == 3 || displayOverride == 4
    val nr = networkType == TelephonyManager.NETWORK_TYPE_NR || nsa || displayOverride == 5
    val lte = networkType == TelephonyManager.NETWORK_TYPE_LTE || networkType == 19 || nsa
    return RadioCapabilities(if (lte) true else null, if (nr) true else null, false)
}
