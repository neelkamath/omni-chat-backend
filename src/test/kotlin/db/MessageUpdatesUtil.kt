package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.MessageUpdates
import io.reactivex.rxjava3.subscribers.TestSubscriber

fun createMessageUpdatesSubscriber(userId: String, chatId: Int): TestSubscriber<MessageUpdates> {
    val subscription = subscribeToMessageUpdates(userId, chatId)
    val subscriber = TestSubscriber<MessageUpdates>()
    subscription.subscribe(subscriber)
    return subscriber
}