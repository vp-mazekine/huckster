package com.broxus.huckster.notifiers.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TelegramBotAuth (
    @Expose @SerializedName("bot_id") val botId: String,
    @Expose @SerializedName("api_key") val apiKey: String
)
