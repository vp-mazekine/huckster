package com.broxus.huckster.interfaces

import com.broxus.huckster.models.PlaceOrderEvent
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.types.AddressType
import kotlinx.coroutines.CoroutineScope

interface OrdersQueue: CoroutineScope {
    /**
     * Enqueue the event
     *
     * @param event Event to be enqueued for execution
     * @param initialDelay Period in milliseconds to delay queueing the event
     * @param cancelDelay Period in milliseconds after which the order cancelling event should be scheduled
     */
    suspend fun enqueue(event: PlaceOrderEvent, threadId: Int? = null)

    /**
     * Initialize the queue with api instance
     *
     * @param api Instance of the @NovaApiService
     */
    fun init(api: NovaApiService)

    /**
     * Cancel all active orders of the selected user
     *
     * @param userAddress User address
     * @param addressType Address type
     * @param workspaceId Workspace Id
     */
    fun flush(
        userAddress: String,
        addressType: AddressType,
        workspaceId: String?,
        currency: String?
    ): Boolean

    suspend fun drawOrderBook(base: String, counter: String, refreshInterval: Long)
}