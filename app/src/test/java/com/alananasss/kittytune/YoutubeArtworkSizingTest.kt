package com.alananasss.kittytune

import com.alananasss.kittytune.ui.youtubeArtworkUpgrades
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeArtworkSizingTest {
    @Test fun ytMusicCoverUsesTheAvailable1080PixelArtwork() {
        // Representative public CDN URL shape; no track-specific metadata needed.
        val cover = "https://yt3.googleusercontent.com/example-cover"
        assertEquals(listOf("$cover=w1080-h1080-l90-rj"),
            youtubeArtworkUpgrades("$cover=w120-h120-l90-rj", 640))
    }

    @Test fun ytCdnSmallImagesAndLookalikeHostsAreHandledSafely() {
        assertEquals(listOf("https://yt3.ggpht.com/cover=s216-c"),
            youtubeArtworkUpgrades("https://yt3.ggpht.com/cover=s60-c", 216))
        assertEquals(emptyList<String>(),
            youtubeArtworkUpgrades("https://yt3.googleusercontent.com.example.com/cover=s120", 640))
        assertEquals(emptyList<String>(),
            youtubeArtworkUpgrades("https://yt3.googleusercontent.com/cover=s1080", 640))
    }

    @Test fun smallGgphtArtworkIsNotStretchedAtHighDensity() {
        assertEquals(listOf("https://lh3.ggpht.com/cover=w216-h216-l90-rj"),
            youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=w60-h60-l90-rj", 216))
    }

    @Test fun squareAndRectangularMusicUrlsKeepTheirOptions() {
        assertEquals(listOf("https://lh3.googleusercontent.com/cover=s144-c?x=1"),
            youtubeArtworkUpgrades("https://lh3.googleusercontent.com/cover=s60-c?x=1", 144))
        assertEquals(listOf("https://lh3.ggpht.com/cover=w144-h108-l90#fragment"),
            youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=w60-h45-l90#fragment", 144))
    }

    @Test fun sufficientlySizedSmallImagesNeedNoExtraRequest() {
        for (url in listOf("https://lh3.ggpht.com/cover=s300-c", "https://lh3.googleusercontent.com/cover=w300-h300")) {
            assertEquals(emptyList<String>(), youtubeArtworkUpgrades(url, 144))
        }
    }

    @Test fun largeCoverPolicyIsUnchanged() {
        assertEquals(listOf("https://lh3.ggpht.com/cover=s1080-c"),
            youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=s60-c", 640))
        assertEquals(emptyList<String>(), youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=s1600", 640))
    }

    @Test fun invalidSizesAndUnrelatedUrlsAreUntouched() {
        for (size in listOf(-1, 0, 144)) {
            assertEquals(emptyList<String>(), youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=s0", size))
            assertEquals(emptyList<String>(), youtubeArtworkUpgrades("https://example.com/cover=s60", size))
        }
        assertEquals(emptyList<String>(), youtubeArtworkUpgrades("https://lh3.ggpht.com/cover=s60", 0))
    }

    @Test fun videoThumbnailPolicyIsUnchanged() {
        val url = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        assertEquals(emptyList<String>(), youtubeArtworkUpgrades(url, 144))
        assertEquals(listOf("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hq720.jpg"),
            youtubeArtworkUpgrades(url, 640))
    }
}
