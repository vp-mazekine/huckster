package com.broxus.nova.types

enum class ApplicationMode(url: String) {
    TEST("https://apidev.broxus.com"),
    PRODUCTION("https://api.broxus.com")
}