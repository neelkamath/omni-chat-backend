package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Audio
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AudioTest {
    @Nested
    inner class Init {
        @Test
        fun `An excessively large audio file must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> { Audio(ByteArray(Audio.MAX_BYTES + 1)) }
        }
    }

    @Suppress("ClassName")
    @Nested
    inner class Companion_isValidExtension {
        @Test
        fun `Extensions must be matched case-insensitively`(): Unit = assertTrue(Audio.isValidExtension("mP3"))
    }
}
