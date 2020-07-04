package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.graphql.GraphQlDocDataException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.EXITED_USER_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_GROUP_CHAT_INFO_QUERY = """
    subscription SubscribeToGroupChatInfo(${"$"}chatId: Int!) {
        subscribeToGroupChatInfo(chatId: ${"$"}chatId) {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $UPDATED_GROUP_CHAT_FRAGMENT
            $UPDATED_ACCOUNT_FRAGMENT
            $EXITED_USER_FRAGMENT
        }
    }
"""

private fun operateSubscribeToGroupChatInfo(accessToken: String, chatId: Int, callback: SubscriptionCallback) {
    val request = GraphQlRequest(SUBSCRIBE_TO_GROUP_CHAT_INFO_QUERY, variables = mapOf("chatId" to chatId))
    operateGraphQlSubscription(
        uri = "group-chat-info-subscription",
        request = request,
        accessToken = accessToken,
        callback = callback
    )
}

fun subscribeToGroupChatInfo(accessToken: String, chatId: Int, callback: SubscriptionCallback): Unit =
    operateSubscribeToGroupChatInfo(accessToken, chatId) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

fun errSubscribeToGroupChatInfo(accessToken: String, chatId: Int, exception: GraphQlDocDataException): Unit =
    operateSubscribeToGroupChatInfo(accessToken, chatId) { incoming ->
        parseFrameError(incoming) shouldBe exception.message
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

class SubscribeToGroupChatInfoTest : FunSpec({
    test("A notification should be received when the chat is updated") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat())
        val update = GroupChatUpdate(chatId, GroupChatTitle("New Title"), newUserIdList = listOf(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken, chatId) { incoming ->
            updateGroupChat(admin.accessToken, update)
            parseFrameData<UpdatedGroupChat>(incoming) shouldBe update.toUpdatedGroupChat()
        }
    }

    test("The subscription should be stopped if the user deletes their account even if the chat still exists") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(user.accessToken, chatId) { incoming ->
            deleteAccount(user.accessToken)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("The subscription should be stopped if the user leaves the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(user.accessToken, chatId) { incoming ->
            leaveGroupChat(user.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Updating an account should notify the updater and the other user in the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken, chatId) { adminIncoming ->
            subscribeToGroupChatInfo(user.accessToken, chatId) { userIncoming ->
                updateAccount(user.accessToken, AccountUpdate())
                parseFrameData<UpdatedAccount>(userIncoming) shouldBe user.info.toUpdatedAccount()
            }
            parseFrameData<UpdatedAccount>(adminIncoming) shouldBe user.info.toUpdatedAccount()
        }
    }

    test("When a user leaves, only the other user should be notified of such") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken, chatId) { adminIncoming ->
            subscribeToGroupChatInfo(user.accessToken, chatId) { userIncoming ->
                leaveGroupChat(user.accessToken, chatId)
                userIncoming.receive().frameType shouldBe FrameType.CLOSE
            }
            parseFrameData<ExitedUser>(adminIncoming) shouldBe ExitedUser(user.info.id)
        }
    }

    test("An error should be returned if an invalid chat ID is supplied") {
        val token = createSignedInUsers(1)[0].accessToken
        errSubscribeToGroupChatInfo(token, chatId = 1, exception = InvalidChatIdException)
    }
})