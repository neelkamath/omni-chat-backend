package com.neelkamath.omniChat.db.tables

import java.lang.ClassLoader.getSystemClassLoader

fun Pic.Companion.build(fileName: String): Pic =
    getSystemClassLoader().getResource(fileName)!!.run { build(readBytes(), file.substringAfterLast(".")) }