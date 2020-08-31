@file:Suppress("RedundantInnerClassModifier")

package com.neelkamath.omniChat

import com.neelkamath.omniChat.graphql.routing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class AuthTest {
    @Nested
    inner class IsValidLogin {
        @Test
        fun `An incorrect login should be invalid`() {
            val login = Login(Username("username"), Password("password"))
            assertFalse(isValidLogin(login))
        }

        @Test
        fun `A correct login should be valid`() {
            val login = createVerifiedUsers(1)[0].login
            assertTrue(isValidLogin(login))
        }
    }

    @Nested
    inner class IsUsernameTaken {
        @Test
        fun `A username similar to an existing username shouldn't be said to exist`() {
            val username = createVerifiedUsers(1)[0].info.username
            val similarUsername = Username(username.value.dropLast(1))
            assertFalse(isUsernameTaken(similarUsername))
        }

        @Test
        fun `An existing username should be said to exist`() {
            val username = createVerifiedUsers(1)[0].info.username
            assertTrue(isUsernameTaken(username))
        }
    }

    @Nested
    inner class EmailAddressExists {
        @Test
        fun `A nonexistent email address should not be said to exist`() {
            assertFalse(emailAddressExists("address"))
        }

        @Test
        fun `An existing email address should be said to exist`() {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            assertTrue(emailAddressExists(address))
        }
    }

    @Nested
    inner class ReadUserByUsername {
        @Test
        fun `Finding a user by their username should yield that user`() {
            val username = createVerifiedUsers(1)[0].info.username
            assertEquals(username, readUserByUsername(username).username)
        }
    }

    @Nested
    inner class SearchUsers {
        /** Creates users, and returns their IDs. */
        private fun createUsers(): List<Int> = listOf(
            AccountInput(Username("tony"), Password("p"), emailAddress = "tony@example.com", firstName = "Tony"),
            AccountInput(Username("johndoe"), Password("p"), emailAddress = "john@example.com", firstName = "John"),
            AccountInput(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            AccountInput(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", firstName = "John")
        ).map {
            createUser(it)
            readUserByUsername(it.username).id
        }

        @Test
        fun `Users should be searched case-insensitively`() {
            val infoList = createUsers()
            val search = { query: String, userIdList: List<Int> ->
                assertEquals(userIdList, searchUsers(query).map { it.id })
            }
            search("tOnY", listOf(infoList[0]))
            search("doe", listOf(infoList[1]))
            search("john", listOf(infoList[1], infoList[2], infoList[3]))
        }

        @Test
        fun `Searching users shouldn't include duplicate results`() {
            val userIdList = listOf(
                AccountInput(Username("tony_stark"), Password("p"), emailAddress = "e"),
                AccountInput(Username("username"), Password("p"), "tony@example.com", firstName = "Tony")
            ).map {
                createUser(it)
                readUserByUsername(it.username).id
            }
            assertEquals(userIdList, searchUsers("tony").map { it.id })
        }
    }

    @Nested
    inner class UpdateUser {
        @Test
        fun `Updating an account should update only the specified fields`() {
            val user = createVerifiedUsers(1)[0]
            val update = AccountUpdate(Username("updated username"), firstName = "updated first name")
            updateUser(user.info.id, update)
            with(readUserById(user.info.id)) {
                assertEquals(update.username, username)
                assertEquals(user.info.emailAddress, emailAddress)
                assertEquals(update.firstName, firstName)
                assertEquals(user.info.lastName, lastName)
            }
        }

        private fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val (userId, _, emailAddress) = createVerifiedUsers(1)[0].info
            val address = if (changeAddress) "updated address" else emailAddress
            updateUser(userId, AccountUpdate(emailAddress = address))
            assertNotEquals(changeAddress, isEmailVerified(userId))
        }

        @Test
        fun `Updating an account's email address should cause it to become unverified`() {
            assertEmailAddressUpdate(changeAddress = true)
        }

        @Test
        fun `Updating an account's email address to the same address shouldn't cause it to become unverified`() {
            assertEmailAddressUpdate(changeAddress = false)
        }
    }
}
