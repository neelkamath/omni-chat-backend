package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.DocFile
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DocMessagesTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the doc is too big`() {
            assertFailsWith<IllegalArgumentException> { DocFile("doc.pdf", ByteArray(DocFile.MAX_BYTES + 1)) }
        }
    }
}
