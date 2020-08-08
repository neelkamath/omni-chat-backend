package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.graphql.routing.UpdatedAccount
import com.neelkamath.omniChat.graphql.routing.UpdatedContact
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class BrokerTest : FunSpec({
    context("negotiateUserUpdate(Int)") {
        test("Updating an account should trigger a notification for the contact owner, but not the contact") {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, setOf(contactId))
            val (ownerSubscriber, contactSubscriber) = listOf(ownerId, contactId)
                .map { contactsNotifier.safelySubscribe(ContactsAsset(it)).subscribeWith(TestSubscriber()) }
            negotiateUserUpdate(contactId)
            awaitBrokering()
            ownerSubscriber.assertValue(UpdatedContact.build(contactId))
            contactSubscriber.assertNoValues()
        }

        test(
            """
            Given subscribers to updated chats,
            when a user updates their account,
            then only the users who share their group chat should be notified of the updated account, except the updater
            """
        ) {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            listOf(user1Id, user2Id).forEach { GroupChats.create(listOf(adminId), listOf(it)) }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { updatedChatsNotifier.safelySubscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            negotiateUserUpdate(user1Id)
            awaitBrokering()
            adminSubscriber.assertValue(UpdatedAccount.build(user1Id))
            listOf(user1Subscriber, user2Subscriber).forEach { it.assertNoValues() }
        }

        test(
            """
            Given two users subscribed to the private chat,
            when one user updates their account,
            then only the other user should be notified
            """
        ) {
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
})

class NotifierTest : FunSpec({
    context("notify(List<Notification<A, U>>)") {
        test("Clients who have subscribed with a matching asset should be notified") {
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
})