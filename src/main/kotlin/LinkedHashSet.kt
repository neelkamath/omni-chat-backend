package com.neelkamath.omniChatBackend

fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> = toCollection(LinkedHashSet())
