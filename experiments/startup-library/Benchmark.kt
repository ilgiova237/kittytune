package com.alananasss.kittytune.ui.library

import kotlin.system.measureNanoTime

fun main() {
    val parents = List(20000) { (it % 1000).toLong() }
    val folders = List(1000) { it.toLong() }
    var checksum = 0
    fun legacy(): List<Int> = folders.map { folder -> parents.count { it == folder } }
    fun indexed(): List<Int> { val counts=countLibraryChildren(parents.asSequence()); return folders.map { counts[it] ?: 0 } }
    check(legacy() == indexed())
    repeat(20) { checksum += legacy().sum(); checksum += indexed().sum() }
    val old = mutableListOf<Long>(); val new = mutableListOf<Long>()
    repeat(9) { pass ->
        fun run(slow:Boolean) = measureNanoTime { repeat(10) { checksum += (if(slow) legacy() else indexed()).sum() } }
        if(pass%2==0){old+=run(true);new+=run(false)}else{new+=run(false);old+=run(true)}
    }
    println("folder-counts,playlists=20000,folders=1000,legacyMs=${old.sorted()[4]/1e6},indexedMs=${new.sorted()[4]/1e6},checksum=$checksum")
}
