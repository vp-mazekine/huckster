package com.broxus.huckster.notifiers

import com.broxus.huckster.interfaces.Notifier

object Notifier {
    private var adapter: Notifier? = null

    fun init(adapter: Notifier) {
        this.adapter = adapter
    }

    operator fun invoke(): Notifier? {
        return this.adapter
    }
}