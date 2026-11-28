package com.alananasss.kittytune.ui.library

/** Counts children once, preserving duplicate entries. Null denotes the root, not a folder. */
internal fun countLibraryChildren(parentIds: Sequence<Long?>): Map<Long, Int> {
    val counts = HashMap<Long, Int>()
    for (id in parentIds) if (id != null) counts[id] = (counts[id] ?: 0) + 1
    return counts
}

/** Keep equal elements in place; callers publish snapshot-list changes atomically. */
internal fun <T> reconcileLibraryList(target: MutableList<T>, next: List<T>) {
    val shared = minOf(target.size, next.size)
    for (index in 0 until shared) if (target[index] != next[index]) target[index] = next[index]
    if (target.size > next.size) target.subList(next.size, target.size).clear()
    else if (next.size > shared) target.addAll(next.subList(shared, next.size))
}
