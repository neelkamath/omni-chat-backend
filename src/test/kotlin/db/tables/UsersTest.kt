@file:Suppress("RedundantInnerClassModifier")

package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.OnlineStatusesAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.onlineStatusesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.*
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
        fun `Updating the user's online status to the current value shouldn't cause notifications to be sent`() {
            runBlocking {
                val (contactOwnerId, contactId) = createVerifiedUsers(2).map { it.info.id }
                val subscriber = onlineStatusesNotifier
                    .safelySubscribe(OnlineStatusesAsset(contactOwnerId)).subscribeWith(TestSubscriber())
                Users.setOnlineStatus(contactId, Users.read(contactId).isOnline)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }

        @Test
        fun `Updating the user's status should only notify users who have them in their contacts or chats`(): Unit =
            runBlocking {
                val (updaterId, contactOwnerId, privateChatSharerId, userId) = createVerifiedUsers(4).map { it.info.id }
                Contacts.create(contactOwnerId, setOf(updaterId))
                PrivateChats.create(privateChatSharerId, updaterId)
                val (updaterSubscriber, contactOwnerSubscriber, privateChatSharerSubscriber, userSubscriber) =
                    listOf(updaterId, contactOwnerId, privateChatSharerId, userId).map {
                        onlineStatusesNotifier.safelySubscribe(OnlineStatusesAsset(it)).subscribeWith(TestSubscriber())
                    }
                val status = Users.read(updaterId).isOnline.not()
                Users.setOnlineStatus(updaterId, status)
                awaitBrokering()
                listOf(updaterSubscriber, userSubscriber).forEach { it.assertNoValues() }
                listOf(contactOwnerSubscriber, privateChatSharerSubscriber)
                    .forEach { it.assertValue(UpdatedOnlineStatus(updaterId, status)) }
            }
    }

    @Nested
    inner class VerifyEmailAddress {
        @Test
        fun `Using an invalid code shouldn't set the account's email address verification status to verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            assertFalse(Users.verifyEmailAddress(account.emailAddress, 123))
        }

        @Test
        fun `Using a valid email address verification code should cause the account's email address to get verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            assertTrue(Users.verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode))
        }
    }

    @Nested
    inner class Search {
        /** Creates users, and returns their IDs. */
        private fun createUsers(): List<Int> = listOf(
            AccountInput(Username("tony"), Password("p"), emailAddress = "tony@example.com", Name("Tony")),
            AccountInput(Username("johndoe"), Password("p"), emailAddress = "john@example.com", Name("John")),
            AccountInput(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            AccountInput(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", Name("John"))
        ).map {
            Users.create(it)
            Users.read(it.username).id
        }

        @Test
        fun `Users should be searched case-insensitively`() {
            val infoList = createUsers()
            val search = { query: String, userIdList: List<Int> ->
                assertEquals(userIdList, Users.search(query).edges.map { it.cursor })
            }
            search("tOnY", listOf(infoList[0]))
            search("doe", listOf(infoList[1]))
            search("john", listOf(infoList[1], infoList[2], infoList[3]))
        }

        @Test
        fun `Searching users shouldn't include duplicate results`() {
            val userIdList = listOf(
                AccountInput(Username("tony_stark"), Password("p"), emailAddress = "e"),
                AccountInput(Username("username"), Password("p"), "tony@example.com", Name("Tony"))
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
        fun `Updating an account should update only the specified fields`() {
            val user = createVerifiedUsers(1)[0]
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
        fun `The password should be reset if the password reset code is correct`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            val password = Password("new")
            assertTrue(Users.resetPassword(user.emailAddress, user.passwordResetCode, password))
            val login = Login(account.username, password)
            assertTrue(Users.isValidLogin(login))
        }

        @Test
        fun `The password shouldn't be reset if the password reset code is incorrect`() {
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
            val (userId, _, emailAddress) = createVerifiedUsers(1)[0].info
            val address = if (changeAddress) "updated address" else emailAddress
            Users.update(userId, AccountUpdate(emailAddress = address))
            assertNotEquals(changeAddress, Users.read(userId).hasVerifiedEmailAddress)
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

    @Nested
    inner class IsValidLogin {
        @Test
        fun `A nonexistent username should be an invalid login`() {
            val login = Login(Username("username"), Password("p"))
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `An incorrect password shouldn't be a valid login`() {
            val username = createVerifiedUsers(1)[0].login.username
            val login = Login(username, Password("incorrect"))
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `A valid login should be stated as such`() {
            val login = createVerifiedUsers(1)[0].login
            assertTrue(Users.isValidLogin(login))
        }
    }
}