package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorage

/*
reason for this class existence:
to limit the dependency of the data-store library, we use a wrapper class for easier refactoring at the future.
*/
class DataStoreIo {
    val storage: SecureStorage
    val cache : HashMap<String, String>

    @Inject constructor(storage: SecureStorage) {
        this.storage = storage
        this.cache = HashMap()                                  // Local cache for boosting the performance
    }
    fun write(key: String, data: String) {
        this.cache[key] = data                                  // Update the cache

        val charset = Charsets.UTF_8
        val tmpKey = key.toByteArray(charset)
        this.storage.write(tmpKey, data.toByteArray(charset))
    }

    fun read(key: String): String? {
        if (this.cache[key] != null) {
            return this.cache[key]                              // Got a hit at the cache
        }

        val tmpKey = key.toByteArray()
        val tmp = this.storage.read(tmpKey) ?: return null
        val charset = Charsets.UTF_8
        val value = tmp.toString(charset)
        this.cache[key] = value                                 // Update the cache
        return value
    }
}
