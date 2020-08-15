package com.neelkamath.omniChat.db.tables

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Mp3Test {
    @Nested
    inner class Init {
        @Test
        fun `An excessively large audio file should cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> { Mp3(ByteArray(Mp3.MAX_BYTES + 1)) }
        }
    }
}