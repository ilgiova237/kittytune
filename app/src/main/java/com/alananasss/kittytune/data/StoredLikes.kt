package com.alananasss.kittytune.data

import com.alananasss.kittytune.domain.Track

internal data class StoredLikes(val tracks: List<Track>, val changed: Boolean)

/** Fast path keeps clean storage untouched; migration retains the original index-based timestamps. */
internal fun prepareStoredLikes(tracks: List<Track>, blacklist: Set<Long>, now: Long): StoredLikes {
    var changed: MutableList<Track>? = null
    for ((index, track) in tracks.withIndex()) {
        val blocked = track.id in blacklist
        val migrate = track.likedAt == null || track.likedAt == 0L
        if (changed == null && (blocked || migrate)) {
            changed = ArrayList(tracks.size)
            changed.addAll(tracks.subList(0, index))
        }
        if (!blocked && changed != null) changed.add(if (migrate) track.copy(likedAt = now - index * 1000) else track)
    }
    return StoredLikes(changed ?: tracks, changed != null)
}
