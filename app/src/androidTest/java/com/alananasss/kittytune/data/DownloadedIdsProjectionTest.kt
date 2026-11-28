package com.alananasss.kittytune.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alananasss.kittytune.data.local.AppDatabase
import com.alananasss.kittytune.data.local.LocalTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadedIdsProjectionTest {
    @Test fun projectedIdsMatchFullRowsAndStayLiveAcrossUpdatesAndDeletes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        val dao = db.downloadDao()
        val emitted = Channel<Set<Long>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            dao.getDownloadedTrackIds().collect { emitted.send(it.toSet()) }
        }
        suspend fun expect(expected: Set<Long>) {
            withTimeout(10000) { while (emitted.receive() != expected) { } }
            assertEquals(dao.getAllTracksList().map { it.id }.toSet(), expected)
        }
        fun row(id:Long, path:String) = LocalTrack(id,"title","artist","https://example.invalid/cover",1000,path,"",source="vk",secretToken="metadata")
        try {
            expect(emptySet())
            val audio=row(1,"/audio/one"); val metadata=row(2,""); val negative=row(-3," ")
            dao.insertTrack(audio); dao.insertTrack(metadata); dao.insertTrack(negative)
            expect(setOf(1L,-3L))
            dao.updateTrack(metadata.copy(localAudioPath="/audio/two")); expect(setOf(1L,2L,-3L))
            dao.updateTrack(audio.copy(title="renamed")); expect(setOf(1L,2L,-3L))
            dao.updateTrack(audio.copy(localAudioPath="")); expect(setOf(2L,-3L))
            dao.deleteTrack(-3); expect(setOf(2L))
            db.clearAllTables(); expect(emptySet())
            dao.insertTrack(audio); expect(setOf(1L))
        } finally { observer.cancelAndJoin(); emitted.close(); db.close() }
    }
}
