package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class AuthTest : FunSpec({
    listener(AuthListener())

    context("isValidLogin(Login)") {
        test("An incorrect login should be invalid") { isValidLogin(Login("username", "password")).shouldBeFalse() }

        test("A correct login should be valid") {
            val login = createVerifiedUsers(1)[0].login
            isValidLogin(login).shouldBeTrue()
        }
    }

    context("isUsernameTaken(String)") {
        test(
            """
            Given an existing username, and a nonexistent username similar to the one which exists,
            when checking if the nonexistent username exists,
            then it should be said to not exist
            """
        ) {
            val username = createVerifiedUsers(1)[0].info.username
            isUsernameTaken(username.dropLast(1)).shouldBeFalse()
        }

        test("An existing username should be said to exist") {
            val username = createVerifiedUsers(1)[0].info.username
            isUsernameTaken(username).shouldBeTrue()
        }
    }

    context("userIdExists(String)") {
        test("A nonexistent user ID should not be said to exist") { userIdExists("user ID").shouldBeFalse() }

        test("An existing user ID should be said to exist") {
            val id = createVerifiedUsers(1)[0].info.id
            userIdExists(id).shouldBeTrue()
        }
    }

    context("emailAddressExists(String)") {
        test("A nonexistent email address should not be said to exist") {
            emailAddressExists("address").shouldBeFalse()
        }

        test("An existing email address should be said to exist") {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            emailAddressExists(address).shouldBeTrue()
        }
    }

    context("findUserByUsername(String)") {
        test("Finding a user by their username should yield that user") {
            val username = createVerifiedUsers(1)[0].info.username
            findUserByUsername(username).username shouldBe username
        }
    }

    context("searchUsers(String)") {
        /** Creates users, and returns their IDs. */
        fun createUsers(): List<String> = listOf(
            NewAccount(username = "tony", password = "p", emailAddress = "tony@example.com", firstName = "Tony"),
            NewAccount(username = "johndoe", password = "p", emailAddress = "john@example.com", firstName = "John"),
            NewAccount(username = "john.rogers", password = "p", emailAddress = "rogers@example.com"),
            NewAccount(username = "anonymous", password = "p", emailAddress = "anon@example.com", firstName = "John")
        ).map {
            createAccount(it)
            findUserByUsername(it.username).id
        }

        test("Users should be searched case-insensitively") {
            val infoList = createUsers()
            val search = { query: String ->
                searchUsers(query).map { it.id }
            }
            search("tOnY") shouldBe listOf(infoList[0])
            search("doe") shouldBe listOf(infoList[1])
            search("john") shouldBe listOf(infoList[1], infoList[2], infoList[3])
        }

        test("Searching users shouldn't include duplicate results") {
            val userIdList = listOf(
                NewAccount(username = "tony_stark", password = "p", emailAddress = "e"),
                NewAccount("username", "password", "tony@example.com", firstName = "Tony")
            ).map {
                createAccount(it)
                findUserByUsername(it.username).id
            }
            searchUsers("tony").map { it.id } shouldBe userIdList
        }
    }

    context("updateUser(String, AccountUpdate)") {
        test("Updating an account should update only the specified fields") {
            val user = createVerifiedUsers(1)[0]
            val update = AccountUpdate("updated username", firstName = "updated first name")
            updateUser(user.info.id, update)
            with(findUserById(user.info.id)) {
                username shouldBe update.username
                email shouldBe user.info.emailAddress
                firstName shouldBe update.firstName
                lastName shouldBe user.info.lastName
            }
        }

        fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val user = createVerifiedUsers(1)[0]
            val address = if (changeAddress) "updated address" else user.info.emailAddress
            updateUser(user.info.id, AccountUpdate(emailAddress = address))
            findUserById(user.info.id).isEmailVerified shouldBe !changeAddress
        }

        test(
            """
            Given an account with a verified email address,
            when its email address is changed,
            then its email address should become unverified
            """
        ) { assertEmailAddressUpdate(changeAddress = true) }

        test(
            """
            Given an account with a verified email address,
            when its email address is updated to the same address,
            then its email address shouldn't become unverified
            """
        ) { assertEmailAddressUpdate(changeAddress = false) }
    }
})