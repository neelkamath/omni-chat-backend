package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.GroupChats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

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
            readUserByUsername(username).username shouldBe username
        }
    }

    context("searchUsers(String)") {
        /** Creates users, and returns their IDs. */
        fun createUsers(): List<String> = listOf(
            NewAccount(Username("tony"), Password("p"), emailAddress = "tony@example.com", firstName = "Tony"),
            NewAccount(Username("johndoe"), Password("p"), emailAddress = "john@example.com", firstName = "John"),
            NewAccount(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            NewAccount(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", firstName = "John")
        ).map {
            createUser(it)
            readUserByUsername(it.username).id
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
                NewAccount(Username("tony_stark"), Password("p"), emailAddress = "e"),
                NewAccount(Username("username"), Password("p"), "tony@example.com", firstName = "Tony")
            ).map {
                createUser(it)
                readUserByUsername(it.username).id
            }
            searchUsers("tony").map { it.id } shouldBe userIdList
        }
    }

    context("updateUser(String, UpdatedAccount)") {
        test("Updating an account should trigger a notification for the contact owner, but not the contact") {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, setOf(contactId))
            val (ownerSubscriber, contactSubscriber) = listOf(ownerId, contactId).map {
                contactsBroker.subscribe(ContactsAsset(it)).subscribeWith(TestSubscriber())
            }
            updateUser(contactId, AccountUpdate())
            ownerSubscriber.assertValue(UpdatedContact.fromUserId(contactId))
            contactSubscriber.assertNoValues()
        }

        test(
            """
            Given user 1 in group chat 1, and user 2 in group chats 1 and 2,
            when user 1 updates their account,
            then user 1 should be notified, and user 2 should be notified only in group chat 1 
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chat1Id = GroupChats.create(adminId = user1Id, chat = buildNewGroupChat(user2Id))
            val chat2Id = GroupChats.create(adminId = user2Id, chat = buildNewGroupChat())
            val (user1Subscriber, user2Chat1Subscriber, user2Chat2Subscriber) =
                listOf(user1Id to chat1Id, user2Id to chat1Id, user2Id to chat2Id).map { (userId, chatId) ->
                    groupChatInfoBroker.subscribe(GroupChatInfoAsset(chatId, userId)).subscribeWith(TestSubscriber())
                }
            updateUser(user1Id, AccountUpdate())
            val update = UpdatedAccount.fromUserId(user1Id)
            user1Subscriber.assertValue(update)
            user2Chat1Subscriber.assertValue(update)
            user2Chat2Subscriber.assertNoValues()
        }

        test("Updating an account should notify the subscriber of the update but not the user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val (user1Subscriber, user2Subscriber) = mapOf(user1Id to user2Id, user2Id to user1Id)
                .map { (subscriberId, userId) ->
                    privateChatInfoBroker
                        .subscribe(PrivateChatInfoAsset(subscriberId, userId))
                        .subscribeWith(TestSubscriber())
                }
            updateUser(user2Id, AccountUpdate())
            user1Subscriber.assertValue(UpdatedAccount.fromUserId(user2Id))
            user2Subscriber.assertNoValues()
        }

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