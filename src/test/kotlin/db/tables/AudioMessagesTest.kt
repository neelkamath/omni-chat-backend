package com.neelkamath.omniChat.db.tables

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class Mp3Test : FunSpec({
    context("init") {
        test("An excessively large audio file should cause an exception to be thrown") {
            shouldThrowExactly<IllegalArgumentException> { buildMp3("2MB.mp3") }
        }
    }
})