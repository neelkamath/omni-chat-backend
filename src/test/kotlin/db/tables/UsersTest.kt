package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.accountsNotifier
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.onlineStatusesNotifier
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.readPic
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class UsersTest {
    @Nested
    inner class SetOnlineStatus {
        @Test
        fun `Updating the user's online status to the current value mustn't cause notifications to be sent`() {
            runBlocking {
                val (contactOwnerId, contactId) = createVerifiedUsers(2).map { it.info.id }
                val subscriber = onlineStatusesNotifier.subscribe(contactOwnerId).subscribeWith(TestSubscriber())
                Users.setOnlineStatus(contactId, Users.read(contactId).isOnline)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }

        @Test
        fun `Updating the user's status must only notify users who have them in their contacts or chats`(): Unit =
            runBlocking {
                val (updaterId, contactOwnerId, privateChatSharerId, userId) = createVerifiedUsers(4).map { it.info.id }
                Contacts.create(contactOwnerId, setOf(updaterId))
                PrivateChats.create(privateChatSharerId, updaterId)
                val (updaterSubscriber, contactOwnerSubscriber, privateChatSharerSubscriber, userSubscriber) =
                    listOf(updaterId, contactOwnerId, privateChatSharerId, userId)
                        .map { onlineStatusesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                val status = Users.read(updaterId).isOnline.not()
                Users.setOnlineStatus(updaterId, status)
                awaitBrokering()
                listOf(updaterSubscriber, userSubscriber).forEach { it.assertNoValues() }
                listOf(contactOwnerSubscriber, privateChatSharerSubscriber).forEach {
                    val lastOnline = Users.read(updaterId).lastOnline
                    it.assertValue(OnlineStatus(updaterId, status, lastOnline))
                }
            }
    }

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
            val user = Users.read(account.username)
            assertTrue(Users.verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode))
        }
    }

    @Nested
    inner class Search {
        /** Creates users, and returns their IDs. */
        private fun createUsers(): Set<Int> = setOf(
            AccountInput(Username("tony"), Password("p"), emailAddress = "tony@example.com", Name("Tony")),
            AccountInput(Username("johndoe"), Password("p"), emailAddress = "john@example.com", Name("John")),
            AccountInput(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            AccountInput(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", Name("John")),
        ).map {
            Users.create(it)
            Users.read(it.username).id
        }.toSet()

        @Test
        fun `Users must be searched case-insensitively`() {
            val infoList = createUsers()
            val search = { query: String, userIdList: Set<Int> ->
                assertEquals(userIdList, Users.search(query).edges.map { it.cursor }.toSet())
            }
            search("tOnY", setOf(infoList.first()))
            search("doe", setOf(infoList.elementAt(1)))
            search("john", setOf(infoList.elementAt(1), infoList.elementAt(2), infoList.elementAt(3)))
        }

        @Test
        fun `Searching users mustn't include duplicate results`() {
            val userIdList = listOf(
                AccountInput(Username("tony_stark"), Password("p"), emailAddress = "e"),
                AccountInput(Username("username"), Password("p"), "tony@example.com", Name("Tony")),
            ).map {
                Users.create(it)
                Users.read(it.username).id
            }
            assertEquals(userIdList, Users.search("tony").edges.map { it.cursor })
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `Updating an account must update only the specified fields`() {
            val user = createVerifiedUsers(1).first()
            val update = AccountUpdate(Username("updated"), firstName = Name("updated"))
            Users.update(user.info.id, update)
            with(Users.read(user.info.id)) {
                assertEquals(update.username, username)
                assertEquals(user.info.emailAddress, emailAddress)
                assertEquals(update.firstName, firstName)
                assertEquals(user.info.lastName, lastName)
            }
        }
    }

    @Nested
    inner class ResetPassword {
        @Test
        fun `The password must be reset if the password reset code is correct`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            val password = Password("new")
            assertTrue(Users.resetPassword(user.emailAddress, user.passwordResetCode, password))
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
    inner class UpdateEmailAddress {
        private fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val (userId, _, emailAddress) = createVerifiedUsers(1).first().info
            val address = if (changeAddress) "updated address" else emailAddress
            Users.update(userId, AccountUpdate(emailAddress = address))
            assertNotEquals(changeAddress, Users.read(userId).hasVerifiedEmailAddress)
        }

        @Test
        fun `Updating an account's email address must cause it to become unverified`() {
            assertEmailAddressUpdate(changeAddress = true)
        }

        @Test
        fun `Updating an account's email address to the same address mustn't cause it to become unverified`() {
            assertEmailAddressUpdate(changeAddress = false)
        }
    }

    @Nested
    inner class IsValidLogin {
        @Test
        fun `A nonexistent username must be an invalid login`() {
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
    inner class UpdatePic {
        @Test
        fun `Updating the pic must notify subscribers`(): Unit = runBlocking {
            val userId = createVerifiedUsers(1).first().info.id
            val subscriber = accountsNotifier.subscribe(userId).subscribeWith(TestSubscriber())
            Users.updatePic(userId, readPic("76px×57px.jpg"))
            awaitBrokering()
            subscriber.assertValue(UpdatedProfilePic(userId))
        }
    }
}
