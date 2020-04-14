package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

data class PrivateChat(val id: Int, val creatorUserId: String, val invitedUserId: String)

object PrivateChats : IntIdTable() {
    override val tableName get() = "private_chats"
    val creatorUserId = varchar("creator_user_id", Auth.userIdLength)
    val invitedUserId = varchar("invited_user_id", Auth.userIdLength)

    /** Returns the chat ID after creating it. */
    fun create(creatorUserId: String, invitedUserId: String): Int = Db.transact {
        insertAndGetId {
            it[this.creatorUserId] = creatorUserId
            it[this.invitedUserId] = invitedUserId
        }.value
    }

    fun read(creatorUserId: String): List<PrivateChat> = Db.transact {
        select { PrivateChats.creatorUserId eq creatorUserId }
            .map { PrivateChat(it[id].value, it[this.creatorUserId], it[invitedUserId]) }
    }
}