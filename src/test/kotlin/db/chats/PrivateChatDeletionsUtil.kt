package com.neelkamath.omniChat.db.chats

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun PrivateChatDeletions.count(): Long = transact { selectAll().count() }