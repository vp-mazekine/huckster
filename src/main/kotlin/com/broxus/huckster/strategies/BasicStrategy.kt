package com.broxus.huckster.strategies

import com.broxus.huckster.OrdersQueue
import com.broxus.huckster.interfaces.IPriceFeed
import com.broxus.huckster.interfaces.Strategy
import com.broxus.huckster.models.PlaceOrderEvent
import com.broxus.huckster.models.StrategyInput
import com.broxus.huckster.logger2
import com.broxus.huckster.notifiers.Notifier
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.models.SelfTradingPrevention
import com.broxus.utils.green
import com.broxus.utils.red
import com.broxus.utils.yellow
import kotlinx.coroutines.*
//import org.apache.logging.log4j.kotlin.logger
import org.apache.logging.log4j.LogManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.MessageFormat
import java.util.*

class BasicStrategy(
    val strategy: StrategyInput,
    val priceAdapter: IPriceFeed
): Strategy {

    val logger by lazy { LogManager.getLogger(this::class.java) }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun run() {
        var offsetPart: Float?
        var currencyPart: Float?
        var structuralPart: Float?
        var basePrice: Float?

        var restartNotification = true
        var initialBalance: Float? = null

        while(true) {
            val thread = GlobalScope.launch {
                //  Flush all current orders
                strategy.strategies.forEach {
                    OrdersQueue.flush(
                        strategy.account.userAddress,
                        strategy.account.addressType,
                        strategy.account.workspaceId,
                        strategy.configuration.sourceCurrency,
                        it.targetCurrency
                    )
                }

                //  Get balance for specific currency from the list of all available ones
                var availableBalance: Float? = null
                var totalBalance: Float? = null
                val balances = NovaApiService.getSpecificUserBalance(
                    strategy.account.userAddress,
                    strategy.account.addressType,
                    strategy.account.workspaceId
                )
                val numberFormat = DecimalFormat("#,##0.00####").apply {
                    decimalFormatSymbols = DecimalFormatSymbols().apply {
                        decimalSeparator = '.'
                        groupingSeparator = ' '
                    }
                }

                balances?.forEach {
                    if (it.currency == strategy.configuration.sourceCurrency) {
                        availableBalance = it.available.toFloat()
                        totalBalance = it.total.toFloat()
                        return@forEach
                    }
                }

                //  Check that orders has not stuck after the resetting
                strategy.configuration.faultTolerance?.let {ft ->
                    if(
                        totalBalance != null &&
                        totalBalance!! > 0.0F &&
                        availableBalance != null
                    ) {
                        if(availableBalance!! <= (totalBalance!! * ft)) {
                            val errorMessage =
                                MessageFormat(
                                    ResourceBundle.getBundle("messages", Locale.ENGLISH).getString("strategy.basic.inconsistent_balance"),
                                    Locale.ENGLISH
                                ).format(
                                    arrayOf(
                                        strategy.configuration.sourceCurrency,
                                        numberFormat.format(availableBalance ?: 0.0F),
                                        numberFormat.format(totalBalance!! - availableBalance!!),
                                        numberFormat.format(totalBalance ?: 0.0F)
                                    )
                                )

                            logger.error(errorMessage.red())
                            logger2(errorMessage.red())
                            Notifier()?.error(errorMessage, "Huckster")
                        }
                    }
                }

                //  Check that the balance is sufficient to run the selected strategy
                if (
                    (availableBalance == null)
                    || (strategy.configuration.hardFloor != null &&
                            availableBalance!! * strategy.configuration.volumeLimit.toFloat()
                            < strategy.configuration.hardFloor.toFloat())
                    || (availableBalance!! * strategy.configuration.volumeLimit.toFloat()
                            < strategy.configuration.minOrderSize.toFloat())
                ) {
                    val errorMessage =
                        MessageFormat(
                            ResourceBundle.getBundle("messages", Locale.ENGLISH).getString("strategy.basic.insufficient_balance"),
                            Locale.ENGLISH
                        ).format(
                            arrayOf(
                                numberFormat.format(availableBalance ?: 0.0F),
                                strategy.configuration.sourceCurrency,
                                strategy.account.userAddress,
                                strategy.account.addressType,
                                strategy.account.workspaceId,
                                NovaApiService.getStaticAddressByUser(
                                    strategy.configuration.sourceCurrency,
                                    strategy.account.userAddress,
                                    strategy.account.addressType,
                                    strategy.account.workspaceId
                                )?.blockchainAddress
                            )
                        )
                    logger.error(errorMessage.red())
                    logger2(errorMessage.red())
                    Notifier()?.error(errorMessage, "Huckster")
                    return@launch
                }

                availableBalance = availableBalance?.times(strategy.configuration.volumeLimit.toFloat()) ?: 0.0F

                //  Check if available balance doesn't meet the warning threshold
                if(initialBalance == null) initialBalance = availableBalance ?: 0.0F
                try {
                    when {
                        initialBalance!! < availableBalance!! -> {
                            //  In case the balance was refilled
                            initialBalance = availableBalance

                            val message = MessageFormat(
                                ResourceBundle
                                    .getBundle("messages", Locale.ENGLISH)
                                    .getString("strategy.basic.balance_refilled"),
                                Locale.ENGLISH
                            ).format(
                                arrayOf(
                                    strategy.configuration.sourceCurrency,
                                    numberFormat.format(availableBalance ?: 0.0F).trim(),
                                    (balances?.
                                        filter{ it.total.toFloat() > 0.0F }?.
                                        joinToString("\n"){
                                            it.currency + ": " +
                                            numberFormat.format(it.available.toFloat()).trim() +
                                            " (" + numberFormat.format(it.total.toFloat()).trim() + ")"
                                        } ?: "No other balances")
                                )
                            )

                            logger2(message.green())
                            Notifier()?.info(message, "Huckster")
                        }

                        (!strategy.configuration.notification?.soft.isNullOrEmpty() &&
                            availableBalance!! <= initialBalance!!
                            * strategy.configuration.notification!!.soft!!.toFloat()) ||
                        (!strategy.configuration.notification?.hard.isNullOrEmpty() &&
                            availableBalance!! <= strategy.configuration.notification!!.hard!!.toFloat()) -> {

                            //  In case available balance is lower than established threshold
                            val message = MessageFormat(
                                ResourceBundle
                                    .getBundle("messages", Locale.ENGLISH)
                                    .getString("strategy.basic.low_balance"),
                                Locale.ENGLISH
                            ).format(
                                arrayOf(
                                    numberFormat.format(availableBalance ?: 0.0F),
                                    strategy.configuration.sourceCurrency,
                                    strategy.account.userAddress,
                                    strategy.account.addressType,
                                    strategy.account.workspaceId,
                                    NovaApiService.getStaticAddressByUser(
                                        strategy.configuration.sourceCurrency,
                                        strategy.account.userAddress,
                                        strategy.account.addressType,
                                        strategy.account.workspaceId
                                    )?.blockchainAddress,
                                    strategy.configuration.notification.soft?.toFloat() ?: 0.0F,
                                    strategy.configuration.notification.hard?.toFloat() ?: 0.0F
                                )
                            )

                            logger.warn(message)
                            logger2(message.yellow())
                            Notifier()?.warning(message, "Huckster")
                        }
                    }
                } catch(e: Exception) {
                    logger2(
                        (
                            "Unable to check warning limits\n" +
                            e.message + "\n" +
                            e.stackTrace.joinToString("\n")
                        ).red()
                    )
                }

                logger2("Available balance: ".green() + "${availableBalance!!} ${strategy.configuration.sourceCurrency}")
                logger2("Pausing for 3 seconds before launching MM...".green())
                delay(3000)

                if(restartNotification) {
                    Notifier()?.info(
                        MessageFormat(
                            ResourceBundle.getBundle("messages", Locale.ENGLISH).getString("strategy.basic.mm_restarted"),
                            Locale.ENGLISH
                        ).format(
                            arrayOf(
                                numberFormat.format(availableBalance ?: 0.0F),
                                strategy.configuration.sourceCurrency
                            )
                        ),
                        "Huckster"
                    )
                    restartNotification = false
                }

                //  TODO: Perform checks on strategy data validity

                //  Calculate and place orders
                strategy.configuration.placementOffset.forEachIndexed offset@{ offsetIndex, offset ->
                    offsetPart = offset.volumePart.toFloat()

                    strategy.strategies.forEach strategy@{ s ->
                        currencyPart = s.volumePart.toFloat()

                        s.sizeStructure.forEachIndexed size@{ index, size ->
                            structuralPart = size.toFloat()

                            val fromAmount =
                                (availableBalance ?: 0.0F) *
                                        (offsetPart ?: 1.0F) *
                                        (currencyPart ?: 1.0F) *
                                        (structuralPart ?: 1.0F)

                            basePrice = priceAdapter.getPrice(
                                strategy.configuration.sourceCurrency,
                                s.targetCurrency,
                                fromAmount
                            )

                            if (basePrice == null) return@size

                            val toAmount = fromAmount * basePrice!! *
                                    (1.0F + s.spreadStructure[index].toFloat()) //  TODO: Add a check if spread and size structure are of a different length

                            //  In case the order in structure is less then it is allowed by the strategy, it is skipped
                            if (fromAmount < strategy.configuration.minOrderSize.toFloat()) {
                                logger2(
                                    "[$offsetIndex] Order is too small to place: ".yellow() +
                                            "$fromAmount ${strategy.configuration.sourceCurrency} --> $toAmount ${s.targetCurrency}"
                                )
                                return@size
                            }

                            val event = PlaceOrderEvent(
                                strategy.account.userAddress,
                                strategy.account.addressType,
                                strategy.account.workspaceId,
                                fromAmount.toString(),
                                toAmount.toString(),
                                strategy.configuration.sourceCurrency,
                                s.targetCurrency,
                                "Huckster MarketMaker",
                                SelfTradingPrevention.CancelOldest,
                                (offset.offset.toFloat() * 1000).toLong(),
                                (strategy.configuration.refreshInterval.soft.toFloat() * 1000).toLong()
                            )

                            try {
                                launch {
                                    OrdersQueue.enqueue(
                                        event,
                                        offsetIndex
                                    )
                                }
                            } catch(e: Exception) {
                                logger2(
                                    "[$offsetIndex] Coroutine failed to launch at this event:\n$event\n".red() +
                                            "Details:" + e.message + "\n" + e.stackTrace.joinToString("\n").red()
                                )
                            }
                        }
                    }
                }
            }

            delay((strategy.configuration.refreshInterval.hard.toFloat() * 1000).toLong())
            thread.cancelAndJoin()
        }
    }
}