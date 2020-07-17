package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Chats.count(): Long = transaction { selectAll().count() }