package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.UpdatedAccount
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(DbExtension::class)
class BrokerTest {
    @Nested
    inner class NegotiateUserUpdate {
        @Test
        fun `Updating an account must only notify non-deleted chat sharers, contact owners, and the updater`(): Unit =
            runBlocking {
                val (userId, contactOwnerId, deletedPrivateChatSharer, privateChatSharer, groupChatSharer) =
                    createVerifiedUsers(5).map { it.info.id }
                Contacts.create(contactOwnerId, setOf(userId))
                val chatId = PrivateChats.create(userId, deletedPrivateChatSharer)
                PrivateChatDeletions.create(chatId, userId)
                PrivateChats.create(userId, privateChatSharer)
                GroupChats.create(adminIdList = listOf(userId), userIdList = listOf(groupChatSharer))
                val (
                    userSubscriber,
                    contactOwnerSubscriber,
                    deletedPrivateChatSharerSubscriber,
                    privateChatSharerSubscriber,
                    groupChatSharerSubscriber
                ) = listOf(userId, contactOwnerId, deletedPrivateChatSharer, privateChatSharer, groupChatSharer)
                    .map { accountsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                negotiateUserUpdate(userId)
                awaitBrokering()
                deletedPrivateChatSharerSubscriber.assertNoValues()
                listOf(
                    userSubscriber,
                    contactOwnerSubscriber,
                    privateChatSharerSubscriber,
                    groupChatSharerSubscriber
                ).forEach { it.assertValue(UpdatedAccount.build(userId)) }
            }
    }
}

@ExtendWith(DbExtension::class)
class NotifierTest {
    @Nested
    inner class Notify {
        @Test
        fun `Clients who have subscribed with a matching asset must be notified`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
                val notifier = Notifier<String>(Topic.MESSAGES)
                val (subscriber1, subscriber2, subscriber3, subscriber4) = listOf(user1Id, user1Id, user2Id, user3Id)
                    .map { notifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                val update = "update"
                notifier.notify(listOf(Notification(user1Id, update), Notification(user2Id, update)))
                awaitBrokering()
                listOf(subscriber1, subscriber2, subscriber3).forEach { it.assertValue(update) }
                subscriber4.assertNoValues()
            }
        }
    }
}
