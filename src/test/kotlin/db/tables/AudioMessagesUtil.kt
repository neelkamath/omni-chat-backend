package com.neelkamath.omniChat.db.tables

import java.lang.ClassLoader.getSystemClassLoader

fun buildMp3(fileName: String): Mp3 = getSystemClassLoader().getResource(fileName)!!.readBytes().let(::Mp3)