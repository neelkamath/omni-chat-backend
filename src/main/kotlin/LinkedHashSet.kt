package com.neelkamath.omniChat

fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> = toCollection(LinkedHashSet())
