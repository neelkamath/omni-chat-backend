package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.count
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
        fun `If an ID and pic are supplied, then the pic should be updated, and the ID should be returned`() {
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val picId = Pics.create(pic)
            val newPic = Pic(ByteArray(1), Pic.Type.JPEG)
            assertEquals(picId, Pics.update(picId, newPic))
            assertEquals(newPic, Pics.read(picId))
        }

        @Test
        fun `If only an ID is supplied, then the pic should be deleted, and an ID shouldn't be returned`() {
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val picId = Pics.create(pic)
            assertNull(Pics.update(picId, pic = null))
            assertEquals(0, Pics.count())
        }

        @Test
        fun `If only a pic is supplied, then the pic should be created, and its ID should be returned`() {
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val picId = Pics.update(id = null, pic = pic)!!
            assertEquals(pic, Pics.read(picId))
        }

        @Test
        fun `If neither ID nor pic were supplied, then no ID should be returned`() {
            assertNull(Pics.update(id = null, pic = null))
        }
    }
}