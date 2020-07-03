package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.GraphQlDocDataException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deleteAccount
import com.neelkamath.omniChat.graphql.operations.mutations.updateAccount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_PRIVATE_INFO_QUERY = """
    subscription SubscribeToPrivateChatInfo(${"$"}chatId: Int!) {
        subscribeToPrivateChatInfo(chatId: ${"$"}chatId) {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $UPDATED_ACCOUNT_FRAGMENT
        }
    }
"""

private fun operateSubscribeToPrivateChatInfo(accessToken: String, chatId: Int, callback: SubscriptionCallback) {
    val request = GraphQlRequest(SUBSCRIBE_TO_PRIVATE_INFO_QUERY, variables = mapOf("chatId" to chatId))
    operateGraphQlSubscription(
        uri = "subscribe-to-private-chat-info",
        request = request,
        accessToken = accessToken,
        callback = callback
    )
}

fun subscribeToPrivateChatInfo(accessToken: String, chatId: Int, callback: SubscriptionCallback): Unit =
    operateSubscribeToPrivateChatInfo(accessToken, chatId) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

fun errSubscribeToPrivateChatInfo(accessToken: String, chatId: Int, exception: GraphQlDocDataException): Unit =
    operateSubscribeToPrivateChatInfo(accessToken, chatId) { incoming ->
        parseFrameError(incoming) shouldBe exception.message
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

class SubscribeToPrivateChatInfoTest : FunSpec({
    fun testAccountUpdate(shouldUpdateSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToPrivateChatInfo(user1.accessToken, chatId) { incoming ->
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

    fun testAccountDeletion(shouldDeleteSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToPrivateChatInfo(user1.accessToken, chatId) { incoming ->
            val user = if (shouldDeleteSubscriber) user1 else user2
            deleteAccount(user.accessToken)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("The subscription should be stopped if the user deletes their account") {
        testAccountDeletion(shouldDeleteSubscriber = true)
    }

    test("The subscription should be stopped if the other user deletes their account") {
        testAccountDeletion(shouldDeleteSubscriber = false)
    }

    test("An error should be returned if an invalid chat ID is supplied") {
        val token = createSignedInUsers(1)[0].accessToken
        errSubscribeToPrivateChatInfo(token, chatId = 1, exception = InvalidChatIdException)
    }
})