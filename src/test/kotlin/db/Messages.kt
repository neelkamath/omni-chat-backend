package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.Messages
import org.jetbrains.exposed.sql.selectAll

/** Returns the number of messages in every chat. */
fun Messages.count(): Int = Db.transact { selectAll().toList().size }