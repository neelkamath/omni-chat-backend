package com.neelkamath.omniChat.test.graphql.api

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
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
class EncodingTest : FunSpec({
    listener(AppListener())

    test("A group chat's title should allow emoji") {
        val token = createVerifiedUsers(1)[0].accessToken
        val title = "Title \uD83D\uDCDA"
        val chatId = createGroupChat(NewGroupChat(title), token)
        GroupChats.read(chatId).title shouldBe title
    }

    fun testMessage(message: String) {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        createMessage(chatId, message, token)
        Messages.read(chatId)[0].text shouldBe message
    }

    test("A message should allow emoji") { testMessage("message \uD83D\uDCDA") }

    test("A message should allow using multiple languages") { testMessage("Japanese: 日 Chinese: 传/傳 Kannada: ಘ") }
})