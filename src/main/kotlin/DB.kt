package com.neelkamath.omniChat

import org.jetbrains.exposed.sql.Database

/**
 * Opens the DB connection, and creates the tables.
 *
 * This must be run before any DB-related activities are performed. This takes a small, but noticeable amount of time.
 */
fun initDb() {
    Database.connect(
        "jdbc:postgresql://${System.getenv("POSTGRES_URL")}/${System.getenv("POSTGRES_DB")}",
        "org.postgresql.Driver",
        System.getenv("POSTGRES_USER"),
        System.getenv("POSTGRES_PASSWORD")
    )
}