package com.alananasss.kittytune.data

import com.alananasss.kittytune.domain.Track
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class StoredLikesTest {
    @Test fun cleanLibraryReusesListAndNeedsNoRewrite() {
        val tracks=listOf(Track(artworkUrl=null,durationMs=1000,user=null,id=1,title="one",likedAt=5L),Track(artworkUrl=null,durationMs=1000,user=null,id=-2,title="two",likedAt=7L,source="vk"))
        val result=prepareStoredLikes(tracks,emptySet(),1000L)
        assertSame(tracks,result.tracks);assertFalse(result.changed)
    }
    @Test fun migrationAndBlacklistMatchPreviousBehaviorExactly() {
        val random=Random(93)
        repeat(100) {
            val tracks=List(200) { index -> Track(artworkUrl=null,durationMs=1000,user=null,id=index.toLong()-100,title="track $index",likedAt=when(index%4){0->null;1->0L;else->index.toLong()}) }
            val blacklist=tracks.filter {random.nextBoolean()}.map{it.id}.toSet()
            val expected=tracks.mapIndexed {index,track -> if(track.likedAt==null||track.likedAt==0L) track.copy(likedAt=1000000L-index*1000) else track }.filter{it.id !in blacklist}
            val actual=prepareStoredLikes(tracks,blacklist,1000000L)
            assertEquals(expected,actual.tracks)
            assertEquals(expected!=tracks,actual.changed)
        }
    }
    @Test fun emptyAndAllBlacklistedLibrariesRemainEmpty() {
        assertFalse(prepareStoredLikes(emptyList(),emptySet(),0).changed)
        val result=prepareStoredLikes(listOf(Track(artworkUrl=null,durationMs=1000,user=null,id=1,title="x",likedAt=10L)),setOf(1L),100)
        assertTrue(result.changed); assertTrue(result.tracks.isEmpty())
    }
}
