package com.neelkamath.omniChat

import org.jetbrains.exposed.sql.Database

object DB {
    /**
     * Opens the DB connection, and creates the tables.
     *
     * This must be run before any DB-related activities are performed. This takes a small, but noticeable amount of time.
     */
    fun setUp() {
        Database.connect(
            "jdbc:postgresql://${System.getenv("POSTGRES_URL")}/${System.getenv("POSTGRES_DB")}",
            "org.postgresql.Driver",
            System.getenv("POSTGRES_USER"),
            System.getenv("POSTGRES_PASSWORD")
        )
    }
}