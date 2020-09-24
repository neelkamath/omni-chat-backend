package com.neelkamath.omniChat.db.tables

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Mp4Test {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the video is too big`() {
            assertFailsWith<IllegalArgumentException> { Mp4(ByteArray(Mp4.MAX_BYTES + 1)) }
        }
    }
}
