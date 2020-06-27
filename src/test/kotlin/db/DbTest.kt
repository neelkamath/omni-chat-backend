package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.AccountEdge
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.chats.GroupChats
import com.neelkamath.omniChat.db.chats.PrivateChatDeletions
import com.neelkamath.omniChat.db.chats.PrivateChats
import com.neelkamath.omniChat.db.chats.count
import com.neelkamath.omniChat.deleteUser
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class DbTest : FunSpec({
    context("deleteUserFromDb(String)") {
        test("An exception should be thrown when the admin of a nonempty group chat deletes their data") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            GroupChats.create(adminId, chat)
            shouldThrowExactly<IllegalArgumentException> { deleteUserFromDb(adminId) }
        }

        test(
            "A private chat deleted by the user should be deleted for the other user when the user deletes their data"
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            deleteUserFromDb(user1Id)
            PrivateChats.count().shouldBeZero()
        }
    }

    context("buildAccountsConnection(List<AccountEdge>, ForwardPagination?)") {
        /** Creates [count] users. */
        fun createAccountEdges(count: Int = 3): List<AccountEdge> =
            createVerifiedUsers(count).zip(Users.read()).map { (user, cursor) -> AccountEdge(user.info, cursor) }

        test("Every user should be retrieved if neither cursor nor limit get supplied") {
            val edges = createAccountEdges()
            buildAccountsConnection(edges).edges shouldBe edges
        }

        test("Using a deleted user's cursor should cause pagination to work as if the user still exists") {
            val edges = createAccountEdges(10)
            val index = 5
            val deletedUser = edges[index]
            deleteUser(deletedUser.node.id)
            val first = 3
            buildAccountsConnection(edges, ForwardPagination(first, deletedUser.cursor)).edges shouldBe
                    edges.subList(index + 1, index + 1 + first)
        }

        test(
            """
            Given multiple users,
            when retrieving only the first user,
            then the page's info should state that there are users after, but none before, the first user
            """
        ) {
            buildAccountsConnection(createAccountEdges(), ForwardPagination(first = 1)).pageInfo.run {
                hasNextPage.shouldBeTrue()
                hasPreviousPage.shouldBeFalse()
            }
        }

        test(
            """
            Given multiple users,
            when retrieving only the last user,
            then the page's info should state that there are users before, but none after, the last user
            """
        ) {
            val edges = createAccountEdges()
            buildAccountsConnection(edges, ForwardPagination(after = edges[1].cursor)).pageInfo.run {
                hasNextPage.shouldBeFalse()
                hasPreviousPage.shouldBeTrue()
            }
        }

        test("The start and end cursors should be null if there are no users") {
            buildAccountsConnection(AccountEdges = listOf()).pageInfo.run {
                startCursor.shouldBeNull()
                endCursor.shouldBeNull()
            }
        }

        test("The start and end cursors should be the same if there's only one user") {
            buildAccountsConnection(createAccountEdges(1)).pageInfo.run { startCursor shouldBe endCursor }
        }

        test("The first and last cursors should be the first and last users respectively") {
            val edges = createAccountEdges()
            buildAccountsConnection(edges).pageInfo.run {
                startCursor shouldBe edges[0].cursor
                endCursor shouldBe edges.last().cursor
            }
        }

        test(
            """
            Given both a limit and cursor, 
            when retrieving users, 
            then the number of users specified by the limit should be returned from after the cursor
            """
        ) {
            val edges = createAccountEdges(10)
            val first = 3
            val index = 5
            buildAccountsConnection(edges, ForwardPagination(first, edges[index].cursor)).edges shouldBe
                    edges.subList(index + 1, index + 1 + first)
        }

        test(
            """
            Given a limit without a cursor, 
            when retrieving users, 
            then the number of users specified by the limit from the first user should be retrieved
            """
        ) {
            val edges = createAccountEdges(5)
            val first = 3
            buildAccountsConnection(edges, ForwardPagination(first)).edges shouldBe
                    edges.subList(0, first)
        }

        test(
            """
            Given a cursor without a limit, 
            when retrieving users, 
            then every user after the cursor should be retrieved
            """
        ) {
            val edges = createAccountEdges(10)
            val index = 5
            buildAccountsConnection(edges, ForwardPagination(after = edges[index].cursor)).edges shouldBe
                    edges.drop(index + 1)
        }

        test("Supplying unsorted rows shouldn't affect pagination") {
            val edges = createVerifiedUsers(10)
                .zip(Users.read())
                .map { (user, cursor) -> AccountEdge(user.info, cursor) }
            val first = 3
            val index = 5
            buildAccountsConnection(
                edges.shuffled(),
                ForwardPagination(first, edges[index].cursor)
            ).edges shouldBe edges.subList(index + 1, index + 1 + first)
        }
    }
})