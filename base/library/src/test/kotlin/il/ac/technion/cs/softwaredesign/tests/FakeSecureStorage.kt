package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.storage.*


// Fake secure storage for testing
class FakeSecureStorage : SecureStorage {
    private var DB: MutableMap<ByteArray, ByteArray> = mutableMapOf()

    constructor(newDB: MutableMap<ByteArray, ByteArray>) {
        this.DB = newDB
    }

    override fun write(key: ByteArray, value: ByteArray) {
        this.DB[key] = value
    }

    override fun read(key: ByteArray): ByteArray? {
        val ret = this.DB[key]
        if (ret != null)
            Thread.sleep(ret.size.toLong())     // similar as the real Secure Storage is working, with a "payment" of 1 ms per return byte
        return ret
    }
}

class FakeSecureStorageFactory : SecureStorageFactory {
    private var masterControllerDB: MutableMap<ByteArray, MutableMap<ByteArray, ByteArray>> = mutableMapOf()
    override fun open(name: ByteArray): SecureStorage {
        if (masterControllerDB[name] == null)
            masterControllerDB[name] = mutableMapOf()
        val ret = FakeSecureStorage(masterControllerDB[name]!!)
        return ret
    }
}