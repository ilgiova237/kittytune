package com.alananasss.kittytune.ui.library

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class LibraryCollectionsTest {
    @Test fun indexedCountsMatchScansIncludingDuplicatesAndRoot() {
        val random = Random(120)
        repeat(100) {
            val parents = List(1000) { if(random.nextBoolean()) null else random.nextLong(-2,30) }
            val index = countLibraryChildren(parents.asSequence())
            for(id in -2L..30L) assertEquals(parents.count { it == id }, index[id] ?: 0)
            assertEquals(parents.count { it != null }, index.values.sum())
        }
    }
    @Test fun reconciliationPreservesExactOrderDuplicatesAndEmptyCases() {
        val random = Random(32)
        val target = mutableListOf<Int>()
        repeat(300) {
            val next = List(random.nextInt(100)) { random.nextInt(8) }
            reconcileLibraryList(target, next)
            assertEquals(next, target)
        }
        reconcileLibraryList(target, emptyList()); assertTrue(target.isEmpty())
    }
    @Test fun unchangedItemsAreNotReplacedAndSingleChangeIsOneWrite() {
        val first = String(charArrayOf('a')); val second = String(charArrayOf('b'))
        class RecordingList: ArrayList<String>() {
            var writes = 0
            override fun set(index:Int, element:String):String { writes++; return super.set(index,element) }
        }
        val target = RecordingList().apply { add(first); add(second) }
        reconcileLibraryList(target, listOf(String(charArrayOf('a')), String(charArrayOf('b'))))
        assertEquals(0,target.writes); assertSame(first,target[0]); assertSame(second,target[1])
        reconcileLibraryList(target,listOf("a","c"));assertEquals(1,target.writes)
    }
}
