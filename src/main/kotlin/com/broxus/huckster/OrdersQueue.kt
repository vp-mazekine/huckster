package com.broxus.huckster

import com.broxus.huckster.interfaces.OrdersQueue
import com.broxus.huckster.models.PlaceOrderEvent
import com.broxus.logger2
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.types.AddressType
import com.broxus.nova.types.OrderSideType
import com.importre.crayon.*
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
            event.toAmount
        )?.let {

            logger2(
                "[$threadId] Order ${it.transactionId} placed: ".recolorByThread(threadId) +
                        "${event.fromAmount} ${event.fromCurrency} --> ${event.toAmount} ${event.toCurrency}")
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
                    if (orders[0].fromExchangedValue.toFloat() != 0.0F) {
                        sequentialOrder.fromAmount =
                            (orders[0].fromValue.toFloat() - orders[0].fromExchangedValue.toFloat()).toString()
                        sequentialOrder.toAmount =
                            (orders[0].toValue.toFloat() - orders[0].toExchangedValue.toFloat()).toString()
                    }
                } catch (e: Exception) {
                    logger2(e.localizedMessage)
                }
            }

            //  Cancel current order
            api!!.cancelOrder(it.transactionId)

            enqueue(sequentialOrder, threadId)
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
        addressType: AddressType,
        workspaceId: String?,
        currency: String?
    ): Boolean {
        if(api == null) {
            logger.error("Nova API interface has not been initialized!")
            return false
        }

        logger2("Flushing all active ${currency ?: ""} orders...")

        api!!.getSpecificUserOrders(
            userAddress = userAddress,
            addressType = addressType,
            workspaceId = workspaceId,
            isAlive = true,
            base = currency
        )?.forEach {
            api!!.cancelOrder(it.transactionId)
        }

        return true
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
}