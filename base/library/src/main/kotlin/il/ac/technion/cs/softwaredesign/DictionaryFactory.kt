package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.function.Supplier

/**
 * Factory for Dictionary<[String], [String]> based on SecureStorage.
 * *EVERY!* factory *MUST* have unique name (include [LinkedListFactory] and [MaxHeapFactory]).
 * DictionaryFactory supply new Dictionary instances or restoring instances of Dictionary
 * (if you have its id from [Dictionary.getId])
 * You can create up to [MAX_LIBRARY_NUM] different dictionaries from the same Factory
 * (can be changed easily by changing [ID_NUM_OF_BYTES])
 *
 * Every factory must have unique name because it open its own SecureStorage.
 * You shouldn't have more than one factory for each data structure (3),
 * so 600 millis latency in summary if you use all 3 data structures.
 * In system reboot, if you instantiate the dictionaries at the same order (why wont you?),
 * the data in the storage should be available and correct.
 *
 * Use example:
 *      val dictionaryFactory = DictionaryFactory(storageFactory, "dict")
 *      val dict = dictionaryFactory.get()
 *      val sameDict = dictionaryFactory.restoreDictionary(dict.getId())
 */
class DictionaryFactory @Inject constructor(sf: SecureStorageFactory, name: String) : Supplier<Dictionary> {
    private val storage = sf.open(name.toByteArray()).join()

    private var counter = 0

    private fun nextId(): ByteArray {
        if (counter > MAX_LIBRARY_NUM) {
            throw Exception("Too many instances of library")
        }
        val id = intToByteArray(counter)
        counter++
        return id
    }


    /**
     * [get] and [newDictionary] are the same.
     *
     * @return new Dictionary
     */
    override fun get(): Dictionary = newDictionary()

    fun newDictionary(): Dictionary = DictionaryImpl(storage, nextId())

    /**
     * restore lost instance of Dictionary
     *
     * @param dictionaryId - String got from old instance [Dictionary.getId] method
     * @return new [Dictionary] instance with the data of the old one (modify ond will modify the other)
     * @throws Exception("ID not valid") if the id
     */
    fun restoreDictionary(dictionaryId: String): Dictionary {
        if (dictionaryId.toByteArray().size != ID_NUM_OF_BYTES) throw Exception("ID not valid")
        return DictionaryImpl(storage, dictionaryId.toByteArray())
    }
}
