package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Every user's cursor (ID) in their order of creation. */
fun Users.read(): List<Int> = transaction {
    // Use <sorted()> because <selectAll()> occasionally returns unordered rows.
    selectAll().map { it[Users.id].value }.sorted()
}