package com.neelkamath.omniChat

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.streams.asInput
import java.lang.ClassLoader.getSystemClassLoader
import java.net.URL

fun readImage(fileName: String): ByteArray = readResource(fileName).readBytes()

fun readInput(fileName: String): Input = readResource(fileName).openStream().asInput()

private fun readResource(fileName: String): URL = getSystemClassLoader().getResource(fileName)!!