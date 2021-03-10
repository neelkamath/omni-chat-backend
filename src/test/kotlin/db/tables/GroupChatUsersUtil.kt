package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Returns the primary keys in order of their creation. */
fun GroupChatUsers.read(): LinkedHashSet<Int> = transaction {
    selectAll().orderBy(GroupChatUsers.id).map { it[GroupChatUsers.id].value }.toSet() as LinkedHashSet
}

fun GroupChatUsers.makeAdmins(chatId: Int, vararg userIdList: Int): Unit = makeAdmins(chatId, userIdList.toList())
