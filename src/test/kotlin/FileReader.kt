package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.ProcessedImage
import java.lang.ClassLoader.getSystemClassLoader

/** Reads the [filename] from the filesystem. */
fun readImage(filename: String): ProcessedImage =
    ProcessedImage.build(filename, readBytes(filename))

/** Reads the [filename]'s bytes from the filesystem. */
fun readBytes(filename: String): ByteArray =
    getSystemClassLoader().getResourceAsStream(filename).use { it!!.readAllBytes() }
