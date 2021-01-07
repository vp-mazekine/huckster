package com.broxus.huckster.notifiers.adapters

import com.broxus.huckster.interfaces.Notifier
import com.broxus.huckster.notifiers.models.TelegramBotConfig
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.network.fold
import org.slf4j.LoggerFactory
import java.lang.Exception

class TelegramBotAdapter(private val config: TelegramBotConfig): Notifier {
    private var bot: Bot? = null
    private val logger by lazy { LoggerFactory.getLogger(TelegramBotAdapter::class.java) }

    override fun info(message: String, header: String?) {
        notify(message,
            when(header) {
                null -> "ℹ️ Notification"
                else -> "ℹ️ $header notification"
            }
        )
    }

    override fun error(message: String, header: String?) {
        notify(message,
            when(header) {
                null -> "\uD83D\uDCA2 Error"
                else -> "\uD83D\uDCA2 $header error"
            }
        )
    }

    override fun warning(message: String, header: String?) {
        notify(message,
            when(header) {
                null -> "⚠️ Warning"
                else -> "⚠️ $header warning"
            }
        )
    }

    /**
     * Dispatches message to the list of recipients through the Telegram bot
     *
     * @param message
     * @param header
     */
    private fun notify(message: String, header: String) {
        try {
            this@TelegramBotAdapter.config.let {
                this.bot = bot {
                    token = it.auth.botId + ":" + it.auth.apiKey
                }

                it.subscribers.forEach { chatId ->
                    bot!!.sendMessage(
                        chatId,
                        ("*$header*\n\n" +
                                message).escapeTelegramSpecialCharacters(),
                        parseMode = ParseMode.MARKDOWN_V2
                    ).fold(
                        {
                            /* no-op */
                        },
                        {re ->
                            this.logger.error("Error sending notification:\n${re.errorBody}", re.exception)
                        }
                    )
                }
            }
        } catch(e: Exception) {
            this.logger.error("Telegram bot adapter is not properly initialized!", e)
        }
    }

    /**
     * Fixes Telegram issue with sending special characters
     */
    fun String.escapeTelegramSpecialCharacters() = this.replace("[_\\-\\.]".toRegex()) {"\\${it.value}"}
}