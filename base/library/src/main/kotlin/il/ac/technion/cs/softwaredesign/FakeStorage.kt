package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

class FakeStorage(private val id: ByteArray) : SecureStorage {
    companion object{
        private val db = HashMap<String, ByteArray>()
    }
    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        val value = db[(id + key).toString(Charset.defaultCharset())]
        Thread.sleep((value?:return CompletableFuture.completedFuture(null)).size.toLong())
        return CompletableFuture.completedFuture(value)
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit>{
        db[(id + key).toString(Charset.defaultCharset())] = value.clone()
        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * use this method on any instance to clear all storage data
     */
    fun clear(){
        db.clear()
    }
}

class FakeStorageFactory: SecureStorageFactory{
    companion object{
        val names = HashSet<ByteArray>()
    }
    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        names.add(name) // will add only if not already exist
        Thread.sleep(100* names.size.toLong())
        return CompletableFuture.completedFuture(FakeStorage(name))
    }
}
