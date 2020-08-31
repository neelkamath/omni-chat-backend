package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Every JWT ID in order of creation. */
fun OnetimeTokens.read(): List<Int> = transaction {
    selectAll().map { it[OnetimeTokens.id].value }
}