package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.buildNewGroupChat
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun GroupChats.count(): Long = transaction { selectAll().count() }

fun GroupChats.create(adminId: Int): Int = create(adminId, buildNewGroupChat())