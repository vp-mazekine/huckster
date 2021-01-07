package com.broxus.huckster.interfaces

interface Notifier {
    fun info(message: String, header: String?)
    fun error(message: String, header: String?)
    fun warning(message: String, header: String?)
}