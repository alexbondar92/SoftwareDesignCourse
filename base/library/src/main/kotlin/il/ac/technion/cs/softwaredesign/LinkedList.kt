package il.ac.technion.cs.softwaredesign

/**
 * LinkedList of <[String]>.
 * Implemented by [LinkedListImpl] that based on the secureStorage
 * Implements Iterable so you can use foreach: `for (elem in list) {...}`
 * Do not use to store the million users ☺
 * we used it for channels list per user and users list per channel (512 elements maximum)
 * and per user/channel we kept only the id and not the list instance
 *
 * Every list has a unique id (allow it to use private sector on the storage),
 * and you can get it by [getId] to restore list instance with [LinkedListFactory.restoreLinkedList].
 * secureStorage Latency (in millis):
 * * [add] - the max length of element (WC)
 * * [remove] - 5 * max length of element (WC) O(1)!!
 * * [getFirst] - the length of the first element
 * * [getNext] - the length of the returned element (foreach use this method so this is its latency too)
 * * [isEmpty] - no latency
 * * [nonEmpty] - no latency
 * * [contains] - the max length of element (WC) O(1)!!
 * * [count] - no latency
 * * [getId] - no latency
 *
 * @see LinkedListImpl to see how it works
 */
interface LinkedList: Iterable<String>{
    override fun iterator(): Iterator<String> {
        return LinkedListIterator(this)
    }

    /**
     * adds [value] to the end of the list
     * does nothing if list already contains [value]
     */
    fun add(value: String)

    /**
     * remove [value] from the list
     * does nothing if list not contains [value]
     */
    fun remove(value: String)

    /**
     * @return the first element if exist and null otherwise
     */
    fun getFirst(): String?

    /**
     * as usual, you must call [getFirst] before use this method
     * @return the next element if exist and null otherwise
     */
    fun getNext(): String?

    fun isEmpty(): Boolean
    fun nonEmpty(): Boolean
    fun contains(value: String): Boolean

    /**
     * @return num of elements in list
     */
    fun count(): Int

    /**
     * @return list id to use for list restore
     */
    fun getId(): String
}