package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.selectAll

fun PrivateChats.count(): Long = transact { selectAll().count() }