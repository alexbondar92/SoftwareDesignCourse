package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage

/*
reason for this class existence:
to limit the dependency of the data-store library, we use a wrapper class for easier refactoring at the future.
*/
class DataStoreIo {
    val storage: SecureStorage
    constructor(storage: SecureStorage) {
        this.storage = storage
    }
    fun write(key: String, data: String) {
        val charset = Charsets.UTF_8
        val tmpKey = key.toByteArray(charset)
        return this.storage.write(tmpKey, data.toByteArray(charset))
    }

    fun read(key: String): String? {
        val tmpKey = key.toByteArray()
        val tmp = this.storage.read(tmpKey) ?: return null
        val charset = Charsets.UTF_8
        return tmp.toString(charset)
    }
}
