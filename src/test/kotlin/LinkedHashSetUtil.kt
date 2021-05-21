package com.neelkamath.omniChatBackend

/**
 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list,
 * and vice-versa.
 *
 * Structural changes in the base list make the behavior of the view undefined.
 */
fun <T> LinkedHashSet<T>.subList(fromIndex: Int, toIndex: Int): LinkedHashSet<T> =
    toList().subList(fromIndex, toIndex).toLinkedHashSet()

/** Returns a list containing elements at indices in the specified [indices] range. */
fun <T> LinkedHashSet<T>.slice(indices: IntRange): LinkedHashSet<T> = toList().slice(indices).toLinkedHashSet()

/**
 * Returns a list containing last n elements.
 *
 * @throws IllegalArgumentException - if n is negative.
 */
fun <T> LinkedHashSet<T>.takeLast(n: Int): LinkedHashSet<T> = toList().takeLast(n).toLinkedHashSet()

/**
 * Returns a list containing all elements except last n elements.
 *
 * @throws IllegalArgumentException - if n is negative.
 */
fun <T> LinkedHashSet<T>.dropLast(n: Int): LinkedHashSet<T> = toList().dropLast(n).toLinkedHashSet()
