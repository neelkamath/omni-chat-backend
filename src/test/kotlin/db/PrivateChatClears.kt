package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.PrivateChatClears
import org.jetbrains.exposed.sql.selectAll

fun PrivateChatClears.count(): Int = Db.transact { selectAll().toList().size }