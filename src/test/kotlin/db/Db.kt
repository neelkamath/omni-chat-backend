package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.*
import org.jetbrains.exposed.sql.deleteAll

/** Runs [deleteAll] on every table. */
fun tearDownDb(): Unit = transact {
    Contacts.deleteAll()
    GroupChatUsers.deleteAll()
    GroupChats.deleteAll()
    PrivateChatDeletions.deleteAll()
    PrivateChats.deleteAll()
    MessageStatuses.deleteAll()
    Messages.deleteAll()
}