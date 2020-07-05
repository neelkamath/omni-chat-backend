package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deleteAccount
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.updateAccount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_PRIVATE_INFO_QUERY = """
    subscription SubscribeToPrivateChatInfo {
        subscribeToPrivateChatInfo {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $UPDATED_ACCOUNT_FRAGMENT
        }
    }
"""

private fun operateSubscribeToPrivateChatInfo(accessToken: String, callback: SubscriptionCallback) =
    operateGraphQlSubscription(
        uri = "private-chat-info-subscription",
        request = GraphQlRequest(SUBSCRIBE_TO_PRIVATE_INFO_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun subscribeToPrivateChatInfo(accessToken: String, callback: SubscriptionCallback): Unit =
    operateSubscribeToPrivateChatInfo(accessToken) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

class SubscribeToPrivateChatInfoTest : FunSpec({
    fun testAccountUpdate(shouldUpdateSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToPrivateChatInfo(user1.accessToken) { incoming ->
            val user = if (shouldUpdateSubscriber) user1 else user2
            val name = "new name"
            updateAccount(user.accessToken, AccountUpdate(firstName = name))
            if (shouldUpdateSubscriber) incoming.poll().shouldBeNull()
            else parseFrameData<UpdatedAccount>(incoming) shouldBe user2.info.copy(firstName = name).toUpdatedAccount()
        }
    }

    test("A notification should be received when the other user in the chat updates their account") {
        testAccountUpdate(shouldUpdateSubscriber = false)
    }

    test("Notifications shouldn't be received for the user's own account updates") {
        testAccountUpdate(shouldUpdateSubscriber = true)
    }

    test("A notification should be sent if the user from a deleted chat updates their account") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        subscribeToPrivateChatInfo(user1.accessToken) { incoming ->
            updateAccount(user2.accessToken, AccountUpdate(firstName = "new name"))
            incoming.poll().shouldNotBeNull()
        }
    }

    test("The subscription should be stopped if the user deletes their account") {
        val (user1, user2) = createSignedInUsers(2)
        createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToPrivateChatInfo(user1.accessToken) { incoming ->
            deleteAccount(user1.accessToken)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})