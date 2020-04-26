package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.GroupChatUsers
import org.jetbrains.exposed.sql.selectAll

/** Returns the number of users in every chat. */
fun GroupChatUsers.count(): Int = Db.transact { selectAll().toList().size }