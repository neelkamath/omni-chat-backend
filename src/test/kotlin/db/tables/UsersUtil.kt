package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun Users.count(): Long = transact { selectAll().count() }

/** @return every user's cursor in their order of creation. */
fun Users.read(): List<Int> = transact {
    selectAll().map { it[Users.id].value }
}