package com.neelkamath.omniChat.db.tables

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class Mp4Test : FunSpec({
    context("init") {
        test("An exception should be thrown if the video is too big") {
            shouldThrowExactly<IllegalArgumentException> { Mp4(ByteArray(Mp4.MAX_BYTES + 1)) }
        }
    }
})