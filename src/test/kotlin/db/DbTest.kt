package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class DbTest : FunSpec({
    context("deleteUserFromDb(String)") {
        test("An exception should be thrown when the admin of a nonempty group chat deletes their data") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(userId))
            shouldThrowExactly<IllegalArgumentException> { deleteUserFromDb(adminId) }
        }

        test("The deleted user should be unsubscribed via the new group chats broker") {
            val userId = createVerifiedUsers(1)[0].info.id
            val subscriber = newGroupChatsBroker.subscribe(NewGroupChatsAsset(userId)).subscribeWith(TestSubscriber())
            deleteUserFromDb(userId)
            subscriber.assertComplete()
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

        test("The deleted user should be unsubscribed from contact updates") {
            val userId = createVerifiedUsers(1)[0].info.id
            val subscriber =
                contactsBroker.subscribe(ContactsAsset(userId)).subscribeWith(TestSubscriber())
            deleteUserFromDb(userId)
            subscriber.assertComplete()
        }

        test("The deleted subscriber should be unsubscribed from private chat info updates") {
            val userId = createVerifiedUsers(1)[0].info.id
            val subscriber =
                privateChatInfoBroker.subscribe(PrivateChatInfoAsset(userId)).subscribeWith(TestSubscriber())
            deleteUserFromDb(userId)
            subscriber.assertComplete()
        }

        test("Only the deleted subscriber should be unsubscribed from group chat info updates") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { groupChatInfoBroker.subscribe(GroupChatInfoAsset(it)).subscribeWith(TestSubscriber()) }
            deleteUserFromDb(userId)
            adminSubscriber.assertValue(ExitedUser(chatId, userId))
            userSubscriber.assertComplete()
        }

        test("The user should be unsubscribed from message updates") {
            val userId = createVerifiedUsers(1)[0].info.id
            val subscriber = messagesBroker.subscribe(MessagesAsset(userId)).subscribeWith(TestSubscriber())
            deleteUserFromDb(userId)
            subscriber.assertComplete()
        }
    }

    context("AccountsConnection.build(List<AccountEdge>, ForwardPagination?)") {
        /** Creates [count] users. */
        fun createAccountEdges(count: Int = 3): List<AccountEdge> =
            createVerifiedUsers(count).zip(Users.read()).map { (user, cursor) -> AccountEdge(user.info, cursor) }

        test("Every user should be retrieved if neither cursor nor limit get supplied") {
            val edges = createAccountEdges()
            AccountsConnection.build(edges).edges shouldBe edges
        }

        test("Using a deleted user's cursor should cause pagination to work as if the user still exists") {
            val edges = createAccountEdges(10)
            val index = 5
            val deletedUser = edges[index]
            deleteUser(deletedUser.node.id)
            val first = 3
            AccountsConnection.build(edges, ForwardPagination(first, deletedUser.cursor)).edges shouldBe
                    edges.subList(index + 1, index + 1 + first)
        }

        test(
            """
            Given multiple users,
            when retrieving only the first user,
            then the page's info should state that there are users after, but none before, the first user
            """
        ) {
            AccountsConnection.build(createAccountEdges(), ForwardPagination(first = 1)).pageInfo.run {
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
            AccountsConnection.build(edges, ForwardPagination(after = edges[1].cursor)).pageInfo.run {
                hasNextPage.shouldBeFalse()
                hasPreviousPage.shouldBeTrue()
            }
        }

        test("The start and end cursors should be null if there are no users") {
            AccountsConnection.build(AccountEdges = listOf()).pageInfo.run {
                startCursor.shouldBeNull()
                endCursor.shouldBeNull()
            }
        }

        test("The start and end cursors should be the same if there's only one user") {
            AccountsConnection.build(createAccountEdges(1)).pageInfo.run { startCursor shouldBe endCursor }
        }

        test("The first and last cursors should be the first and last users respectively") {
            val edges = createAccountEdges()
            AccountsConnection.build(edges).pageInfo.run {
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
            AccountsConnection.build(edges, ForwardPagination(first, edges[index].cursor)).edges shouldBe
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
            AccountsConnection.build(edges, ForwardPagination(first)).edges shouldBe edges.subList(0, first)
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
            AccountsConnection.build(edges, ForwardPagination(after = edges[index].cursor)).edges shouldBe
                    edges.drop(index + 1)
        }

        test("Supplying unsorted rows shouldn't affect pagination") {
            val edges = createVerifiedUsers(10)
                .zip(Users.read())
                .map { (user, cursor) -> AccountEdge(user.info, cursor) }
            val first = 3
            val index = 5
            AccountsConnection.build(edges.shuffled(), ForwardPagination(first, edges[index].cursor)).edges shouldBe
                    edges.subList(index + 1, index + 1 + first)
        }
    }
})