package com.neelkamath.omniChat.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

/**
 * Every time a user in a private chat cleared the chat for themselves.
 *
 * When a user deletes a private chat, the chat is only deleted for themselves. The person they were chatting with still
 * has the chat in its original condition. If the person they were chatting with sends them a message, it will appear as
 * the first message in a new private chat for the user.
 */
object PrivateChatClears : IntIdTable() {
    override val tableName get() = "private_chat_clears"
    val chatId = integer("chat_id").references(PrivateChats.id)
    private val pointInTime = datetime("point_in_time").clientDefault { LocalDateTime.now() }

    /** Whether the [PrivateChats.creatorUserId] cleared the chat ([PrivateChats.invitedUserId] otherwise). */
    val isCreator = bool("is_creator")

    /** Creates a new [pointInTime] for the deletion of the [chatId] ([isCreator] specifies who deleted it). */
    fun create(chatId: Int, isCreator: Boolean): Unit = Db.transact {
        insert {
            it[this.chatId] = chatId
            it[this.isCreator] = isCreator
        }
    }

    /** Whether the user (specified by whether they are the creator or not) has deleted the [chatId]. */
    fun hasCleared(isCreator: Boolean, chatId: Int): Boolean = Db.transact {
        with(select { PrivateChatClears.chatId eq chatId }) {
            if (empty()) false else first()[PrivateChatClears.isCreator] == isCreator
        }
    }

    /** Deletes every chat clear for the [chatId]. */
    fun delete(chatId: Int): Unit = Db.transact {
        deleteWhere { PrivateChatClears.chatId eq chatId }
    }
}