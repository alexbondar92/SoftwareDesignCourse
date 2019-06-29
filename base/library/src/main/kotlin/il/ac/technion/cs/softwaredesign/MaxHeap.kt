package il.ac.technion.cs.softwaredesign

/**
 * MaxHeap of <[String]>. sorted by score (int) and then by addition order (older is bigger)
 * Implemented by [MaxHeapImpl] that based on the secureStorage
 * you can use it to store the million users ☺
 * added million users in 20 seconds! changed the scores and get top10 in another 5 seconds!
 *
 * Every max heap has a unique id (allow it to use private sector on the storage),
 * and you can get it by [getId] to restore max heap instance with [MaxHeapFactory.restoreMaxHeap].
 * secureStorage Latency (in millis):
 * * [add] - no latency (price for that - dont add element that already exist!)
 * * [remove] - O(log n) * the max length of element name
 * * [getScore] - the length of the element name + log10(Score) + log10(n)
 * * [changeScore] - O(log n) * the max length of element name
 * * [topTen] - O(1)
 * * [isEmpty] - no latency
 * * [nonEmpty] - no latency
 * * [count] - no latency
 * * [getId] - no latency
 *
 * @see MaxHeapImpl to see how it works
 */
interface MaxHeap {

    /**
     * add element with score = 0
     * element added to the end of the array
     * Do not add name that already exist!
     */
    fun add(name: String)

    /**
     * remove element
     * after remove you can add element with this name again
     */
    fun remove(name: String)

    /**
     * @return element score or 0 if [name] does not exist
     */
    fun getScore(name: String): Int
    fun changeScore(name: String, by: Int)

    /**
     * @return list of top ten elements
     */
    fun topTen(): List<String>
    fun isEmpty(): Boolean
    fun nonEmpty(): Boolean
    fun alreadyExist(name: String): Boolean
    /**
     * @return num of elements in the heap
     */
    fun count(): Int

    /**
     * @return heap id to use for heap restore
     */
    fun getId(): String
}