package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.OnlineStatus
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedProfileImage
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.readImage
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

/** Returns the ID of every user in ascending order. */
fun Users.read(): LinkedHashSet<Int> = transaction {
    selectAll().orderBy(Users.id).map { it[Users.id].value }.toLinkedHashSet()
}

fun Users.readEmailAddress(username: Username): String = readEmailAddress(readId(username))

fun Users.readEmailAddressVerificationCode(username: Username): Int = readEmailAddressVerificationCode(readId(username))

@ExtendWith(DbExtension::class)
class UsersTest {
    @Nested
    inner class VerifyEmailAddress {
        @Test
        fun `Using an invalid code mustn't set the account's email address verification status to verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            assertFalse(Users.verifyEmailAddress(account.emailAddress, 123))
        }

        @Test
        fun `Using a valid email address verification code must cause the account's email address to get verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            val emailAddress = Users.readEmailAddress(account.username)
            val code = Users.readEmailAddressVerificationCode(account.username)
            assertTrue(Users.verifyEmailAddress(emailAddress, code))
        }
    }

    @Nested
    inner class IsValidLogin {
        @Test
        fun `A non-existing username must be an invalid login`() {
            val login = Login(Username("username"), Password("p"))
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `An incorrect password mustn't be a valid login`() {
            val username = createVerifiedUsers(1).first().login.username
            val login = Login(username, Password("incorrect"))
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `A valid login must be stated as such`() {
            val login = createVerifiedUsers(1).first().login
            assertTrue(Users.isValidLogin(login))
        }
    }

    @Nested
    inner class SetOnlineStatus {
        @Test
        fun `Changing the online status must notify unauthenticated users`(): Unit = runBlocking {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val subscriber =
                chatOnlineStatusesNotifier.subscribe(ChatId(chatId)).flowable.subscribeWith(TestSubscriber())
            Users.setOnlineStatus(adminId, isOnline = true)
            awaitBrokering()
            val actual = subscriber.values().map { (it as OnlineStatus).getUserId() }
            assertEquals(listOf(adminId), actual)
        }

        @Test
        fun `Updating the user's online status to the current value mustn't cause notifications to be sent`() {
            runBlocking {
                val (contactOwnerId, contactId) = createVerifiedUsers(2).map { it.userId }
                val subscriber =
                    onlineStatusesNotifier.subscribe(UserId(contactOwnerId)).flowable.subscribeWith(TestSubscriber())
                Users.setOnlineStatus(contactId, Users.isOnline(contactId))
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }

        @Test
        fun `Updating the user's status must only notify (authenticated) users who have them in their contacts or chats`(): Unit =
            runBlocking {
                val (updaterId, contactOwnerId, privateChatSharerId, userId) = createVerifiedUsers(4).map { it.userId }
                Contacts.create(contactOwnerId, updaterId)
                PrivateChats.create(privateChatSharerId, updaterId)
                val (updaterSubscriber, contactOwnerSubscriber, privateChatSharerSubscriber, userSubscriber) =
                    setOf(updaterId, contactOwnerId, privateChatSharerId, userId)
                        .map { onlineStatusesNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
                val status = !Users.isOnline(updaterId)
                Users.setOnlineStatus(updaterId, status)
                awaitBrokering()
                setOf(updaterSubscriber, userSubscriber).forEach { it.assertNoValues() }
                setOf(contactOwnerSubscriber, privateChatSharerSubscriber).forEach { subscriber ->
                    val actual = subscriber.values().map { (it as OnlineStatus).getUserId() }
                    assertEquals(listOf(updaterId), actual)
                }
            }
    }

    @Nested
    inner class ResetPassword {
        @Test
        fun `The password must be reset if the password reset code is correct`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val password = Password("new")
            val isReset = Users.resetPassword(
                Users.readEmailAddress(account.username),
                Users.readPasswordResetCode(account.emailAddress),
                password,
            )
            assertTrue(isReset)
            val login = Login(account.username, password)
            assertTrue(Users.isValidLogin(login))
        }

        @Test
        fun `The password mustn't be reset if the password reset code is incorrect`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val password = Password("new")
            assertFalse(Users.resetPassword(account.emailAddress, 123, password))
            val login = Login(account.username, password)
            assertFalse(Users.isValidLogin(login))
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `Updating an account must update only the specified fields`() {
            val user = createVerifiedUsers(1).first()
            val userId = Users.readId(user.username)
            val update = AccountUpdate(Username("updated"), firstName = Name("updated"))
            Users.update(user.userId, update)
            assertEquals(update.username, Users.readUsername(userId))
            assertEquals(user.emailAddress, Users.readEmailAddress(userId))
            assertEquals(update.firstName, Users.readFirstName(userId))
            assertEquals(user.lastName, Users.readLastName(userId))
        }
    }

    @Nested
    inner class UpdateEmailAddress {
        private fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val (userId, _, emailAddress) = createVerifiedUsers(1).first()
            val address = if (changeAddress) "updated address" else emailAddress
            Users.update(userId, AccountUpdate(emailAddress = address))
            assertNotEquals(changeAddress, Users.hasVerifiedEmailAddress(userId))
        }

        @Test
        fun `Updating an account's email address must cause it to become unverified`(): Unit =
            assertEmailAddressUpdate(changeAddress = true)

        @Test
        fun `Updating an account's email address to the same address mustn't cause it to become unverified`(): Unit =
            assertEmailAddressUpdate(changeAddress = false)
    }

    @Nested
    inner class UpdateImage {
        @Test
        fun `Updating the image must notify subscribers`(): Unit = runBlocking {
            val userId = createVerifiedUsers(1).first().userId
            val subscriber = accountsNotifier.subscribe(UserId(userId)).flowable.subscribeWith(TestSubscriber())
            Users.updateImage(userId, readImage("76px×57px.jpg"))
            awaitBrokering()
            val actual = subscriber.values().map { (it as UpdatedProfileImage).getUserId() }
            assertEquals(listOf(userId), actual)
        }
    }

    @Nested
    inner class BuildQuery {
        /** Creates users, and returns their IDs. */
        private fun createUsers(): Set<Int> = setOf(
            AccountInput(Username("tony"), Password("p"), emailAddress = "tony@example.com", Name("Tony")),
            AccountInput(Username("johndoe"), Password("p"), emailAddress = "john@example.com", Name("John")),
            AccountInput(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            AccountInput(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", Name("John")),
        )
            .map {
                Users.create(it)
                Users.readId(it.username)
            }
            .toSet()

        @Test
        fun `Users must be searched case-insensitively`() {
            val infoList = createUsers()
            val search = { query: String, userIdList: Set<Int> -> assertEquals(userIdList, Users.searchAll(query)) }
            search("tOnY", setOf(infoList.first()))
            search("doe", setOf(infoList.elementAt(1)))
            search("john", setOf(infoList.elementAt(1), infoList.elementAt(2), infoList.elementAt(3)))
        }

        @Test
        fun `Searching users mustn't include duplicate results`() {
            val userIdList = listOf(
                AccountInput(Username("tony_stark"), Password("p"), emailAddress = "e"),
                AccountInput(Username("username"), Password("p"), "tony@example.com", Name("Tony")),
            )
                .map {
                    Users.create(it)
                    Users.readId(it.username)
                }
                .toLinkedHashSet()
            assertEquals(userIdList, Users.searchAll("tony"))
        }
    }
}
