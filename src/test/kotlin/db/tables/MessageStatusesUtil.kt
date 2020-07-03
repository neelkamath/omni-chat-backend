package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

/** Returns the number of saved statuses. */
fun MessageStatuses.count(): Long = transact { selectAll().count() }