package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.PrivateChatDeletions
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun PrivateChatDeletions.count(): Long = transact { selectAll().count() }