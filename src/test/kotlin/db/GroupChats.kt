package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.GroupChats
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Int = Db.transact { selectAll().toList().size }