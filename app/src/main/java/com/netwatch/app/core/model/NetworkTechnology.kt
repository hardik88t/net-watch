package com.netwatch.app.core.model

enum class NetworkTechnology(val rank: Int) {
    NO_SERVICE(0),
    EMERGENCY_ONLY(1),
    NETWORK_2G(2),
    NETWORK_3G(3),
    LTE(4),
    NETWORK_5G(5),
    WIFI(6);

    fun isOutage(): Boolean = this == NO_SERVICE || this == EMERGENCY_ONLY
}
