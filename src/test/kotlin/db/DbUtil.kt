package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.db.tables.*
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Table.count(): Long = transaction { selectAll().count() }

private val tables: Set<Table> = setOf(
    Contacts,
    Bookmarks,
    TextMessages,
    ActionMessageActions,
    ActionMessages,
    ImageMessages,
    AudioMessages,
    VideoMessages,
    DocMessages,
    GroupChatInviteMessages,
    PollMessageVotes,
    PollMessageOptions,
    PollMessages,
    Messages,
    TypingStatuses,
    GroupChatUsers,
    GroupChats,
    PrivateChatDeletions,
    PrivateChats,
    Chats,
    BlockedUsers,
    Users,
    Images,
)

/** Deletes every row from every table created. */
fun wipeDb(): Unit = transaction {
    tables.forEach { it.deleteAll() }
}
