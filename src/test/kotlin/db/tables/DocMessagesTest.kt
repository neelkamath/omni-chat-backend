package com.neelkamath.omniChat.db.tables

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DocMessagesTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the doc is too big`() {
            assertFailsWith<IllegalArgumentException> { Doc(ByteArray(Doc.MAX_BYTES + 1)) }
        }
    }
}
