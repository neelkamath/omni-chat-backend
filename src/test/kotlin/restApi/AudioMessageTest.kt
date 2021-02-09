package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.create
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

fun getAudioMessage(accessToken: String? = null, messageId: Int): TestApplicationResponse =
    getFileMessage(accessToken, path = "audio-message", messageId)

fun postAudioMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString(), "context-message-id" to contextMessageId?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "audio-message", parameters)
}

@ExtendWith(DbExtension::class)
class AudioMessageTest {
    @Nested
    inner class RouteAudioMessage {
        @Test
        fun `Using a capitalized file extensions mustn't fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("audio.MP3", bytes = 1)
            assertEquals(HttpStatusCode.NoContent, postAudioMessage(admin.accessToken, dummy, chatId).status())
        }
    }
}
