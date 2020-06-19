package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Long = transact { selectAll().count() }