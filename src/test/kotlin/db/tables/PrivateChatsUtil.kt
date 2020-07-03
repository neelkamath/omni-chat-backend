package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun PrivateChats.count(): Long = transact { selectAll().count() }