package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Audio
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AudioTest {
    @Nested
    inner class Init {
        @Test
        fun `An excessively large audio file must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> { Audio(ByteArray(Audio.MAX_BYTES + 1), Audio.Type.MP3) }
        }
    }
}
