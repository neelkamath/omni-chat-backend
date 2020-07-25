package com.neelkamath.omniChat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AuthTest : FunSpec({
    context("isValidLogin(Login)") {
        test("An incorrect login should be invalid") {
            val login = Login(Username("username"), Password("password"))
            isValidLogin(login).shouldBeFalse()
        }

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
            val similarUsername = Username(username.value.dropLast(1))
            isUsernameTaken(similarUsername).shouldBeFalse()
        }

        test("An existing username should be said to exist") {
            val username = createVerifiedUsers(1)[0].info.username
            isUsernameTaken(username).shouldBeTrue()
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

    context("readUserByUsername(Username)") {
        test("Finding a user by their username should yield that user") {
            val username = createVerifiedUsers(1)[0].info.username
            readUserByUsername(username).username shouldBe username
        }
    }

    context("searchUsers(String)") {
        /** Creates users, and returns their IDs. */
        fun createUsers(): List<Int> = listOf(
            AccountInput(Username("tony"), Password("p"), emailAddress = "tony@example.com", firstName = "Tony"),
            AccountInput(Username("johndoe"), Password("p"), emailAddress = "john@example.com", firstName = "John"),
            AccountInput(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            AccountInput(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", firstName = "John")
        ).map {
            createUser(it)
            readUserByUsername(it.username).id
        }

        test("Users should be searched case-insensitively") {
            val infoList = createUsers()
            val search = { query: String, userIdList: List<Int> ->
                searchUsers(query).map { it.id } shouldBe userIdList
            }
            search("tOnY", listOf(infoList[0]))
            search("doe", listOf(infoList[1]))
            search("john", listOf(infoList[1], infoList[2], infoList[3]))
        }

        test("Searching users shouldn't include duplicate results") {
            val userIdList = listOf(
                AccountInput(Username("tony_stark"), Password("p"), emailAddress = "e"),
                AccountInput(Username("username"), Password("p"), "tony@example.com", firstName = "Tony")
            ).map {
                createUser(it)
                readUserByUsername(it.username).id
            }
            searchUsers("tony").map { it.id } shouldBe userIdList
        }
    }

    context("updateUser(String, UpdatedAccount)") {
        test("Updating an account should update only the specified fields") {
            val user = createVerifiedUsers(1)[0]
            val update = AccountUpdate(Username("updated username"), firstName = "updated first name")
            updateUser(user.info.id, update)
            with(readUserById(user.info.id)) {
                username shouldBe update.username
                emailAddress shouldBe user.info.emailAddress
                firstName shouldBe update.firstName
                lastName shouldBe user.info.lastName
            }
        }

        fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val (userId, _, emailAddress) = createVerifiedUsers(1)[0].info
            val address = if (changeAddress) "updated address" else emailAddress
            updateUser(userId, AccountUpdate(emailAddress = address))
            isEmailVerified(userId) shouldNotBe changeAddress
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
