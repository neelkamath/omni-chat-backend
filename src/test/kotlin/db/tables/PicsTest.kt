package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.readPic
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PicTest : FunSpec({
    context("init") {
        test("Passing an excessively large image should cause an exception to be thrown") {
            shouldThrowExactly<IllegalArgumentException> { readPic("734kB.png") }
        }
    }

    context("Companion.build()") {
        test("Building a pic from an unsupported file type should fail") {
            shouldThrowExactly<IllegalArgumentException> { readPic("39kB.webp") }
        }
    }
})

class PicsTest : FunSpec({
    context("update(Int?, Pic?)") {
        test("If an ID and pic are supplied, then the pic should be updated, and the ID should be returned") {
            val pic = readPic("31kB.png")
            val picId = Pics.create(pic)
            val newPic = readPic("36kB.png")
            Pics.update(picId, newPic) shouldBe picId
            Pics.read(picId) shouldBe newPic
        }

        test("If only an ID is supplied, then the pic should be deleted, and an ID shouldn't be returned") {
            val pic = readPic("31kB.png")
            val picId = Pics.create(pic)
            Pics.update(picId, pic = null).shouldBeNull()
            Pics.count().shouldBeZero()
        }

        test("If only a pic is supplied, then the pic should be created, and its ID should be returned") {
            val pic = readPic("31kB.png")
            val picId = Pics.update(id = null, pic = pic)!!
            Pics.read(picId) shouldBe pic
        }

        test("If neither ID nor pic were supplied, then no ID should be returned") {
            Pics.update(id = null, pic = null).shouldBeNull()
        }
    }
})