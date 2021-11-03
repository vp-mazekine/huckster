package com.broxus.huckster.models

import com.broxus.huckster.interfaces.QueueEvent
import com.broxus.nova.models.SelfTradingPrevention
import com.broxus.nova.types.AddressType

data class PlaceOrderEvent(
    val userAddress: String,
    val addressType: String,
    val workspaceId: String?,
    var fromAmount: String,
    var toAmount: String,
    val fromCurrency: String,
    val toCurrency: String,
    val applicationId: String?,
    val selfTradingPrevention: SelfTradingPrevention?,
    val initialDelay: Long,
    val cancelDelay: Long
): QueueEvent