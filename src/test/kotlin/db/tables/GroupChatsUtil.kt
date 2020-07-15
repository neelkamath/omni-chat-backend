package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Long = transact { selectAll().count() }

fun GroupChats.create(adminId: Int): Int = create(adminId, buildNewGroupChat())