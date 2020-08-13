package com.neelkamath.omniChat.db

import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.time.delay
import java.time.Duration

/**
 * Makes up for the message broker's latency.
 *
 * There's a delay between messages being [Notifier.publish]ed and [Notifier.notify]d. This causes messages
 * [Notifier.publish]ed before a subscription to be received, and messages [Notifier.publish]ed after a subscription to
 * be missed.
 */
suspend fun awaitBrokering(): Unit = delay(Duration.ofMillis(100))

/** [Notifier.subscribe]s after [awaitBrokering]. */
suspend fun <A, U> Notifier<A, U>.safelySubscribe(asset: A): Flowable<U> {
    awaitBrokering()
    return subscribe(asset)
}

/** Removes every listener added in [subscribeToMessageBroker]. */
fun unsubscribeFromMessageBroker(): Unit =
    Topic.values().forEach { redisson.getTopic(it.toString()).removeAllListeners() }