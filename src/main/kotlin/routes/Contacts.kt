package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Contacts
import com.neelkamath.omniChat.DB
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select

fun Route.routeContacts() {
    post("contacts") {
        val saved = DB.dbTransaction {
            DB.Contacts.select { DB.Contacts.contactOwner eq call.userId }.map { it[DB.Contacts.contact] }
        }
        val contacts = call.receive<Contacts>().contacts.filter { it !in saved && it != call.userId }
        if (!contacts.all { it in Auth.getUserIdList() }) call.respond(HttpStatusCode.BadRequest)
        else {
            DB.dbTransaction {
                DB.Contacts.batchInsert(contacts) {
                    this[DB.Contacts.contactOwner] = call.userId
                    this[DB.Contacts.contact] = it
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}