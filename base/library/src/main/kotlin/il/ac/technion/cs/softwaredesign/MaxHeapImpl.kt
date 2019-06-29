package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.LinkedList

/**
 * Implementation based on 3 [Dictionary]s:
 * * [pointers] - for element as key has the index as value (pointer to use for [remove], [getScore] and [changeScore])
 * * [array] - for index as key has the score + element as value
 * * [order] - for element as key has the addition order as value
 */
class MaxHeapImpl(storage: SecureStorage, heapId: ByteArray): MaxHeap {
    companion object {
        private const val MAX_SCORE = 1024
        private const val DALIM = '|'
    }

    private val pointers = DictionaryImpl(storage, heapId.slice(0 until (heapId.size / 3)).toByteArray())
    private val array = DictionaryImpl(storage,
            heapId.slice((heapId.size / 3) until (2 * heapId.size / 3)).toByteArray())
    private val order = DictionaryImpl(storage, heapId.slice((2 * heapId.size / 3) until heapId.size).toByteArray())

    override fun add(name: String) {
//        if (alreadyExist(name)) return
        incCount(1)
        val index = count()
        pointers.write(name, index.toString())
        array.write(index.toString(), "0$DALIM$name")
        order.incCount(1)
        val orderCount = order.count()
        order.write(name, orderCount.toString())
    }

    override fun remove(name: String) {
        val index = pointers.read(name)
        if (index.isNullOrEmpty()) return
        incScore(index.toInt(), MAX_SCORE)
        deleteMax()
    }

    override fun getScore(name: String): Int{
        val index = pointers.read(name)
        if (index.isNullOrEmpty()) return 0
        return getScoreByIndex(index.toInt())
    }

    override fun changeScore(name: String, by: Int) {
        if (by == 0) return
        if (!alreadyExist(name)) return
        val index = pointers.read(name)?.toInt() ?: return
        val newScore = getScoreByIndex(index) + by
        if (newScore >= MAX_SCORE) return
        if (newScore < 0) return
        if (by > 0) incScore(index, newScore)
        else if (by < 0) decScore(index, newScore)
    }

    override fun topTen(): List<String> {
        val res = LinkedList<String>()
        if (array.isEmpty()) return res
        val open = HashSet<Int>()
        open.add(1)
        while (open.isNotEmpty() && res.count() < 10) {
            val next = open.maxBy { comperator(it) } ?: return res
            open.remove(next)
            if (comperator(next) == -1) return res
            res.add(getName(next))
            if (2 * next <= count()) open.add(2 * next)
            if (2 * next + 1 <= count()) open.add(2 * next + 1)
        }
        return res
    }

    private fun siftUp(index: Int) {
        if (index == 1) return
        val parent = index / 2
        if (compare(parent, index) > 0) return
        switch(parent, index)
        siftUp(parent)
    }

    private fun siftDown(index: Int) {
        if (2 * index > count()) return
        val left = 2 * index
        val right = left + 1
        var max = left
        if (right <= count()) if (compare(right, left) > 0) max = right
        if (compare(index, max) > 0) return
        switch(index, max)
        siftDown(max)
    }

    private fun incScore(index: Int, newScore: Int) {
        val name = getName(index)
        array.write(index.toString(), newScore.toString() + DALIM + name)
        siftUp(index)
    }

    private fun decScore(index: Int, newScore: Int) {
        if (isEmpty()) return
        val name = getName(index)
        array.write(index.toString(), newScore.toString() + DALIM + name)
        siftDown(index)
    }

    override fun alreadyExist(name: String): Boolean {
        val i = pointers.read(name)
        if (i.isNullOrEmpty()) return false
        return true
    }

    private fun deleteMax() {
        if (isEmpty()) return
        val name = getName(1)
        if (name == "") return
        switch(1, count())
        array.write(count().toString(), "")
        pointers.write(name, "")
        incCount(-1)
        siftDown(1)
    }

    private fun getScoreByIndex(index: Int): Int {
        val string = array.read(index.toString()) ?: return -1
        return string.split(DALIM)[0].toInt()
    }

    private fun getName(index: Int): String {
        val string = array.read(index.toString()) ?: return ""
        val i = string.indexOf(DALIM)
        return string.substring(i + 1)
    }

    private fun switch(first: Int, second: Int) {
        if (first == second) return
        val firstString = array.read(first.toString()) ?: return
        val secondString = array.read(second.toString()) ?: return
        val firstName = getName(first)
        val secondName = getName(second)
        array.write(first.toString(), secondString)
        array.write(second.toString(), firstString)
        pointers.write(firstName, second.toString())
        pointers.write(secondName, first.toString())
    }

    private fun compare(first: Int, second: Int): Int {
        val firstScore = getScoreByIndex(first)
        val secondScore = getScoreByIndex(second)
        if (firstScore == secondScore) {
            val firstName = getName(first)
            val secondName = getName(second)
            val firstOrder = order.read(firstName)?.toInt() ?: 0
            val secondOrder = order.read(secondName)?.toInt() ?: 0
            if (firstOrder < secondOrder) return 1
            return -1
        } else if (firstScore > secondScore) return 1
        return -1
    }

    private fun comperator(index: Int): Int {
        val name = getName(index)
        if (name == "") return -1
        val ord = order.read(name)?.toInt() ?: return -1
        return (getScoreByIndex(index) + 1) * 10 * order.count() - ord
    }

    override fun count(): Int {
        return array.count()
    }

    private fun incCount(value: Int) {
        array.incCount(value)
    }

    override fun isEmpty(): Boolean{
        return array.isEmpty()
    }
    override fun nonEmpty(): Boolean{
        return array.nonEmpty()
    }


    override fun getId(): String = pointers.getId() + array.getId() + order.getId()
}