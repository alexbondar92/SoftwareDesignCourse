package il.ac.technion.cs.softwaredesign

class LinkedListIterator(private val list: LinkedList) : Iterator<String> {
    var next = list.getFirst()
    override fun hasNext(): Boolean {
        next ?: return false
        return true
    }

    override fun next(): String {
        val value = next
        next = list.getNext()
        return value ?: ""
    }

}
