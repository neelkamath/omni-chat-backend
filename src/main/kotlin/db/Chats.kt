package com.neelkamath.omniChat.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId

/** The IDs for [PrivateChats] and [GroupChats]. */
object Chats : IntIdTable() {
    /** Creates a chat ID. */
    fun create(): Int = transact {
        insertAndGetId {}.value
    }

    fun delete(id: Int): Unit = transact {
        deleteWhere { Chats.id eq id }
    }
}