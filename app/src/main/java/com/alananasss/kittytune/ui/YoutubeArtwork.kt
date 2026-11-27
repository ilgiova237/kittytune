package com.alananasss.kittytune.ui

private const val MIN_LARGE_ARTWORK_PIXELS = 240
private const val YOUTUBE_MUSIC_COVER_SIZE = 1080
private val MUSIC_IMAGE_SIZE = Regex(
    """^(https://(?:lh|yt)\d+\.(?:googleusercontent\.com|ggpht\.com)/[^?#=]+)=(?:w(\d+)-h(\d+)|s(\d+))(?=[-?#]|$)"""
)
private val VIDEO_THUMBNAIL = Regex(
    """^(https://i\.ytimg\.com/vi(?:_webp)?/[A-Za-z0-9_-]{11}/)(?:default|mqdefault|hqdefault|sddefault)\.(jpg|webp)(?:[?#].*)?$"""
)

/** Avoid stretching undersized music thumbnails; retain HD alternatives for large covers. */
internal fun youtubeArtworkUpgrades(url: String, displayPixels: Int): List<String> {
    if (displayPixels <= 0) return emptyList()

    MUSIC_IMAGE_SIZE.find(url)?.let { match ->
        // Music CDNs may return only 60px artwork, including on ggpht.com where the
        // generic thumbnail mapper leaves the URL unchanged. Small views need enough
        // physical pixels too; they should not fetch a 1080px cover just to stay sharp.
        val target = if (displayPixels >= MIN_LARGE_ARTWORK_PIXELS) YOUTUBE_MUSIC_COVER_SIZE else displayPixels
        val size = match.groupValues[4].toIntOrNull()
        val resized = if (size != null) {
            if (size !in 1 until target) return emptyList()
            "s$target"
        } else {
            val width = match.groupValues[2].toIntOrNull() ?: return emptyList()
            val height = match.groupValues[3].toIntOrNull() ?: return emptyList()
            val longest = maxOf(width, height)
            if (minOf(width, height) <= 0 || longest >= target) return emptyList()
            val newWidth = (width * target / longest).coerceAtLeast(1)
            val newHeight = (height * target / longest).coerceAtLeast(1)
            "w$newWidth-h$newHeight"
        }
        return listOf(match.groupValues[1] + "=" + resized + url.substring(match.range.last + 1))
    }

    if (displayPixels < MIN_LARGE_ARTWORK_PIXELS) return emptyList()
    val (prefix, format) = VIDEO_THUMBNAIL.matchEntire(url)?.destructured ?: return emptyList()
    return listOf(prefix + "maxresdefault.$format", prefix + "hq720.$format")
}
