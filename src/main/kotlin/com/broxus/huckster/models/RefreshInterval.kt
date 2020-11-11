package com.broxus.huckster.models

import com.google.gson.annotations.Expose

/**
 * Indicates how often the orderbook should be rebalanced
 *
 * @property hard Interval in seconds of the whole strategy relaunch
 * @property soft Interval in seconds of the shift relaunch
 */
data class RefreshInterval(
    @Expose val hard: String,
    @Expose val soft: String
)