package com.mikesajak.library.util

class IdGenerator {
    private var id = 0L

    fun get(): Long {
        val curId = id
        id++
        return curId
    }
}