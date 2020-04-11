package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DB {
    val tables = arrayOf(ContactsData.Table)

    /**
     * Opens the DB connection, and creates the tables.
     *
     * This must be run before any DB-related activities are performed. This takes a small, but noticeable amount of
     * time.
     */
    fun setUp() {
        connectDb()
        createTables()
    }

    private fun connectDb() {
        val url = System.getenv("POSTGRES_URL")
        val db = System.getenv("POSTGRES_DB")
        Database.connect(
            "jdbc:postgresql://$url/$db?reWriteBatchedInserts=true",
            "org.postgresql.Driver",
            System.getenv("POSTGRES_USER"),
            System.getenv("POSTGRES_PASSWORD")
        )
    }

    private fun createTables(): Unit = transact { SchemaUtils.create(*tables) }

    /** Always use this in place of [transaction]. */
    fun <T> transact(function: () -> T): T = transaction {
        addLogger(StdOutSqlLogger)
        return@transaction function()
    }
}