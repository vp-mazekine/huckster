package com.broxus.huckster.models

import com.google.gson.annotations.Expose

/**
 * Defines the shifted execution rule
 *
 * @property offset Offset in seconds from the time of the first order placement
 * @property volumePart Share of the trading amount to be included in the shift
 */
data class PlacementOffset(
    @Expose val offset: String,
    @Expose val volumePart: String
)