package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

data class PrivateMessage(
    val userId: String,
    val message: String,
    val status: PrivateMessageStatus,
    val dateTimes: PrivateMessageDateTimes
)

enum class PrivateMessageStatus { SENT, DELIVERED, READ }

data class PrivateMessageDateTimes(
    val sent: LocalDateTime,
    val delivered: LocalDateTime? = null,
    val read: LocalDateTime? = null
)

/** The messages for [PrivateChats]. */
object PrivateMessages : IntIdTable() {
    override val tableName get() = "private_chat_messages"
    private val chatId = integer("chat_id").references(PrivateChats.id)
    private const val maxMessageLength = 10_000
    private val message = varchar("message", maxMessageLength)
    private val sentDateTime = datetime("sent_date_time").clientDefault { LocalDateTime.now() }
    private val deliveredDateTime = datetime("delivered_date_time").nullable()
    private val readDateTime = datetime("read_date_time").nullable()

    /** One of `"SENT"`, `"DELIVERED"`, and `"READ"`. */
    private val status = varchar("status", "delivered".length).clientDefault { "SENT" }

    /** The ID of the user who sent the message. */
    private val userId = varchar("user_id", Auth.userIdLength)

    fun create(chatId: Int, userId: String, message: String): Unit = Db.transact {
        insert {
            it[this.chatId] = chatId
            it[this.message] = message
            it[this.userId] = userId
        }
    }

    /** Returns the [chatId]'s messages in the order of their creation. */
    fun read(chatId: Int): List<PrivateMessage> = Db.transact {
        select { PrivateMessages.chatId eq chatId }.map(::buildPrivateMessage)
    }

    private fun buildPrivateMessage(row: ResultRow): PrivateMessage = PrivateMessage(
        row[userId],
        row[message],
        enumerateStatus(row[status]),
        PrivateMessageDateTimes(row[sentDateTime], row[deliveredDateTime], row[readDateTime])
    )

    /** Converts the [status] (one of `"SENT"`, `"DELIVERED"`, or `"READ"`) to a [PrivateMessageStatus]. */
    private fun enumerateStatus(status: String): PrivateMessageStatus = when (status) {
        "SENT" -> PrivateMessageStatus.SENT
        "DELIVERED" -> PrivateMessageStatus.DELIVERED
        "READ" -> PrivateMessageStatus.READ
        else -> throw Error("""The status was "$status" instead of "SENT", "DELIVERED", or "READ".""")
    }
}