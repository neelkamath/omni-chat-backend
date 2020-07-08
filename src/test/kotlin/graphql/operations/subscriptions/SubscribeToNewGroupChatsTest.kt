package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_ID_FRAGMENT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_NEW_GROUP_CHATS_QUERY = """
    subscription SubscribeToNewGroupChats {
        subscribeToNewGroupChats {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $GROUP_CHAT_ID_FRAGMENT
        }
    }
"""

private fun operateSubscribeToNewGroupChats(accessToken: String, callback: SubscriptionCallback): Unit =
    operateGraphQlSubscription(
        uri = "new-group-chats-subscription",
        request = GraphQlRequest(SUBSCRIBE_TO_NEW_GROUP_CHATS_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun subscribeToNewGroupChats(accessToken: String, callback: SubscriptionCallback): Unit =
    operateSubscribeToNewGroupChats(accessToken) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

class SubscribeToNewGroupChatsTest : FunSpec({
    test("The user shouldn't be notified of a created chat they weren't added to") {
        val (admin, user) = createVerifiedUsers(2)
        subscribeToNewGroupChats(user.accessToken) { incoming ->
            GroupChats.create(admin.info.id, buildNewGroupChat())
            incoming.poll().shouldBeNull()
        }
    }

    test("The user should be notified of a newly created chat they were added to") {
        val (admin, user) = createVerifiedUsers(2)
        subscribeToNewGroupChats(user.accessToken) { incoming ->
            val chatId = GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
            parseFrameData<GroupChatId>(incoming).id shouldBe chatId
        }
    }

    test("The user should be notified of a previously created chat which they were recently added to") {
        val (admin, user) = createVerifiedUsers(2)
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat())
        subscribeToNewGroupChats(user.accessToken) { incoming ->
            GroupChats.update(GroupChatUpdate(chatId, newUserIdList = listOf(user.info.id)))
            parseFrameData<GroupChatId>(incoming).id shouldBe chatId
        }
    }

    test("The subscription should be stopped if the user deletes their account") {
        val (admin, user) = createVerifiedUsers(2)
        GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        subscribeToNewGroupChats(user.accessToken) { incoming ->
            deleteUser(user.info.id)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})