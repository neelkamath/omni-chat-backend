package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.DB
import com.neelkamath.omniChat.db.PrivateChats
import org.jetbrains.exposed.sql.selectAll

data class PrivateChat(val creatorUserId: String, val invitedUserId: String)

/** Returns the private chats of every user. */
fun PrivateChats.read(): List<PrivateChat> = DB.transact {
    selectAll().map { PrivateChat(it[creatorUserId], it[invitedUserId]) }
}