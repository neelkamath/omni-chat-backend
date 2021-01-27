package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.Pic
import java.lang.ClassLoader.getSystemClassLoader

/** Reads the [filename] from the filesystem. */
fun readPic(filename: String): Pic = Pic.build(filename, readBytes(filename))

/** Reads the [filename]'s bytes from the filesystem. */
fun readBytes(filename: String): ByteArray =
    getSystemClassLoader().getResourceAsStream(filename).use { it!!.readAllBytes() }
