package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.Pic
import java.lang.ClassLoader.getSystemClassLoader

/** Reads the [filename] from the filesystem. */
fun readPic(filename: String): Pic = Pic.build(filename, readBytes(filename))

/** Reads the [filename]'s bytes from the filesystem. */
fun readBytes(filename: String): ByteArray =
    getSystemClassLoader().getResourceAsStream(filename).use { it!!.readAllBytes() }
