package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.MessageDateTimeStatus
import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.USER_ID_LENGTH
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

/** When [Messages] were delivered/read. */
object MessageStatuses : IntIdTable() {
    override val tableName = "message_statuses"
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)
    private val status: Column<MessageStatus> = customEnumeration(
        name = "status",
        sql = "message_status",
        fromDb = {
            val status = it as String
            MessageStatus.valueOf(status.toUpperCase())
        },
        toDb = { PostgresEnum("message_status", it) }
    )

    /** The user recording the [status]. */
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    /** When the [status] was recorded. */
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }

    /**
     * Records that the [userId]'s [status] on the [messageId]. Clients who have [subscribeToMessageUpdates] will be
     * notified of the [messageId]'s [MessageStatus] update.
     *
     * If you record that the [userId] has [MessageStatus.READ] the [messageId], but haven't recorded that the [userId]
     * had a [messageId] [MessageStatus.DELIVERY], then it will also be recorded that the [userId] had a [messageId]
     * [MessageStatus.DELIVERY] too. An [IllegalArgumentException] will be thrown if the status has already been
     * recorded, or if the [userId] is the sender of the [messageId].
     */
    fun create(messageId: Int, userId: String, status: MessageStatus) {
        if (Messages.readSender(messageId) == userId)
            throw IllegalArgumentException("You cannot save a status for the user (ID: $userId) on their own message.")
        if (exists(messageId, userId, status)) {
            val text = if (status == MessageStatus.DELIVERY) "delivered to" else "seen by"
            throw IllegalArgumentException(
                "The message (ID: $messageId) has already been $text the user (ID: $userId)."
            )
        }
        if (status == MessageStatus.READ && !exists(messageId, userId, MessageStatus.DELIVERY))
            insertAndNotify(messageId, userId, MessageStatus.DELIVERY)
        insertAndNotify(messageId, userId, status)
    }

    /** Inserts the status into the DB, and notifies clients who have [subscribeToMessageUpdates]. */
    private fun insertAndNotify(messageId: Int, userId: String, status: MessageStatus) {
        transact {
            insert {
                it[this.messageId] = messageId
                it[this.userId] = userId
                it[this.status] = status
            }
        }
        notifyMessageUpdate(messageId)
    }

    /** Whether the [userId] has the specified [status] on the [messageId]. */
    private fun exists(messageId: Int, userId: String, status: MessageStatus): Boolean = !transact {
        select {
            (MessageStatuses.messageId eq messageId) and
                    (MessageStatuses.userId eq userId) and
                    (MessageStatuses.status eq status)
        }.empty()
    }

    /** Deletes all [MessageStatuses] from the [messageIdList]. */
    fun delete(messageIdList: List<Int>): Unit = transact {
        deleteWhere { messageId inList messageIdList }
    }

    /** Deletes all [MessageStatuses] from the [messageIdList]. */
    fun delete(vararg messageIdList: Int): Unit = delete(messageIdList.toList())

    /** Returns the [MessageDateTimeStatus]es for the [messageId]. */
    fun read(messageId: Int): List<MessageDateTimeStatus> = transact {
        select { MessageStatuses.messageId eq messageId }.map {
            MessageDateTimeStatus(
                it[userId],
                it[dateTime],
                it[status]
            )
        }
    }
}