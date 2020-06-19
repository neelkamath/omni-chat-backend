package com.neelkamath.omniChat.test.graphql.api

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sanity tests for encoding.
 *
 * Don't write a test for every GraphQL operation saving text; a few are enough. Although encoding issues may seem to
 * not be a problem these days, https://github.com/graphql-java/graphql-java/issues/1877 shows otherwise. Had these
 * tests not existed, such an encoding problem may not have been found until technical debt from the tool at fault
 * had already accumulated.
 */
class EncodingTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A group chat's title should allow emoji") {
        val token = createSignedInUsers(1)[0].accessToken
        val title = "Title \uD83D\uDCDA"
        val chatId = createGroupChat(token, NewGroupChat(title))
        GroupChats.readChat(chatId).title shouldBe title
    }

    fun testMessage(message: String) {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        createMessage(token, chatId, message)
        Messages.readGroupChat(chatId)[0].node.text shouldBe message
    }

    test("A message should allow emoji") { testMessage("message \uD83D\uDCDA") }

    test("A message should allow using multiple languages") { testMessage("Japanese: 日 Chinese: 传/傳 Kannada: ಘ") }
}