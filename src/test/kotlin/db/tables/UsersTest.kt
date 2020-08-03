package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.OnlineStatusesAsset
import com.neelkamath.omniChat.db.onlineStatusesBroker
import com.neelkamath.omniChat.graphql.routing.UpdatedOnlineStatus
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class UsersTest : FunSpec({
    context("setOnlineStatus(Int, Boolean)") {
        test("Updating the user's online status to the current value shouldn't cause notifications to be sent") {
            val (contactOwnerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            val subscriber =
                onlineStatusesBroker.subscribe(OnlineStatusesAsset(contactOwnerId)).subscribeWith(TestSubscriber())
            Users.setOnlineStatus(contactId, Users.read(contactId).isOnline)
            subscriber.assertNoValues()
        }

        test("Updating the user's status should only notify users who have them in their contacts or chats") {
            val (updaterId, contactOwnerId, chatSharerId, userId) = createVerifiedUsers(4).map { it.info.id }
            val (updaterSubscriber, contactOwnerSubscriber, chatSharerSubscriber, userSubscriber) =
                listOf(updaterId, contactOwnerId, chatSharerId, userId)
                    .map { onlineStatusesBroker.subscribe(OnlineStatusesAsset(it)).subscribeWith(TestSubscriber()) }
            Contacts.create(contactOwnerId, setOf(updaterId))
            PrivateChats.create(chatSharerId, updaterId)
            val status = Users.read(updaterId).isOnline.not()
            Users.setOnlineStatus(updaterId, status)
            listOf(updaterSubscriber, userSubscriber).forEach { it.assertNoValues() }
            listOf(contactOwnerSubscriber, chatSharerSubscriber)
                .forEach { it.assertValue(UpdatedOnlineStatus(updaterId, status)) }
        }
    }
})