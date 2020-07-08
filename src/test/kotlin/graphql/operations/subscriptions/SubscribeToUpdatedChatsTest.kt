package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.PrivateChatDeletions
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.EXITED_USER_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_GROUP_CHAT_FRAGMENT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_UPDATED_CHATS_QUERY = """
    subscription SubscribeToUpdatedChats {
        subscribeToUpdatedChats {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $UPDATED_GROUP_CHAT_FRAGMENT
            $UPDATED_ACCOUNT_FRAGMENT
            $EXITED_USER_FRAGMENT
        }
    }
"""

private fun operateSubscribeToUpdatedChats(accessToken: String, callback: SubscriptionCallback) =
    operateGraphQlSubscription(
        uri = "updated-chats-subscription",
        request = GraphQlRequest(SUBSCRIBE_TO_UPDATED_CHATS_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun subscribeToUpdatedChats(accessToken: String, callback: SubscriptionCallback): Unit =
    operateSubscribeToUpdatedChats(accessToken) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

class SubscribeToUpdatedChatsTest : FunSpec({
    fun testAccountUpdate(shouldUpdateSubscriber: Boolean) {
        val (user1, user2) = createVerifiedUsers(2)
        PrivateChats.create(user1.info.id, user2.info.id)
        subscribeToUpdatedChats(user1.accessToken) { incoming ->
            val user = if (shouldUpdateSubscriber) user1 else user2
            val name = "new name"
            updateUser(user.info.id, AccountUpdate(firstName = name))
            if (shouldUpdateSubscriber) incoming.poll().shouldBeNull()
            else parseFrameData<UpdatedAccount>(incoming) shouldBe user2.info.copy(firstName = name).toUpdatedAccount()
        }
    }

    test("A notification should be received when the other user in the private chat updates their account") {
        testAccountUpdate(shouldUpdateSubscriber = false)
    }

    test("Notifications shouldn't be received for the user's own account updates") {
        testAccountUpdate(shouldUpdateSubscriber = true)
    }

    test("A notification should be sent if the user from a deleted private chat updates their account") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        PrivateChatDeletions.create(chatId, user1.info.id)
        subscribeToUpdatedChats(user1.accessToken) { incoming ->
            updateUser(user2.info.id, AccountUpdate(firstName = "new name"))
            incoming.poll().shouldNotBeNull()
        }
    }

    test("A notification should be received when the group chat is updated") {
        val (admin, user) = createVerifiedUsers(2)
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat())
        val update = GroupChatUpdate(chatId, GroupChatTitle("New Title"), newUserIdList = listOf(user.info.id))
        subscribeToUpdatedChats(admin.accessToken) { incoming ->
            GroupChats.update(update)
            parseFrameData<UpdatedGroupChat>(incoming) shouldBe update.toUpdatedGroupChat()
        }
    }

    test("Updating an account should notify only the other user in the group chat") {
        val (admin, user) = createVerifiedUsers(2)
        GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        subscribeToUpdatedChats(admin.accessToken) { adminIncoming ->
            subscribeToUpdatedChats(user.accessToken) { userIncoming ->
                updateUser(user.info.id, AccountUpdate())
                userIncoming.poll().shouldBeNull()
            }
            parseFrameData<UpdatedAccount>(adminIncoming) shouldBe user.info.toUpdatedAccount()
        }
    }

    test("When a user leaves the group chat, only the other user should be notified of such") {
        val (admin, user) = createVerifiedUsers(2)
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        subscribeToUpdatedChats(admin.accessToken) { adminIncoming ->
            subscribeToUpdatedChats(user.accessToken) { userIncoming ->
                GroupChatUsers.removeUsers(chatId, user.info.id)
                userIncoming.poll().shouldBeNull()
            }
            parseFrameData<ExitedUser>(adminIncoming) shouldBe ExitedUser(chatId, user.info.id)
        }
    }

    test("The subscription should be stopped if the user deletes their account") {
        val user = createVerifiedUsers(1)[0]
        subscribeToUpdatedChats(user.accessToken) { incoming ->
            deleteUser(user.info.id)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})