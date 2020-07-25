package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.Pic
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.streams.asInput
import java.lang.ClassLoader.getSystemClassLoader
import java.net.URL

fun readPic(fileName: String): Pic =
    with(readResource(fileName)) { Pic.build(readBytes(), file.substringAfterLast(".")) }

fun readInput(fileName: String): Input = readResource(fileName).openStream().asInput()

private fun readResource(fileName: String): URL = getSystemClassLoader().getResource(fileName)!!