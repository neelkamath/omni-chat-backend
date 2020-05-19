package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.MessageUpdate
import com.neelkamath.omniChat.db.subscribeToMessageUpdates
import io.reactivex.rxjava3.subscribers.TestSubscriber

fun createMessageUpdatesSubscriber(userId: String, chatId: Int): TestSubscriber<MessageUpdate> {
    val subscription = subscribeToMessageUpdates(userId, chatId)
    val subscriber = TestSubscriber<MessageUpdate>()
    subscription.subscribe(subscriber)
    return subscriber
}