package com.neelkamath.omniChat

import org.jetbrains.exposed.sql.SchemaUtils

fun DB.tearDown(): Unit = dbTransaction { SchemaUtils.drop(*tables) }