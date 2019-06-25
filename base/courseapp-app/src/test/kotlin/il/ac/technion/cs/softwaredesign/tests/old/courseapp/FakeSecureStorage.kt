package il.ac.technion.cs.softwaredesign.tests.old.courseapp

import il.ac.technion.cs.softwaredesign.storage.*
import java.util.concurrent.CompletableFuture


// Fake secure storage for testing
class FakeSecureStorage : SecureStorage {
    private var cacheStorage: MutableMap<String, String> = mutableMapOf()
    private val charset = Charsets.UTF_8

    constructor(newDB: MutableMap<String, String>) {
        this.cacheStorage = newDB
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit).thenApply {
            this.cacheStorage[key.toString(charset)] = value.toString(charset)
        }
    }

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        val ret = this.cacheStorage[key.toString(charset)]

        return CompletableFuture.completedFuture(ret?.toByteArray(charset)).thenApply {
            if (it != null)
                Thread.sleep(it.size.toLong())     // similar as the real Secure Storage is working, with a "payment" of 1 ms per return byte
            it
        }
    }
}

class FakeSecureStorageFactory : SecureStorageFactory {
    private var masterControllerDB: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    private val charset = Charsets.UTF_8

    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        if (masterControllerDB[name.toString(charset)] == null)
            masterControllerDB[name.toString(charset)] = mutableMapOf()

        return CompletableFuture.completedFuture(FakeSecureStorage(masterControllerDB[name.toString(charset)]!!))
                .thenApply {
                    Thread.sleep(100)
                    it
                }
    }
}