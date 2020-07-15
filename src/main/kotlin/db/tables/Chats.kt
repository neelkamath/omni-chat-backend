package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.tables.Chats.id
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

/** The [id]s of [PrivateChats] and [GroupChats]. */
object Chats : IntIdTable() {
    /** Creates a chat ID. */
    fun create(): Int = transact {
        insertAndGetId {}.value
    }

    fun delete(id: Int): Unit = transact {
        deleteWhere { Chats.id eq id }
    }

    fun exists(id: Int): Boolean = transact {
        !select { Chats.id eq id }.empty()
    }
}