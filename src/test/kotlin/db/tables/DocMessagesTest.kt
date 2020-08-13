package com.neelkamath.omniChat.db.tables

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class DocMessagesTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the doc is too big") {
            shouldThrowExactly<IllegalArgumentException> { Doc(ByteArray(Doc.MAX_BYTES + 1)) }
        }
    }
})