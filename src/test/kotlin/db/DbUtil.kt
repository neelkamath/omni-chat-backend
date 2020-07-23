package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Table.count(): Long = transaction { selectAll().count() }

private val tables: List<Table> = listOf(
    Contacts,
    MessageStatuses,
    Stargazers,
    Messages,
    TypingStatuses,
    GroupChatUsers,
    GroupChats,
    PrivateChatDeletions,
    PrivateChats,
    Chats,
    Users,
    Pics
)

/** Deletes every row from every table created. */
fun wipeDb(): Unit = transaction {
    tables.forEach { it.deleteAll() }
}

/** Drops every table and type created. */
fun tearDownDb(): Unit = transaction {
    SchemaUtils.drop(*tables.toTypedArray())
    listOf("message_status", "pic_type").forEach { exec("DROP TYPE IF EXISTS $it;") }
}