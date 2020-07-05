package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.EXITED_USER_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_GROUP_CHAT_INFO_QUERY = """
    subscription SubscribeToGroupChatInfo {
        subscribeToGroupChatInfo {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $UPDATED_GROUP_CHAT_FRAGMENT
            $UPDATED_ACCOUNT_FRAGMENT
            $EXITED_USER_FRAGMENT
        }
    }
"""

private fun operateSubscribeToGroupChatInfo(accessToken: String, callback: SubscriptionCallback) =
    operateGraphQlSubscription(
        uri = "group-chat-info-subscription",
        request = GraphQlRequest(SUBSCRIBE_TO_GROUP_CHAT_INFO_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun subscribeToGroupChatInfo(accessToken: String, callback: SubscriptionCallback): Unit =
    operateSubscribeToGroupChatInfo(accessToken) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

class SubscribeToGroupChatInfoTest : FunSpec({
    test("A notification should be received when the chat is updated") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat())
        val update = GroupChatUpdate(chatId, GroupChatTitle("New Title"), newUserIdList = listOf(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken) { incoming ->
            updateGroupChat(admin.accessToken, update)
            parseFrameData<UpdatedGroupChat>(incoming) shouldBe update.toUpdatedGroupChat()
        }
    }

    test("The subscription should be stopped if the user deletes their account even if the chat still exists") {
        val token = createSignedInUsers(1)[0].accessToken
        subscribeToGroupChatInfo(token) { incoming ->
            deleteAccount(token)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Updating an account should notify the other user in the chat, but not the updater") {
        val (admin, user) = createSignedInUsers(2)
        createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken) { adminIncoming ->
            subscribeToGroupChatInfo(user.accessToken) { userIncoming ->
                updateAccount(user.accessToken, AccountUpdate())
                userIncoming.poll().shouldBeNull()
            }
            parseFrameData<UpdatedAccount>(adminIncoming) shouldBe user.info.toUpdatedAccount()
        }
    }

    test("When a user leaves, only the other user should be notified of such") {
        val (admin, user) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
        subscribeToGroupChatInfo(admin.accessToken) { adminIncoming ->
            subscribeToGroupChatInfo(user.accessToken) { userIncoming ->
                leaveGroupChat(user.accessToken, chatId)
                userIncoming.poll().shouldBeNull()
            }
            parseFrameData<ExitedUser>(adminIncoming) shouldBe ExitedUser(chatId, user.info.id)
        }
    }
})