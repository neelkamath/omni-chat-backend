package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ContactsData
import com.neelkamath.omniChat.test.db.readContacts
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

private data class CreatedUser(val login: Login, val userId: String)

class DeleteContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be deleted ignoring invalid ones" {
        val users = createUsers()
        val userIdList = setOf(users[0].userId, users[1].userId)
        val jwt = getJwt(users[2].login)
        Server.createContacts(Contacts(userIdList), jwt)
        Server.deleteContacts(Contacts(userIdList + "invalid-user-id"), jwt).status() shouldBe HttpStatusCode.NoContent
        readContacts().userIdList.shouldBeEmpty()
    }

    "Deleting a user should delete it from everyone's contacts" {
        val users = createUsers()
        val uploadedContacts = Contacts(setOf(users[1].userId, users[2].userId))
        val jwt = getJwt(users[0].login)
        Server.createContacts(uploadedContacts, jwt)
        Server.deleteAccount(getJwt(users[1].login))
        readContacts().userIdList shouldContainExactly setOf(users[2].userId)
    }
})

class GetContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be read" {
        val users = createUsers()
        val contacts = Contacts(setOf(users[1].userId, users[2].userId))
        val jwt = getJwt(users[0].login)
        Server.createContacts(contacts, jwt)
        val response = Server.readContacts(jwt)
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, Contacts::class.java) shouldBe contacts
    }
})

class PostContactsTest : StringSpec({
    listener(AppListener())

    "Saving previously saved contacts should be ignored" {
        val users = createUsers()
        val jwt = getJwt(users[0].login)
        val contacts = Contacts(setOf(users[1].userId, users[2].userId))
        Server.createContacts(contacts, jwt)
        Server.createContacts(contacts, jwt)
        readContacts().userIdList shouldContainExactly contacts.userIdList
    }

    "Trying to save the user's own contact should be ignored" {
        val users = createUsers()
        val contacts = Contacts(setOf(users[0].userId, users[1].userId))
        val jwt = getJwt(users[0].login)
        val response = Server.createContacts(contacts, jwt)
        response.status() shouldBe HttpStatusCode.NoContent
        ContactsData.read(users[0].userId).userIdList shouldContainExactly setOf(users[1].userId)
    }

    "If one of the contacts to be saved is incorrect, then none of them should be saved" {
        val users = createUsers()
        Auth.deleteUser(users[0].userId)
        val contacts = Contacts(setOf(users[0].userId, users[2].userId))
        val jwt = getJwt(users[1].login)
        Server.createContacts(contacts, jwt).status() shouldBe HttpStatusCode.BadRequest
        readContacts().userIdList.shouldBeEmpty()
    }
})

/** Creates 3 users, verifies their emails, and returns them. */
private fun createUsers(): List<CreatedUser> = (0..2).map {
    val login = Login("username$it", "password")
    Server.createAccount(User(login, "username$it@gmail.com"))
    Auth.verifyEmail(login.username!!)
    val userId = Auth.findUserByUsername(login.username!!).id
    CreatedUser(login, userId)
}