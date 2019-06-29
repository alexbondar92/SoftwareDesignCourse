package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import java.util.function.Supplier

/**
 * Factory for MaxHeap<[String]> based on SecureStorage.
 * EVERY! factory MUST have unique name (include [DictionaryFactory] and [LinkedListFactory]).
 * MaxHeapFactory supply new [MaxHeap] instances or restoring instances of MaxHeap
 * (if you have its id from [MaxHeap.getId])
 * You can create up to ([MAX_LIBRARY_NUM]/3) different max-heaps from the same Factory
 * (can be changed easily by changing [ID_NUM_OF_BYTES])
 *
 * Every factory must have unique name because it open its own SecureStorage.
 * You shouldn't have more than one factory for each data structure (3),
 * so 600 millis latency in summary if you use all 3 data structures.
 * In system reboot, if you instantiate the max-heaps at the same order (why wont you?),
 * the data in the storage should be available and correct.
 *
 * Use example:
 *      val maxHeapFactory = MaxHeapFactory(storageFactory, "heap")
 *      val heap = maxHeapFactory.get()
 *      val sameHeap = maxHeapFactory.restoreMaxHeap(heap.getId())
 */
class MaxHeapFactory @Inject constructor(sf: SecureStorageFactory, name: String) : Supplier<MaxHeap> {
    private val storage = sf.open(name.toByteArray()).join()

    private var counter = storage.read("".toByteArray()).join()?.toString(Charset.defaultCharset())?.toInt() ?: 0

    private fun nextId(): ByteArray {
        if (counter > MAX_LIBRARY_NUM) {
            throw Exception("Too many instances of library")
        }
        val id = intToByteArray(counter)
        counter++
        storage.write("".toByteArray(), counter.toString().toByteArray())
        return id
    }

    /**
     * [get] and [newMaxHeap] are the same.
     *
     * @return new MaxHeap
     */
    override fun get(): MaxHeap = newMaxHeap()

    fun newMaxHeap(): MaxHeap = MaxHeapImpl(storage, nextId() + nextId() + nextId())

    /**
     * restore lost instance of MaxHeap
     *
     * @param heapId - String got from old instance [MaxHeap.getId] method
     * @return new [MaxHeap] instance with the data of the old one (modify ond will modify the other)
     * @throws Exception("ID not valid") if the id
     */
    fun restoreMaxHeap(heapId: String): MaxHeap{
        if (heapId.toByteArray().size != ID_NUM_OF_BYTES * 3) throw Exception("ID not valid")
        return MaxHeapImpl(storage, heapId.toByteArray())
    }
}