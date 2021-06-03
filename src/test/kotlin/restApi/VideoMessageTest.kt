package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

private fun postVideoMessage(
    accessToken: String,
    chatId: Int,
    dummy: DummyFile,
    contextMessageId: Int? = null,
): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString(), "context-message-id" to contextMessageId?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "video-message", parameters)
}

@ExtendWith(DbExtension::class)
class VideoMessageTest {
    @Nested
    inner class RouteVideoMessage {
        @Test
        fun `Using a capitalized file extension mustn't fail`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val dummy = DummyFile("video.MP4", bytes = 1)
            assertEquals(HttpStatusCode.NoContent, postVideoMessage(admin.accessToken, chatId, dummy).status())
        }
    }
}
