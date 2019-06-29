package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage

/**
 * Implementation based on 3 [Dictionary]s:
 * * [forward] - for element as key has the next element as value ("" as value for the last one / deleted one)
 * * [backward] - for element as key has the previous element as value ("" as value for the first one / deleted one)
 * * [edges] - for "head"/"tail" as key has the first/last element as value ("" as value for empty list)
 */
class LinkedListImpl(storage: SecureStorage, listId: ByteArray) : LinkedList{
    companion object {
        private const val HEAD = "head"
        private const val TAIL = "tail"
    }

    private val forward = DictionaryImpl(storage, listId.slice(0 until (listId.size / 3)).toByteArray())
    private val backward = DictionaryImpl(storage,
            listId.slice((listId.size / 3) until (2 * listId.size / 3)).toByteArray())
    private val edges = DictionaryImpl(storage, listId.slice((2 * listId.size / 3) until listId.size).toByteArray())
    private var iterator = ""

    override fun add(value: String) {
        if (contains(value)) return
        if (count() == 0) {
            forward.write(value, "")
            backward.write(value, "")
            edges.write(HEAD, value)
            edges.write(TAIL, value)
        } else {
            val parent = edges.read(TAIL) ?: ""
            forward.write(parent, value)
            forward.write(value, "")
            backward.write(value, parent)
            edges.write(TAIL, value)
        }
        incCount(1)
    }

    override fun remove(value: String) {
        if (!contains(value)) return
        val successor = forward.read(value) ?: return
        val parent = backward.read(value) ?: return
        val head = edges.read(HEAD) ?: return
        val tail = edges.read(TAIL) ?: return
        if (head == value) {
            edges.write(HEAD, successor)
        } else {
            forward.write(parent, successor)
        }
        if (tail == value) {
            edges.write(TAIL, parent)
        } else {
            backward.write(successor, parent)
        }
        forward.write(value, "")
        backward.write(value, "")
        incCount(-1)
    }

    override fun getFirst(): String? {
        iterator = edges.read(HEAD) ?: ""
        if (iterator == "") return null
        return iterator
    }

    override fun getNext(): String? {
        if (iterator == "") return null
        iterator = forward.read(iterator) ?: ""
        if (iterator == "") return null
        return iterator
    }

    override fun isEmpty(): Boolean {
        return forward.isEmpty()
    }

    override fun nonEmpty(): Boolean {
        return !isEmpty()
    }

    override fun contains(value: String): Boolean {
        val successor = forward.read(value) ?: return false
        if (successor == "")
            // value is tail or deleted so:
            return (value == edges.read(TAIL))
        return true
    }

    override fun count(): Int {
        return forward.count()
    }

    override fun getId(): String = forward.getId() + backward.getId() + edges.getId()

    private fun incCount(value: Int) {
        forward.incCount(value)
    }
}