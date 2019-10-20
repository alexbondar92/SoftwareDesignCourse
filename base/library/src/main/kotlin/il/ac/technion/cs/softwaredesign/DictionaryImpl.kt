package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

/**
 * Implements [Dictionary] using [SecureStorage].
 * Each Dictionary has a unique [dictionaryId] ([DictionaryFactory] take care of that)
 * and it use this id as prefix to the key, so no other dictionary can change the data
 * (unless you used [DictionaryFactory.restoreDictionary] to get the same dictionary
 * or use other DictionaryFactory with the same name. don't do the second).
 * This [dictionaryId] also use for dictionary restoring, get it from [getId] and use it
 * as argument for [DictionaryFactory.restoreDictionary].
 *
 * Dictionary has [counter] (Int) that initiated to 0 and modified only from [incCount].
 * if the dictionary is restored, the [counter] will restored (and this is the only time you read it from storage)
 * [counter] stored in storage every time [incCount] called.
 * If storage was cleared the counter will go back to 0, see [count] to see how it works.
 *
 * secureStorage Latency - only for [read] and [contains] - the length of the value in millis
 */
class DictionaryImpl(private val storage: SecureStorage, private val dictionaryId: ByteArray) : Dictionary {
    private var counter = storage.read(dictionaryId + 2.toByte()).join()?.toString(Charset.defaultCharset())?.toInt()?: 0
    private val cache : HashMap<String, String?> = HashMap()


    override fun read(key: String): String? {
        if (this.cache[key] != null) {
            return this.cache[key]!!                              // Got a hit at the cache
        }

        val ret = storage.read(dictionaryId + 0.toByte() + key.toByteArray()).join()?.toString(Charset.defaultCharset())
        this.cache[key] = ret
        return ret
    }

    override fun write(key: String, value: String) {
        this.cache[key] = value

        storage.write(dictionaryId + 0.toByte() + key.toByteArray(), value.toByteArray()).join()
    }

    override fun contains(key: String): Boolean {
        if (this.cache[key] != null)
            return true

        read(key)?: return false
        return true
    }

    override fun isEmpty(): Boolean {
        return count() == 0
    }

    override fun nonEmpty(): Boolean {
        return !isEmpty()
    }

    override fun count(): Int {
        if (storage.read(dictionaryId + 1.toByte()).join() == null){
            // you here if storage was cleared and this instance is the same
            counter = 0
            storage.write(dictionaryId + 1.toByte(), "".toByteArray())
        }
        return counter
    }

    override fun incCount(by: Int) {
        counter = count() + by
        setCount(counter)
    }

    override fun getId(): String{
        return dictionaryId.toString(Charset.defaultCharset())
    }

    private fun setCount(value: Int){
        storage.write(dictionaryId + 2.toByte() , value.toString().toByteArray()).join()
    }
}
