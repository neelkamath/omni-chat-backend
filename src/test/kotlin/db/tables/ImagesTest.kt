package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.readImage
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(DbExtension::class)
class ImagesTest {
    @Nested
    inner class Update {
        @Test
        fun `If an ID and image are supplied, then the image must be updated, and the ID must be returned`() {
            val imageId = Images.create(readImage("76px×57px.jpg"))
            val newImage = readImage("76px×57px.png")
            assertEquals(imageId, Images.update(imageId, newImage))
            val actual = Images.read(imageId, ImageType.THUMBNAIL).bytes
            assertContentEquals(newImage.thumbnail, actual)
        }

        @Test
        fun `If only an ID is supplied, then the image must be deleted, and an ID mustn't be returned`() {
            val imageId = Images.create(readImage("76px×57px.jpg"))
            assertNull(Images.update(imageId, image = null))
            assertEquals(0, Images.count())
        }

        @Test
        fun `If only a image is supplied, then the image must be created, and its ID must be returned`() {
            val image = readImage("76px×57px.jpg")
            val imageId = Images.update(imageId = null, image)!!
            val actual = Images.read(imageId, ImageType.THUMBNAIL).bytes
            assertContentEquals(image.thumbnail, actual)
        }

        @Test
        fun `If neither ID nor image were supplied, then no ID must be returned`(): Unit =
            assertNull(Images.update(imageId = null, image = null))
    }
}
