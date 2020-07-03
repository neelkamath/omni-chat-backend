package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.TextMessage
import com.neelkamath.omniChat.graphql.GraphQlDocDataException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.SignedInUser
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.*
import com.neelkamath.omniChat.graphql.operations.mutations.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import kotlinx.coroutines.channels.ReceiveChannel

const val SUBSCRIBE_TO_MESSAGES_QUERY = """
    subscription SubscribeToMessages(${"$"}chatId: Int!) {
        subscribeToMessages(chatId: ${"$"}chatId) {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $NEW_MESSAGE_FRAGMENT
            $UPDATED_MESSAGE_FRAGMENT
            $DELETED_MESSAGE_FRAGMENT
            $MESSAGE_DELETION_POINT_FRAGMENT
            $USER_CHAT_MESSAGES_REMOVAL_FRAGMENT
            $DELETION_OF_EVERY_MESSAGE_FRAGMENT
        }
    }
"""

private fun operateSubscribeToMessages(accessToken: String, chatId: Int, callback: SubscriptionCallback) {
    val request = GraphQlRequest(SUBSCRIBE_TO_MESSAGES_QUERY, variables = mapOf("chatId" to chatId))
    operateGraphQlSubscription(
        uri = "subscribe-to-messages",
        request = request,
        accessToken = accessToken,
        callback = callback
    )
}

fun subscribeToMessages(accessToken: String, chatId: Int, callback: SubscriptionCallback): Unit =
    operateSubscribeToMessages(accessToken, chatId) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }

