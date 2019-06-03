package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.*
import java.util.concurrent.CompletableFuture


// Fake secure storage for testing
class FakeSecureStorage : SecureStorage {
    private var cacheStorage: MutableMap<String, String> = mutableMapOf()

    constructor(newDB: MutableMap<String, String>) {
        this.cacheStorage = newDB
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit).thenApply {
            this.cacheStorage[key.toString(Charsets.UTF_8)] = value.toString(Charsets.UTF_8)
        }
    }

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        val ret = this.cacheStorage[key.toString(Charsets.UTF_8)]

        return CompletableFuture.completedFuture(ret?.toByteArray(Charsets.UTF_8)).thenApply {
            if (it != null)
                Thread.sleep(it.size.toLong())     // similar as the real Secure Storage is working, with a "payment" of 1 ms per return byte
            it
        }
    }
}

class FakeSecureStorageFactory : SecureStorageFactory {
    private var masterControllerDB: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        if (masterControllerDB[name.toString(Charsets.UTF_8)] == null)
            masterControllerDB[name.toString(Charsets.UTF_8)] = mutableMapOf()

        return CompletableFuture.completedFuture(FakeSecureStorage(masterControllerDB[name.toString(Charsets.UTF_8)]!!))
                .thenApply {
                    Thread.sleep(100)
                    it
                }
    }
}