package com.neelkamath.omniChat.test

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.PrivateChat
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.db.tearDownDb
import com.neelkamath.omniChat.test.db.wipeDb
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType

object ProjectConfig : AbstractProjectConfig() {
    override fun listeners(): List<Listener> = listOf(AppListener)

    override fun beforeAll() {
        configureObjectMapper()
        setUpAuthForTests()
        setUpDb()
    }

    override fun afterAll() {
        tearDownDb()
        tearDownAuth()
    }
}

/** Updates the [objectMapper] to provide the extra functionality the test source set needs. */
private fun configureObjectMapper() {
    val module = SimpleModule().addDeserializer(Chat::class.java, ChatDeserializer)
    objectMapper.registerModule(module)
}

private object ChatDeserializer : JsonDeserializer<Chat>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Chat {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = if (node.has("users")) GroupChat::class else PrivateChat::class
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object AppListener : TestListener {
    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Test) {
            wipeDb()
            wipeAuth()
        }
    }
}