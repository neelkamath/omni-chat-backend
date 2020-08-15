package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.tables.*
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Table.count(): Long = transaction { selectAll().count() }

private val tables: List<Table> = listOf(
    Contacts,
    MessageStatuses,
    Stargazers,
    TextMessages,
    PicMessages,
    AudioMessages,
    VideoMessages,
    DocMessages,
    GroupChatInviteMessages,
    PollVotes,
    PollOptions,
    PollMessages,
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