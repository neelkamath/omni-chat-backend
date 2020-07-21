package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Every user's cursor in their order of creation. */
fun Users.read(): List<Int> = transaction {
    selectAll().map { it[Users.id].value }
}