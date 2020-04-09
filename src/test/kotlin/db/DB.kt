package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.DB
import org.jetbrains.exposed.sql.SchemaUtils

fun DB.tearDown(): Unit = transact { SchemaUtils.drop(*tables) }