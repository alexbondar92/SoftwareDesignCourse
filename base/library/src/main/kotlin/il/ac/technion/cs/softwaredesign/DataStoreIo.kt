package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

/*
reason for this class existence:
to limit the dependency of the data-store library, we use a wrapper class for easier refactoring at the future.
*/
class DataStoreIo {
    val storage: CompletableFuture<SecureStorage>
    private val cache : HashMap<String, CompletableFuture<String?>>
    private val charset = Charsets.UTF_8


    @Inject constructor(storageFactory: SecureStorageFactory) {
        this.storage = storageFactory.open("remote secure storage".toByteArray(charset))
        this.cache = HashMap()                                  // Local cache for boosting the performance
    }
    fun write(key: String, data: String): CompletableFuture<Unit> {
        this.cache[key] = CompletableFuture.completedFuture(data)                  // Update the cache

        val tmpKey = key.toByteArray(charset)
        return this.storage.thenCompose { it.write(tmpKey, data.toByteArray(charset)) }
    }

    fun read(key: String): CompletableFuture<String?> {
        if (this.cache[key] != null) {
            return this.cache[key]!!                              // Got a hit at the cache
        }

        val tmpKey = key.toByteArray(charset)
        return this.storage.thenCompose {
            val tmpRet = it.read(tmpKey).thenApply {
                tmpVal ->
                tmpVal?.toString(charset)
            }
            this.cache[key] = tmpRet                                 // Update the cache
            tmpRet
        }
    }
}
