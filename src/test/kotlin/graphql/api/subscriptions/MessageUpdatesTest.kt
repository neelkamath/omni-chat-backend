package com.neelkamath.omniChat.graphql.api.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.subscribeToMessageUpdates
import com.neelkamath.omniChat.graphql.ClientException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.SignedInUser
import com.neelkamath.omniChat.graphql.api.*
import com.neelkamath.omniChat.graphql.api.mutations.*
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import kotlinx.coroutines.channels.ReceiveChannel

const val MESSAGE_UPDATES_QUERY: String = """
    subscription MessageUpdates(${"$"}chatId: Int!) {
        messageUpdates(chatId: ${"$"}chatId) {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $MESSAGE_FRAGMENT
            $DELETED_MESSAGE_FRAGMENT
            $MESSAGE_DELETION_POINT_FRAGMENT
            $USER_CHAT_MESSAGES_REMOVAL_FRAGMENT
            $DELETION_OF_EVERY_MESSAGE_FRAGMENT
        }
    }
"""

private fun operateMessageUpdates(accessToken: String, chatId: Int, callback: SubscriptionCallback) {
    val request = GraphQlRequest(MESSAGE_UPDATES_QUERY, variables = mapOf("chatId" to chatId))
    subscribe(accessToken, "message-updates", request, callback)
}

fun receiveMessageUpdates(accessToken: String, chatId: Int, callback: SubscriptionCallback): Unit =
    operateMessageUpdates(accessToken, chatId) { incoming, outgoing ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming, outgoing)
    }

fun errMessageUpdates(accessToken: String, chatId: Int, exception: ClientException): Unit =
    operateMessageUpdates(accessToken, chatId) { incoming, _ ->
        parseFrameError(incoming) shouldBe exception.message
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

class MessageUpdatesTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    /**
     * Creates a message in a private chat, and asserts that it was received. If the [senderIsSubscriber], the
     * subscriber will send the message. Otherwise, the other user will send it.
     */
    fun receiveCreatedMessage(senderIsSubscriber: Boolean) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        receiveMessageUpdates(user1.accessToken, chatId) { incoming, _ ->
            val user = if (senderIsSubscriber) user1 else user2
            createMessage(user.accessToken, chatId, "text")
            parseFrameData<Message>(incoming) shouldBe Messages.readPrivateChat(chatId, user1.info.id).last().node
        }
    }

    test("The subscriber should receive their own messages") { receiveCreatedMessage(senderIsSubscriber = true) }

    test("The subscriber should receive messages from other users") {
        receiveCreatedMessage(senderIsSubscriber = false)
    }

    test("A message created in another chat shouldn't be received") {
        val (admin1, admin2) = createSignedInUsers(2)
        val chat1Id = createGroupChat(admin1.accessToken, NewGroupChat("Title"))
        val chat2Id = createGroupChat(admin2.accessToken, NewGroupChat("Title"))
        receiveMessageUpdates(admin1.accessToken, chat1Id) { incoming, _ ->
            createMessage(admin2.accessToken, chat2Id, "text")
            incoming.poll().shouldBeNull()
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
        receiveMessageUpdates(user1.accessToken, chatId) { incoming, _ ->
            val user = if (deleterIsSubscriber) user1 else user2
            createMessage(user.accessToken, chatId, "text")
            val messageId = parseFrameData<Message>(incoming).id
            deleteMessage(user.accessToken, messageId, chatId)
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
        val chat1Id = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val chat2Id = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val messageId = messageAndReadId(
            admin.accessToken,
            chat2Id,
            "text"
        )
        receiveMessageUpdates(admin.accessToken, chat1Id) { incoming, _ ->
            deleteMessage(admin.accessToken, messageId, chat2Id)
            incoming.poll().shouldBeNull()
        }
    }

    test("The user should be unsubscribed when they delete the private chat") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        receiveMessageUpdates(user1.accessToken, chatId) { incoming, _ ->
            deletePrivateChat(user1.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("The user should be unsubscribed when they leave a group chat") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        receiveMessageUpdates(token, chatId) { incoming, _ ->
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
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        createMessage(user.accessToken, chatId, "text")
        receiveMessageUpdates(admin.accessToken, chatId) { incoming, _ ->
            deleteAccount(user.accessToken)
            parseFrameData<UserChatMessagesRemoval>(incoming) shouldBe UserChatMessagesRemoval(user.info.id)
        }
    }

    /** Asserts the [incoming] of a [DeletionOfEveryMessage], and then that the [ReceiveChannel] is closed. */
    suspend fun assertChatDeletion(incoming: ReceiveChannel<Frame>) {
        parseFrameData<DeletionOfEveryMessage>(incoming) shouldBe DeletionOfEveryMessage()
        incoming.receive().frameType shouldBe FrameType.CLOSE
    }

    // This test is flaky for an unknown reason.
    test("When a user in a private chat deletes their account, both users in the chat should be unsubscribed") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        receiveMessageUpdates(user1.accessToken, chatId) { user1incoming, _ ->
            receiveMessageUpdates(user2.accessToken, chatId) { user2incoming, _ ->
                deleteAccount(user1.accessToken)
                assertChatDeletion(user2incoming)
            }
            assertChatDeletion(user1incoming)
        }
    }

    test("Subscribing to updates in a chat the user isn't in should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errMessageUpdates(chatId = 1, exception = InvalidChatIdException, accessToken = token)
    }

    test("The user should be able to subscribe to updates in a private chat they just deleted") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        receiveMessageUpdates(user1.accessToken, chatId) { _, _ -> }
    }

    /**
     * Creates a group chat with the [subscriber], [sender], and [statusCreator]. The [subscriber] is the user who has
     * [subscribeToMessageUpdates]. The [sender] is the user who sends a message. The [statusCreator] the user who
     * creates a [MessageStatus.DELIVERED] on the [sender]'s message. The [subscriber] and [sender] can be the same
     * [SignedInUser].
     */
    fun createUtilizedChat(
        subscriber: SignedInUser,
        sender: SignedInUser,
        statusCreator: SignedInUser,
        status: MessageStatus
    ) {
        val chat = NewGroupChat("Title", userIdList = listOf(sender.info.id, statusCreator.info.id))
        val chatId = createGroupChat(subscriber.accessToken, chat)
        receiveMessageUpdates(subscriber.accessToken, chatId) { incoming, _ ->
            createMessage(sender.accessToken, chatId, "text")
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
}