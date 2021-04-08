package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.readPic
import org.junit.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PicTest {
    @Nested
    inner class Init {
        @Test
        fun `Passing an excessively large original image must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> { Pic.build(Pic.Type.PNG, ByteArray(Pic.ORIGINAL_MAX_BYTES + 1)) }
        }

        @Test
        fun `Passing an excessively large thumbnail must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> {
                Pic(Pic.Type.PNG, ByteArray(1), ByteArray(Pic.THUMBNAIL_MAX_BYTES + 1))
            }
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class Type_Companion_build {
        @Test
        fun `Using a valid capitalized file extension mustn't fail`() {
            Pic.Type.build("PNG")
        }

        @Test
        fun `Using an invalid file extension must fail`() {
            assertFailsWith<IllegalArgumentException> { Pic.Type.build("html") }
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class Companion_build {
        private fun testSupportedPic(jpeg: String) {
            val pic = Pic.build(jpeg, readPic("76px×57px.jpg").original)
            assertEquals(Pic.Type.JPEG, pic.type)
        }

        private fun testUnsupportedPic(name: String) {
            assertFailsWith<IllegalArgumentException> { Pic.build(name, ByteArray(1)) }
        }

        @Test
        fun `Passing a supported extension must work`(): Unit = testSupportedPic("jpg")

        @Test
        fun `Passing an unsupported extension mustn't work`(): Unit = testUnsupportedPic("webp")

        @Test
        fun `Passing a supported filename must work`(): Unit = testSupportedPic("pic.jpg")

        @Test
        fun `Passing an unsupported filename mustn't work`(): Unit = testUnsupportedPic("pic.gif")
    }
}

class MediaTest {
    @Nested
    inner class CreateThumbnail {
        @Test
        fun `A thumbnail for a pic larger than 100px by 100px must be at most 100px by 100px`(): Unit =
            with(readPic("1008px×756px.jpg")) { assertNotEquals(original, thumbnail) }

        @Test
        fun `A thumbnail for a pic which is smaller than 100px by 100px must be the same as the pic`(): Unit =
            with(readPic("76px×57px.jpg")) { assertEquals(thumbnail, original) }

        @Test
        fun `A thumbnail must be built for a JPEG image`() {
            readPic("76px×57px.jpg")
        }

        @Test
        fun `A thumbnail must be built for a PNG image`() {
            readPic("76px×57px.png")
        }
    }
}
