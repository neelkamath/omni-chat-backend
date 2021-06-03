package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.MessagesSubscription
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedMessage
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** When [Messages] were delivered and read. */
object MessageStatuses : IntIdTable() {
    override val tableName = "message_statuses"
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)
    private val status: Column<MessageStatus> = customEnumeration(
        name = "status",
        sql = "message_status",
        fromDb = { MessageStatus.valueOf((it as String).uppercase()) },
        toDb = { PostgresEnum("message_status", it) },
    )

    /** The user recording the [status]. */
    private val userId: Column<Int> = integer("user_id").references(Users.id)

    /** When the [status] was recorded. */
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }

    /**
     * Records that the [userId]'s [status] on the [messageId]. Clients who have [Notifier.subscribe]d to
     * [MessagesSubscription]s via [messagesNotifier] will be notified of the [messageId]'s [MessageStatus] update.
     *
     * If you record that the [userId] has [MessageStatus.READ] the [messageId] but haven't recorded that the [userId]
     * had a [messageId] [MessageStatus.DELIVERED], then it'll also be recorded that the [userId] had the [messageId]
     * [MessageStatus.DELIVERED].
     *
     * An [IllegalArgumentException] will be thrown if:
     * - The [messageId] was sent by the [userId].
     * - The status has already been recorded (you can check if the [status] [isExisting]).
     * - The [messageId] isn't visible to the [userId] (you can check if the [Messages.isVisible]).
     */
    fun create(userId: Int, messageId: Int, status: MessageStatus) {
        require(Messages.isVisible(userId, messageId)) {
            """
            The user (ID: $userId) can't see the message (ID: $messageId) because it was sent before they deleted the
            chat.
            """
        }
        require(Messages.readSenderId(messageId) != userId) {
            "You cannot save a status for the user (ID: $userId) on their own message."
        }
        require(!isExisting(messageId, userId, status)) {
            val text = if (status == MessageStatus.DELIVERED) "delivered to" else "seen by"
            "The message (ID: $messageId) has already been $text the user (ID: $userId)."
        }
        if (status == MessageStatus.READ && !isExisting(messageId, userId, MessageStatus.DELIVERED))
            insertAndNotify(messageId, userId, MessageStatus.DELIVERED)
        insertAndNotify(messageId, userId, status)
    }

    /**
     * Inserts the status into the table.
     *
     * Notifies subscribers of the [UpdatedMessage]s via [messagesNotifier]. If the chat the [messageId] belongs to is a
     * public group chat, then subscribers will also be notified via [chatMessagesNotifier].
     */
    private fun insertAndNotify(messageId: Int, userId: Int, status: MessageStatus) {
        transaction {
            insert {
                it[this.messageId] = messageId
                it[this.userId] = userId
                it[this.status] = status
            }
        }
        val chatId = Messages.readChatId(messageId)
        val update = UpdatedMessage(messageId)
        messagesNotifier.publish(update, readUserIdList(chatId).map(::UserId))
        if (GroupChats.isExistingPublicChat(chatId)) chatMessagesNotifier.publish(update, ChatId(chatId))
    }

    /** Returns the status IDs of the [messageId] sorted in ascending order. */
    fun readIdList(messageId: Int): LinkedHashSet<Int> = transaction {
        select(MessageStatuses.messageId eq messageId)
            .orderBy(MessageStatuses.id)
            .map { it[MessageStatuses.id].value }
            .toLinkedHashSet()
    }

    /** Whether the [userId] has the specified [status] on the [messageId]. */
    fun isExisting(messageId: Int, userId: Int, status: MessageStatus): Boolean = transaction {
        select(
            (MessageStatuses.messageId eq messageId) and
                    (MessageStatuses.userId eq userId) and
                    (MessageStatuses.status eq status)
        ).empty().not()
    }

    /** Deletes [MessageStatuses] from the [messageIdList], ignoring invalid ones. */
    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /** Deletes every status the [userId] created in the [chatId]. */
    fun deleteUserChatStatuses(chatId: Int, userId: Int) = transaction {
        deleteWhere { (messageId inList Messages.readIdList(chatId)) and (MessageStatuses.userId eq userId) }
    }

    /** Deletes every status the [userId] created. Nothing happens if the [userId] doesn't exist. */
    fun deleteUserStatuses(userId: Int): Unit = transaction {
        deleteWhere { MessageStatuses.userId eq userId }
    }

    /** Returns the ID of the user who created the specified [MessageStatuses.id]. */
    fun readUserId(statusId: Int): Int = transaction { select(MessageStatuses.id eq statusId).first()[userId] }

    /** Returns the [LocalDateTime] of when the specified [MessageStatuses.id] was created. */
    fun readDateTime(statusId: Int): LocalDateTime =
        transaction { select(MessageStatuses.id eq statusId).first()[dateTime] }

    /** Returns the [MessageStatus] of the specified [MessageStatuses.id]. */
    fun readStatus(statusId: Int): MessageStatus =
        transaction { select(MessageStatuses.id eq statusId).first()[status] }

    fun countStatuses(messageId: Int, status: MessageStatus): Long =
        transaction { select((MessageStatuses.messageId eq messageId) and (MessageStatuses.status eq status)).count() }
}
