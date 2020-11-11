package com.broxus.huckster.models

import com.broxus.huckster.types.FeeOrderSide
import com.broxus.huckster.types.FeeUnitsType

/**
 * Definition of the orderbook fee
 *
 * @property currency
 * @property size
 * @property units
 */
data class OrderbookFee(
    val currency: FeeOrderSide,
    val size: String,
    val units: FeeUnitsType
)