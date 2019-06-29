package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import java.util.function.Supplier

/**
 * Factory for LinkedList<[String]> based on SecureStorage.
 * EVERY! factory MUST have unique name (include [DictionaryFactory] and [MaxHeapFactory]).
 * LinkListFactory supply new [LinkedList] instances or restoring instances of LinkedList
 * (if you have its id from [LinkedList.getId])
 * You can create up to ([MAX_LIBRARY_NUM]/3) different lists from the same Factory
 * (can be changed easily by changing [ID_NUM_OF_BYTES])
 *
 * Every factory must have unique name because it open its own SecureStorage.
 * You shouldn't have more than one factory for each data structure (3),
 * so 600 millis latency in summary if you use all 3 data structures.
 * In system reboot, if you instantiate the lists at the same order (why wont you?),
 * the data in the storage should be available and correct.
 *
 * Use example:
 *      val listFactory = LinkedListFactory(storageFactory, "list")
 *      val list = listFactory.get()
 *      val sameList = listFactory.restoreLinkedList(list.getId())
 */
class LinkedListFactory @Inject constructor(sf: SecureStorageFactory, name: String) : Supplier<LinkedList> {
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
     * [get] and [newLinkedList] are the same.
     *
     * @return new linkedList
     */
    override fun get(): LinkedList = newLinkedList()

    fun newLinkedList(): LinkedList = LinkedListImpl(storage, nextId() + nextId() + nextId())

    /**
     * restore lost instance of LinkedList
     *
     * @param listId - String got from old instance [LinkedList.getId] method
     * @return new [LinkedList] instance with the data of the old one (modify ond will modify the other)
     * @throws Exception("ID not valid") if the id
     */
    fun restoreLinkedList(listId: String): LinkedList{
        if (listId.toByteArray().size != ID_NUM_OF_BYTES * 3) throw Exception("ID not valid")
        return LinkedListImpl(storage, listId.toByteArray())
    }
}