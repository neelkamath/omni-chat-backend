package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert

object PrivateChats : IntIdTable() {
    override val tableName get() = "private_chats"
    val creatorUserId = varchar("creator_user_id", Auth.userIdLength)
    val invitedUserId = varchar("invited_user_id", Auth.userIdLength)

    fun create(creatorUserId: String, invitedUserId: String): Unit = DB.transact {
        insert {
            it[this.creatorUserId] = creatorUserId
            it[this.invitedUserId] = invitedUserId
        }
    }
}