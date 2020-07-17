package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Returns the number of saved statuses. */
fun MessageStatuses.count(): Long = transaction { selectAll().count() }