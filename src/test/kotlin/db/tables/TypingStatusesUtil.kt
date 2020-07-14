package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun TypingStatuses.count(): Long = transact { selectAll().count() }