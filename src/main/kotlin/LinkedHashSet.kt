package com.neelkamath.omniChatBackend

fun <T> linkedHashSetOf(vararg elements: T): LinkedHashSet<T> = LinkedHashSet(elements.toSet())

fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> = toCollection(LinkedHashSet())
