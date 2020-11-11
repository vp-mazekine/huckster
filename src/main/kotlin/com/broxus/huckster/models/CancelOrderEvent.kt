package com.broxus.huckster.models

import com.broxus.huckster.interfaces.QueueEvent

data class CancelOrderEvent(
    val transactionId: String
): QueueEvent