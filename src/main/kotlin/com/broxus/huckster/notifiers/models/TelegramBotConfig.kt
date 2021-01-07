package com.broxus.huckster.notifiers.models

import com.google.gson.annotations.Expose

data class TelegramBotConfig (
    @Expose val adapter: String,
    @Expose val auth: TelegramBotAuth,
    @Expose val subscribers: List<Long>
)
