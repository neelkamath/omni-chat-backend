package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.tables.Chats.id
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** The [id]s of [PrivateChats] and [GroupChats]. */
object Chats : IntIdTable() {
    /** Creates a chat ID. */
    fun create(): Int = transaction {
        insertAndGetId {}.value
    }

    fun delete(id: Int): Unit = transaction {
        deleteWhere { Chats.id eq id }
    }

    fun exists(id: Int): Boolean = transaction {
        !select { Chats.id eq id }.empty()
    }
}