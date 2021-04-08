package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.readPic
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(DbExtension::class)
class PicsTest {
    @Nested
    inner class Update {
        @Test
        fun `If an ID and pic are supplied, then the pic must be updated, and the ID must be returned`() {
            val picId = Pics.create(readPic("76px×57px.jpg"))
            val newPic = readPic("76px×57px.png")
            assertEquals(picId, Pics.update(picId, newPic))
            assertEquals(newPic, Pics.read(picId))
        }

        @Test
        fun `If only an ID is supplied, then the pic must be deleted, and an ID mustn't be returned`() {
            val picId = Pics.create(readPic("76px×57px.jpg"))
            assertNull(Pics.update(picId, pic = null))
            assertEquals(0, Pics.count())
        }

        @Test
        fun `If only a pic is supplied, then the pic must be created, and its ID must be returned`() {
            val pic = readPic("76px×57px.jpg")
            val picId = Pics.update(id = null, pic)!!
            assertEquals(pic, Pics.read(picId))
        }

        @Test
        fun `If neither ID nor pic were supplied, then no ID must be returned`(): Unit =
            assertNull(Pics.update(id = null, pic = null))
    }
}
