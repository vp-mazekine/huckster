package com.broxus.huckster

import com.broxus.huckster.interfaces.OrdersQueue
import com.broxus.huckster.models.PlaceOrderEvent
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.models.ExchangeOrderBook
import com.broxus.nova.types.OrderSideType
import com.broxus.utils.*
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Integer.max
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.coroutines.CoroutineContext

object OrdersQueue: OrdersQueue {
    private var api: NovaApiService? = null
    private val logger: Logger = LoggerFactory.getLogger(NovaApiService::class.java)

    override suspend fun enqueue(event: PlaceOrderEvent, threadId: Int?) {
        //  Shifted execution
        delay(event.initialDelay)

        api!!.createLimitOrder(
            UUID.randomUUID().toString(),
            event.userAddress,
            event.addressType,
            event.workspaceId,
            event.fromCurrency,
            event.toCurrency,
            event.fromAmount,
            event.toAmount,
            event.applicationId
        )?.let {

            logger2(
                "[$threadId] Order ${it.transactionId}: ".recolorByThread(threadId) +
                "${event.fromAmount} ${event.fromCurrency} --> ${event.toAmount} ${event.toCurrency} " +
                "[1 ${event.fromCurrency} = ${(event.toAmount.toFloat() / event.fromAmount.toFloat())} ${event.toCurrency}]".blue()
            )
            delay(event.cancelDelay)

            val sequentialOrder: PlaceOrderEvent = event

            val remainingOrder = api!!.getSpecificUserOrders(
                null,
                event.userAddress,
                event.addressType,
                event.workspaceId,
                event.fromCurrency,
                event.toCurrency,
                OrderSideType.sell,
                isAlive = true
            )

            remainingOrder?.filter { order ->
                order.transactionId == it.transactionId
            }?.let {orders ->
                try {   //  TODO: Add check for data validity
                    if (orders.first().fromExchangedValue.toFloat() != 0.0F) {
                        sequentialOrder.fromAmount =
                            (orders.first().fromValue.toFloat() - orders.first().fromExchangedValue.toFloat()).toString()
                        sequentialOrder.toAmount =
                            (orders.first().toValue.toFloat() - orders.first().toExchangedValue.toFloat()).toString()

                        if(orders.first().fromExchangedValue.toFloat() < event.fromAmount.toFloat()) {
                            logger2(
                                "[$threadId] Order ${it.transactionId} filled for ${orders.first().fromExchangedValue} ${event.fromCurrency}".recolorByThread(threadId)
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger2(e.message + "\n" +
                            e.stackTrace.joinToString("\n").red())
                }
            }

            //  Cancel current order
            //  Try several times and only after that throw the error
            var orderCancelled: Boolean = false

            for(i in 1..4) {
                if(api!!.cancelOrder(it.transactionId)) {
                    orderCancelled = true
                    break
                } else {
                    logger2(
                        "[$threadId] Order ${it.transactionId} cancellation delayed. Attempt #$i..."
                    )

                    //  Delay the execution by the increasing number of millis to let API restart
                    delay(
                        when (i) {
                            1 ->     1_000
                            2 ->    10_000
                            3 ->    30_000
                            else -> break
                        }
                    )
                }
            }

            if(orderCancelled) {
                logger2(
                    "[$threadId] Order ${it.transactionId} cancelled".recolorByThread(threadId)
                )

                //  Launch the order for the remainder of the balance
                enqueue(sequentialOrder, threadId)
            } else {
                logger2(
                    "[$threadId] Order ${it.transactionId} was not cancelled for some reason".recolorByThread(threadId)
                )
            }
        }
    }

    override fun init(api: NovaApiService) {
        this.api = api
    }

    /**
     * Cancels all active orders of the specified user
     *
     * @param userAddress
     * @param addressType
     * @param workspaceId
     * @return
     */
    override fun flush(
        userAddress: String,
        addressType: String,
        workspaceId: String?,
        from: String,
        to: String
    ): Boolean {
        if(api == null) {
            logger.error("Nova API interface has not been initialized!")
            return false
        }

        val orderSide = orderSide(from, to)
        var base: String = from
        var counter: String = to
        if(orderSide == OrderSideType.buy) {
            base = to
            counter = from
        }

        logger2("Flushing all active orders $from-->$to...")

        api!!.getSpecificUserOrders(
            userAddress = userAddress,
            addressType = addressType,
            workspaceId = workspaceId,
            isAlive = true,
            base = base,
            counter = counter,
            orderSide = orderSide
        )?.forEach {
            if(api!!.cancelOrder(it.transactionId)){
                logger2(
                    "Order ${it.transactionId} cancelled".green()
                )
            } else {
                logger2(
                    "Order ${it.transactionId} was not cancelled for some reason".green()
                )
            }
        }

        return true
    }

    override suspend fun drawOrderBook(base: String, counter: String, refreshInterval: Int) {
        var orderBook: ExchangeOrderBook?
        var averagePrice: Float
        var priceMaxLength: Int = 0
        var volumeMaxLength: Int = 0
        var minAskPrice: Float
        var maxBidPrice: Float
        var minVolume: Float = 0.0F
        var maxVolume: Float = 0.0F
        var priceColumnWidth: Int
        var volumeColumnWidth: Int
        val format = DecimalFormat(
            "# ##0.00######",
            DecimalFormatSymbols(Locale("en", "US")).apply {
                decimalSeparator = '.'
                groupingSeparator = ' '
            }
        ).apply {
            roundingMode = RoundingMode.HALF_UP
        }
        var outMessage: String
        val minScale = 1.0F
        val maxScale = 11.0F
        var interval: Float
        var bars: Int
        var t: String

        while(true) {
            orderBook = api!!.getOrderBook(base, counter)
            orderBook?.let {
                minAskPrice = 0.0F
                it.asks.forEach {ask ->
                    if((minAskPrice == 0.0F) || (minAskPrice > ask.rate.toFloat())) {
                        minAskPrice = ask.rate.toFloat()
                    }

                    if(ask.rate.length > priceMaxLength) priceMaxLength = ask.rate.length
                    if(ask.volume.length > volumeMaxLength) volumeMaxLength = ask.volume.length

                    if(ask.volume.toFloat() > maxVolume) maxVolume = ask.volume.toFloat()
                    if((minVolume == 0.0F) || (ask.volume.toFloat() < minVolume)) minVolume = ask.volume.toFloat()
                }

                maxBidPrice = 0.0F
                it.bids.forEach {bid ->
                    if((maxBidPrice == 0.0F) || (maxBidPrice < bid.rate.toFloat())) {
                        maxBidPrice = bid.rate.toFloat()
                    }

                    if(bid.rate.length > priceMaxLength) priceMaxLength = bid.rate.length
                    if(bid.volume.length > volumeMaxLength) volumeMaxLength = bid.volume.length

                    if(bid.volume.toFloat() > maxVolume) maxVolume = bid.volume.toFloat()
                    if((minVolume == 0.0F) || (bid.volume.toFloat() < minVolume)) minVolume = bid.volume.toFloat()
                }

                averagePrice = when {
                    (minAskPrice == 0.0F) && (maxBidPrice == 0.0F) -> 0.0F
                    (maxBidPrice == 0.0F) -> minAskPrice
                    (minAskPrice == 0.0F) -> maxBidPrice
                    else -> (minAskPrice + maxBidPrice) / 2
                }

                //  Clear console
                //print("\b".repeat(outMessage.length))
                //print(13.toChar())
                //print("\u001b[H\u001b[2J")
                //Runtime.getRuntime().exec("echo 'something'")
                print("\n".repeat(100))
                //print("\u001B[10E")
                //print(13.toChar())
                //println("\u001Bc")

                priceColumnWidth = max(priceMaxLength, 5) + 2
                volumeColumnWidth = 11 + volumeMaxLength + 4

                interval = (maxVolume - minVolume) / (maxScale - minScale)

                //  Header
                outMessage =
                    "┌" + "─".repeat(priceColumnWidth) + "┬" + "─".repeat(volumeColumnWidth) + "┐\n" +
                    "│ " + "Price".bold() + " ".repeat(priceColumnWidth - 6) + "│ " + "Volume".bold() + " ".repeat(volumeColumnWidth - 7) + "│\n" +
                    "├" + "─".repeat(priceColumnWidth) + "┼" + "─".repeat(volumeColumnWidth) + "┤\n"

                //  Asks chart
                it.asks.take(10).asReversed().forEach { ask ->
                    bars = ((ask.volume.toFloat() - minVolume) / interval).toInt() + 1
                    outMessage +=
                        "│ " + ask.rate + " ".repeat(priceColumnWidth - ask.rate.length - 1) + "│ " +
                        " ".repeat(volumeMaxLength - ask.volume.length) + ask.volume.red() +
                        " ".repeat(volumeColumnWidth - bars - volumeMaxLength - 2) + "█".repeat(bars).red() + " " + "│\n"
                }

                if(it.asks.count() == 0) {
                    outMessage +=
                        "│ " + "Empty asks orderbook".red() + " ".repeat(priceColumnWidth + volumeColumnWidth + 1 - 21) + "│\n"
                }

                //  Average price separator
                t = format.format(averagePrice) + " $base/$counter"
                outMessage +=
                    "├" + "─".repeat(priceColumnWidth) + "┴" + "─".repeat(volumeColumnWidth) + "┤\n" +
                    "│ " + t.italic() + " ".repeat(priceColumnWidth + volumeColumnWidth - t.length) + "│\n" +
                    "├" + "─".repeat(priceColumnWidth) + "┬" + "─".repeat(volumeColumnWidth) + "┤\n"

                //  Bids chart
                it.bids.take(10).asReversed().forEach { bid ->
                    bars = ((bid.volume.toFloat() - minVolume) / interval).toInt() + 1
                    outMessage +=
                        "│ " + bid.rate + " ".repeat(priceColumnWidth - bid.rate.length - 1) + "│ " +
                        " ".repeat(volumeMaxLength - bid.volume.length) + bid.volume.green() +
                        " ".repeat(volumeColumnWidth - bars - volumeMaxLength - 2) + "█".repeat(bars).green() + " " + "│\n"
                }

                if(it.bids.count() == 0) {
                    outMessage +=
                        "│ " + "Empty bids orderbook".green() + " ".repeat(priceColumnWidth + volumeColumnWidth + 1 - 21) + "│\n"
                }

                outMessage +=
                    "└" + "─".repeat(priceColumnWidth) + "┴" + "─".repeat(volumeColumnWidth) + "┘"

                print(outMessage)

                delay(refreshInterval * 1000L)
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = TODO("Not yet implemented")

    private fun String.recolorByThread(threadId: Int?): String {
        return when(threadId) {
            0 -> this.green()
            1 -> this.yellow()
            2 -> this.blue()
            3 -> this.magenta()
            4 -> this.cyan()
            else -> this.green()
        }
    }

    /**
     * Get order side for the specified exchange direction
     *
     * @param from Currency ticker
     * @param to Currency ticker
     * @return OrderSideType or null in case of error
     */
    fun orderSide(from: String, to: String): OrderSideType? {
        NovaApiService.getCurrenciesPairs()?.forEach {pair ->
            when {
                (pair.base == from && pair.counter == to) -> return OrderSideType.sell
                (pair.counter == from && pair.base == to) -> return OrderSideType.buy
                else -> {/* no-op */}
            }
        }

        return null
    }
}