package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

data class Message(val userId: String, val message: String, val status: MessageStatus, val dateTimes: MessageDateTimes)

enum class MessageStatus { SENT, DELIVERED, READ }

data class MessageDateTimes(
    val sent: LocalDateTime,
    val delivered: LocalDateTime? = null,
    val read: LocalDateTime? = null
)

/** The messages for [PrivateChats] and [GroupChats]. */
object Messages : IntIdTable() {
    private val chatId = integer("chat_id")
    const val maxMessageLength = 10_000
    private val message = varchar("message", maxMessageLength)
    private val sentDateTime = datetime("sent_date_time").clientDefault { LocalDateTime.now() }
    private val deliveredDateTime = datetime("delivered_date_time").nullable()
    private val readDateTime = datetime("read_date_time").nullable()

    /** One of `"SENT"`, `"DELIVERED"`, and `"READ"`. */
    private val status = varchar("status", "delivered".length).clientDefault { "SENT" }

    /** The ID of the user who sent the message. */
    private val userId = varchar("user_id", Auth.userIdLength)

    /** Returns the [LocalDateTime] the message was created at. */
    fun create(chatId: Int, userId: String, message: String): LocalDateTime = Db.transact {
        insert {
            it[this.chatId] = chatId
            it[this.message] = message
            it[this.userId] = userId
        }[sentDateTime]
    }

    /** Returns the [chatId]'s messages in the order of their creation. */
    fun read(chatId: Int): List<Message> = Db.transact {
        select { Messages.chatId eq chatId }.map(::buildMessage)
    }

    /** Deletes every message from the [chatId]. */
    fun delete(chatId: Int): Unit = Db.transact {
        deleteWhere { Messages.chatId eq chatId }
    }

    /** Deletes messages in the [chatId] [upTo] the specified [LocalDateTime]. */
    fun delete(chatId: Int, upTo: LocalDateTime): Unit = Db.transact {
        deleteWhere { (Messages.chatId eq chatId) and (sentDateTime lessEq upTo) }
    }

    /** Deletes the [userId]'s messages from the [chatId]. */
    fun delete(chatId: Int, userId: String): Unit = Db.transact {
        deleteWhere { (Messages.chatId eq chatId) and (Messages.userId eq userId) }
    }

    private fun buildMessage(row: ResultRow): Message = Message(
        row[userId],
        row[message],
        enumerateStatus(row[status]),
        MessageDateTimes(row[sentDateTime], row[deliveredDateTime], row[readDateTime])
    )

    /** Converts the [status] (one of `"SENT"`, `"DELIVERED"`, or `"READ"`) to a [MessageStatus]. */
    private fun enumerateStatus(status: String): MessageStatus = when (status) {
        "SENT" -> MessageStatus.SENT
        "DELIVERED" -> MessageStatus.DELIVERED
        "READ" -> MessageStatus.READ
        else -> throw Error("""The status was "$status" instead of "SENT", "DELIVERED", or "READ".""")
    }
}