package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.readBytes
import com.neelkamath.omniChatBackend.readImage
import org.junit.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ImageTest {
    @Nested
    inner class Init {
        @Test
        fun `Passing an excessively large original image must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> {
                ProcessedImage.build(
                    "png",
                    ByteArray(ProcessedImage.ORIGINAL_MAX_BYTES + 1)
                )
            }
        }

        @Test
        fun `Passing an excessively large thumbnail must cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> {
                ProcessedImage(
                    ByteArray(1),
                    ByteArray(ProcessedImage.THUMBNAIL_MAX_BYTES + 1)
                )
            }
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class Type_Companion_build {
        @Test
        fun `Using a valid capitalized file extension mustn't fail`() {
            ProcessedImage.Type.build("PNG")
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class Companion_build {
        @Test
        fun `Passing a supported extension must work`() {
            ProcessedImage.build("jpg", readImage("76px×57px.jpg").original)
        }

        @Test
        fun `Passing an unsupported extension mustn't work`() {
            assertFailsWith<IllegalArgumentException> { ProcessedImage.build("webp", readBytes("76px×57px.webp")) }
        }
    }
}

class MediaTest {
    @Nested
    inner class CreateThumbnail {
        @Test
        fun `A thumbnail for an image larger than 100px by 100px must be at most 100px by 100px`(): Unit =
            with(readImage("1008px×756px.jpg")) { assertNotEquals(original, thumbnail) }

        @Test
        fun `A thumbnail for an image which is smaller than 100px by 100px must be the same as the image`(): Unit =
            with(readImage("76px×57px.jpg")) { assertEquals(thumbnail, original) }

        @Test
        fun `A thumbnail must be built for a JPEG image`() {
            readImage("76px×57px.jpg")
        }

        @Test
        fun `A thumbnail must be built for a PNG image`() {
            readImage("76px×57px.png")
        }
    }
}
