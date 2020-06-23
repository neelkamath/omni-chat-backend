package com.neelkamath.omniChat.graphql.api.subscriptions

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType
import kotlinx.coroutines.channels.ReceiveChannel
import java.time.Duration
import kotlin.system.measureTimeMillis

class PingTest : FunSpec({
    /** Returns once the [channel] sends a [FrameType.CLOSE]. */
    suspend fun awaitClose(channel: ReceiveChannel<Frame>) {
        for (frame in channel)
            if (frame is Frame.Close) return
    }

    /**
     * Asserts that the [timeInMillis] is within the documented ping period and timeout. A leeway is included to prevent
     * flaky tests.
     */
    fun testPingPeriod(timeInMillis: Long) {
        val time = Duration.ofMillis(timeInMillis)
        val pingPeriod = Duration.ofMinutes(1)
        val timeout = Duration.ofSeconds(15)
        val connectionDuration = pingPeriod + timeout
        val leeway = Duration.ofSeconds(5)
        time shouldBeGreaterThan connectionDuration - leeway
        time shouldBeLessThan connectionDuration + leeway
    }

    test("The connection should be closed if the client doesn't ping within the ping period") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        receiveMessageUpdates(token, chatId) { incoming, _ ->
            val time = measureTimeMillis { awaitClose(incoming) }
            testPingPeriod(time)
        }
    }
})