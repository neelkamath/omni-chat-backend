package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [Messages] */
object GroupChatInviteMessages : Table() {
    override val tableName = "group_chat_invite_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)

    /** @see [Messages.createGroupChatInviteMessage] */
    fun create(id: Int, groupChatId: Int): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.groupChatId] = groupChatId
        }
    }

    /** Returns the ID of the invited chat. */
    fun read(messageId: Int): Int = transaction {
        select { GroupChatInviteMessages.messageId eq messageId }.first()[groupChatId]
    }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}