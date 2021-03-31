package com.neelkamath.omniChat

fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> = toCollection(LinkedHashSet())

/** Returns a list containing elements at indices in the specified [indices] range. */
fun <T> LinkedHashSet<T>.slice(indices: IntRange): List<T> =
    if (indices.isEmpty()) listOf() else toList().subList(indices.first, indices.last + 1)
