package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun PrivateChats.count(): Long = transact { selectAll().count() }