package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll

private val tables: List<Table> = listOf(
    Contacts,
    GroupChatUsers,
    GroupChats,
    PrivateChatDeletions,
    PrivateChats,
    MessageStatuses,
    Messages,
    Chats
)

/** Drops every table and type created. */
fun tearDownDb(): Unit = transact {
    dropTables()
    dropTypes()
}

/** Deletes every row from every table created. */
fun wipeDb(): Unit = transact {
    tables.forEach { it.deleteAll() }
}

private fun dropTables(): Unit = SchemaUtils.drop(*tables.toTypedArray())

private fun dropTypes(): Unit = transact { exec("DROP TYPE IF EXISTS message_status;") }