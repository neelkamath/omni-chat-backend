package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.selectAll

fun PrivateChatDeletions.count(): Long = transact { selectAll().count() }