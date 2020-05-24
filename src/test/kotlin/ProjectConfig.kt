package com.neelkamath.omniChat.test

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.test.db.deleteDb
import com.neelkamath.omniChat.test.db.tearDownDb
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType
import io.mockk.unmockkAll

object ProjectConfig : AbstractProjectConfig() {
    override fun listeners(): List<Listener> = listOf(AppListener)

    override fun beforeAll() {
        setUpDb()
        mockEmails()
        configureObjectMapper()
    }

    /** Updates the [objectMapper] to provide the extra functionality the test source set needs. */
    private fun configureObjectMapper() {
        val module = SimpleModule().addDeserializer(Chat::class.java, ChatDeserializer)
        objectMapper.registerModule(module)
    }

    override fun afterAll() {
        tearDownDb()
        unmockkAll()
    }
}

private object ChatDeserializer : JsonDeserializer<Chat>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Chat {
        val node = parser.codec.readTree<JsonNode>(parser)
        return if (node.has("users"))
            parser.codec.treeToValue(node, GroupChat::class.java)
        else
            parser.codec.treeToValue(node, PrivateChat::class.java)
    }
}

private object AppListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.type == TestType.Test) setUpAuth()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Test) {
            deleteDb()
            tearDownAuth()
        }
    }
}