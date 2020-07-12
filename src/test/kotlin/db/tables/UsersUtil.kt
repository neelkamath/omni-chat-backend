package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll
import java.lang.ClassLoader.getSystemClassLoader

fun Users.count(): Long = transact { selectAll().count() }

fun Users.setProfilePic(userId: String, fileName: String = "31kB.png"): Unit =
    updateProfilePic(userId, getSystemClassLoader().getResource(fileName)!!.readBytes())

/** @return every user's cursor in their order of creation. */
fun Users.read(): List<Int> = transact {
    selectAll().map { it[Users.id].value }
}