fun errSubscribeToMessages(accessToken: String, chatId: Int, exception: GraphQlDocDataException): Unit =
    operateSubscribeToMessages(accessToken, chatId) { incoming ->
        parseFrameError(incoming) shouldBe exception.message
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

class SubscribeToMessagesTest : FunSpec({
    /**
     * Creates a message in a private chat, and asserts that it was received. If the [senderIsSubscriber], the
     * subscriber will send the message. Otherwise, the other user will send it.
     */
    fun receiveCreatedMessage(senderIsSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToMessages(user1.accessToken, chatId) { incoming ->
            val user = if (senderIsSubscriber) user1 else user2
            createMessage(user.accessToken, chatId, TextMessage("text"))
            parseFrameData<Message>(incoming) shouldBe Messages.readPrivateChat(chatId, user1.info.id).last().node
        }
    }

    test("The subscriber should receive their own messages") { receiveCreatedMessage(senderIsSubscriber = true) }

    test("The subscriber should receive messages from other users") {
        receiveCreatedMessage(senderIsSubscriber = false)
    }

    test("A message created in another chat shouldn't be received") {
        val (admin1, admin2) = createSignedInUsers(2)
        val chat1 = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chat1Id = createGroupChat(admin1.accessToken, chat1)
        val chat2 = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chat2Id = createGroupChat(admin2.accessToken, chat2)
        subscribeToMessages(admin1.accessToken, chat1Id) { incoming ->
            createMessage(admin2.accessToken, chat2Id, TextMessage("text"))
            incoming.poll().shouldBeNull()
        }
    }

    test("The user should be unsubscribed from the chat's message updates when they leave the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""), listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        subscribeToMessages(user.accessToken, chatId) { incoming ->
            leaveGroupChat(user.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Deleting a user should delete their message update subscriptions for group chats") {
        val token = createSignedInUsers(1)[0].accessToken
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(token, chat)
        subscribeToMessages(token, chatId) { incoming ->
            deleteAccount(token)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Deleting a chat should unsubscribe the user from its message updates") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToMessages(user1.accessToken, chatId) { incoming ->
            deletePrivateChat(user1.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    /**
     * Creates a private chat, sends a message in it, deletes the sent message, and verifies that a notification
     * regarding the message's deletion was received. If the [deleterIsSubscriber], the subscriber will delete the
     * message. Otherwise, the other chat user will delete the message.
     */
    fun receiveDeletedMessage(deleterIsSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToMessages(user1.accessToken, chatId) { incoming ->
            val user = if (deleterIsSubscriber) user1 else user2
            createMessage(user.accessToken, chatId, TextMessage("text"))
            val messageId = parseFrameData<Message>(incoming).id
            deleteMessage(user.accessToken, messageId)
            parseFrameData<DeletedMessage>(incoming) shouldBe DeletedMessage(messageId)
        }
    }

    test("A subscriber should be notified when they delete their own message") {
        receiveDeletedMessage(deleterIsSubscriber = true)
    }

    test("A subscriber should be notified when another user deletes a message") {
        receiveDeletedMessage(deleterIsSubscriber = false)
    }

    test("A message deleted in another chat shouldn't be received") {
        val admin = createSignedInUsers(1)[0]
        val chat1 = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chat1Id = createGroupChat(admin.accessToken, chat1)
        val chat2 = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chat2Id = createGroupChat(admin.accessToken, chat2)
        val messageId = messageAndReadId(admin.accessToken, chat2Id, TextMessage("text"))
        subscribeToMessages(admin.accessToken, chat1Id) { incoming ->
            deleteMessage(admin.accessToken, messageId)
            incoming.poll().shouldBeNull()
        }
    }

    test("The user should be unsubscribed when they delete the private chat") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToMessages(user1.accessToken, chatId) { incoming ->
            deletePrivateChat(user1.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("The user should be unsubscribed when they leave a group chat") {
        val token = createSignedInUsers(1)[0].accessToken
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(token, chat)
        subscribeToMessages(token, chatId) { incoming ->
            leaveGroupChat(token, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test(
        """
        Given a group chat,
        when a member has deleted their account,
        then the members should be notified that the old member's messages were deleted 
        """
    ) {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""), listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        createMessage(user.accessToken, chatId, TextMessage("text"))
        subscribeToMessages(admin.accessToken, chatId) { incoming ->
            deleteAccount(user.accessToken)
            parseFrameData<UserChatMessagesRemoval>(incoming) shouldBe UserChatMessagesRemoval(user.info.id)
        }
    }

    /** Asserts the [incoming] of a [DeletionOfEveryMessage], and then that the [ReceiveChannel] is closed. */
    suspend fun assertChatDeletion(incoming: ReceiveChannel<Frame>) {
        parseFrameData<DeletionOfEveryMessage>(incoming) shouldBe DeletionOfEveryMessage
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

    test("When a user in a private chat deletes their account, both users in the chat should be unsubscribed") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        subscribeToMessages(user1.accessToken, chatId) { user1incoming ->
            subscribeToMessages(user2.accessToken, chatId) { user2incoming ->
                deleteAccount(user1.accessToken)
                assertChatDeletion(user2incoming)
            }
            assertChatDeletion(user1incoming)
        }
    }

    test("Subscribing to updates in a chat the user isn't in should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errSubscribeToMessages(chatId = 1, exception = InvalidChatIdException, accessToken = token)
    }

    test("The user should be able to subscribe to updates in a private chat they just deleted") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        subscribeToMessages(user1.accessToken, chatId) { }
    }

    /**
     * Creates a group chat with the [subscriber], [sender], and [statusCreator]. The [subscriber] is the user who will
     * [subscribeToMessages]. The [sender] is the user who sends a message. The [statusCreator] the user who creates a
     * [MessageStatus.DELIVERED] on the [sender]'s message. The [subscriber] and [sender] can be the same
     * [SignedInUser].
     */
    fun createUtilizedChat(
        subscriber: SignedInUser,
        sender: SignedInUser,
        statusCreator: SignedInUser,
        status: MessageStatus
    ) {
        val chat = NewGroupChat(
            GroupChatTitle("Title"),
            GroupChatDescription(""),
            listOf(sender.info.id, statusCreator.info.id)
        )
        val chatId = createGroupChat(subscriber.accessToken, chat)
        subscribeToMessages(subscriber.accessToken, chatId) { incoming ->
            createMessage(sender.accessToken, chatId, TextMessage("text"))
            val messageId = parseFrameData<Message>(incoming).id
            when (status) {
                MessageStatus.DELIVERED -> createDeliveredStatus(statusCreator.accessToken, messageId)
                MessageStatus.READ -> createReadStatus(statusCreator.accessToken, messageId)
            }
            if (status == MessageStatus.READ) incoming.poll() // Ignore the "delivered" status.
            // We convert it to a set because in the case of a "read" status, a "delivered" status would also exist.
            parseFrameData<Message>(incoming).dateTimes.statuses.map { it.user.id }.toSet() shouldBe
                    setOf(statusCreator.info.id)
        }
    }

    fun `the subscriber should be notified when a user creates a status on their message`(status: MessageStatus): Unit =
        test(
            """
            The subscriber should be notified when a user creates a "${status.name.toLowerCase()}" status on their 
            message
            """
        ) {
            val (user1, user2) = createSignedInUsers(2)
            createUtilizedChat(subscriber = user1, sender = user1, statusCreator = user2, status = status)
        }

    fun `the subscriber should be notified when they create a status a user's message`(status: MessageStatus): Unit =
        test(
            """
            The subscriber should be notified when they create a "${status.name.toLowerCase()}" status a user's message
            """
        ) {
            val (user1, user2) = createSignedInUsers(2)
            createUtilizedChat(subscriber = user1, sender = user2, statusCreator = user1, status = status)
        }

    fun `the subscriber should be notified when a user creates a status on another user's message`(
        status: MessageStatus
    ): Unit = test(
        """
        The subscriber should be notified when a user creates a "${status.name.toLowerCase()}" status on another user's 
        message
        """
    ) {
        val (user1, user2, user3) = createSignedInUsers(3)
        createUtilizedChat(subscriber = user1, sender = user2, statusCreator = user3, status = status)
    }

    `the subscriber should be notified when a user creates a status on their message`(MessageStatus.DELIVERED)

    `the subscriber should be notified when they create a status a user's message`(MessageStatus.DELIVERED)

    `the subscriber should be notified when a user creates a status on another user's message`(MessageStatus.DELIVERED)

    `the subscriber should be notified when a user creates a status on their message`(MessageStatus.READ)

    `the subscriber should be notified when they create a status a user's message`(MessageStatus.READ)

    `the subscriber should be notified when a user creates a status on another user's message`(MessageStatus.READ)
})