package com.neelkamath.omnichat

import org.jetbrains.exposed.sql.Database

/** Opens the DB connection, and creates the tables. */
fun initDb() {
    Database.connect(
        "jdbc:postgresql://${System.getenv("POSTGRES_URL")}/${System.getenv("POSTGRES_DB")}",
        "org.postgresql.Driver",
        System.getenv("POSTGRES_USER"),
        System.getenv("POSTGRES_PASSWORD")
    )
}