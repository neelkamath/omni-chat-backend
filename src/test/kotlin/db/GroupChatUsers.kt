package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.DB
import com.neelkamath.omniChat.db.GroupChatUsers
import org.jetbrains.exposed.sql.select

/** Returns the user ID list from the specified [groupChatId]. */
fun GroupChatUsers.read(groupChatId: Int): Set<String> = DB.transact {
    select { groupChat eq groupChatId }.map { it[userId] }.toSet()
}