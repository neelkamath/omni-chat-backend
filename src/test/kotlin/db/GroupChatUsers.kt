package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

/** Returns the number of users in every chat. */
fun GroupChatUsers.count(): Long = transact { selectAll().count() }