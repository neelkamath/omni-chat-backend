package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.selectAll

/** Returns the number of saved statuses. */
fun MessageStatuses.count(): Long = transact { selectAll().count() }