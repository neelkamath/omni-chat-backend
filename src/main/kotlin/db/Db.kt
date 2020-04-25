package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object Db {
    val tables = arrayOf(Contacts, GroupChats, GroupChatUsers, PrivateChats, PrivateChatClears, PrivateMessages)

    /**
     * Opens the DB connection, and creates the tables.
     *
     * This must be run before any DB-related activities are performed. This takes a small, but noticeable amount of
     * time.
     */
    fun setUp() {
        connect()
        create()
    }

    private fun connect() {
        val url = System.getenv("POSTGRES_URL")
        val db = System.getenv("POSTGRES_DB")
        Database.connect(
            "jdbc:postgresql://$url/$db?reWriteBatchedInserts=true",
            "org.postgresql.Driver",
            System.getenv("POSTGRES_USER"),
            System.getenv("POSTGRES_PASSWORD")
        )
    }

    private fun create(): Unit = transact { SchemaUtils.create(*tables) }

    /** Always use this instead of [transaction]. */
    fun <T> transact(function: () -> T): T = transaction {
        addLogger(StdOutSqlLogger)
        return@transaction function()
    }

    /** Deletes the [userId]'s data (it is assumed that the [userId] is not the admin of a group chat). */
    fun deleteUserData(userId: String) {
        Contacts.deleteUserEntries(userId)
        for (chatId in GroupChats.read(userId).map { it.id }) {
            GroupChatUsers.removeUsers(chatId, setOf(userId))
            if (GroupChats.read(chatId).userIdList.isEmpty()) GroupChats.delete(chatId)
        }
        PrivateChats.read(userId).map { it.id }.forEach(PrivateChatClears::delete)
        PrivateChats.delete(userId)
    }
}