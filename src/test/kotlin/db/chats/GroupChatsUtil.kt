package com.neelkamath.omniChat.db.chats

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Long = transact { selectAll().count() }