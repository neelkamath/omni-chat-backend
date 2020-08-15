package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.graphql.routing.UpdatedAccount
import com.neelkamath.omniChat.graphql.routing.UpdatedContact
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
        fun `Updating an account should trigger a notification for the contact owner, but not the contact`() {
            runBlocking {
                val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
                Contacts.create(ownerId, setOf(contactId))
                val (ownerSubscriber, contactSubscriber) = listOf(ownerId, contactId)
                    .map { contactsNotifier.safelySubscribe(ContactsAsset(it)).subscribeWith(TestSubscriber()) }
                negotiateUserUpdate(contactId)
                awaitBrokering()
                ownerSubscriber.assertValue(UpdatedContact.build(contactId))
                contactSubscriber.assertNoValues()
            }
        }

        @Test
        fun `Updating the user's account should only notify users in the same group chats except the updater`(): Unit =
            runBlocking {
                val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
                listOf(user1Id, user2Id).forEach { GroupChats.create(listOf(adminId), listOf(it)) }
                val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                    .map { updatedChatsNotifier.safelySubscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
                negotiateUserUpdate(user1Id)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedAccount.build(user1Id))
                listOf(user1Subscriber, user2Subscriber).forEach { it.assertNoValues() }
            }

        @Test
        fun `When a user in a private chat updates their account, only the other user should be notified`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                PrivateChats.create(user1Id, user2Id)
                val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                    .map { updatedChatsNotifier.safelySubscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
                negotiateUserUpdate(user2Id)
                awaitBrokering()
                user1Subscriber.assertValue(UpdatedAccount.build(user2Id))
                user2Subscriber.assertNoValues()
            }
        }
    }
}

@ExtendWith(DbExtension::class)
class NotifierTest {
    @Nested
    inner class Notify {
        @Test
        fun `Clients who have subscribed with a matching asset should be notified`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
                val notifier = Notifier<Int, String>(Topic.MESSAGES)
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