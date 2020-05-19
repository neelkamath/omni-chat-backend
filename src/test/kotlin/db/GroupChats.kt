package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Long = transact { selectAll().count() }