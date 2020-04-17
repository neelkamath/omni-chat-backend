package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import org.jetbrains.exposed.sql.SchemaUtils

fun Db.tearDown(): Unit = transact { SchemaUtils.drop(*tables) }