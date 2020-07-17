package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Returns the number of users in every chat. */
fun GroupChatUsers.count(): Long = transaction { selectAll().count() }

/** Returns the primary keys in order of their creation. */
fun GroupChatUsers.read(): List<Int> = transaction {
    selectAll().map { it[GroupChatUsers.id].value }
}