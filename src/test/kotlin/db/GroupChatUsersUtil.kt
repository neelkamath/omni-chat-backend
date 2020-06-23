package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.selectAll

/** Returns the number of users in every chat. */
fun GroupChatUsers.count(): Long = transact { selectAll().count() }

/** Returns the primary keys in order of their creation. */
fun GroupChatUsers.read(): List<Int> = transact {
    selectAll().map { it[GroupChatUsers.id].value }
}