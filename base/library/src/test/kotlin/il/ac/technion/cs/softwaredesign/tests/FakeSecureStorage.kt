package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.storage.*


// Fake secure storage for testing
class FakeSecureStorage : SecureStorage {
    private var DB: MutableMap<String, String> = mutableMapOf()

    constructor() {
        this.DB = mutableMapOf()
    }

    constructor(newDB: MutableMap<String, String>) {
        this.DB = newDB
    }

    override fun write(key: ByteArray, value: ByteArray) {
        this.DB[key.toString(Charsets.UTF_8)] = value.toString(Charsets.UTF_8)
    }

    override fun read(key: ByteArray): ByteArray? {
        val ret = this.DB[key.toString(Charsets.UTF_8)]
        if (ret != null) {
            Thread.sleep(ret.length.toLong())     // similar as the real Secure Storage is working, with a "payment" of 1 ms per return byte
            return ret.toByteArray(Charsets.UTF_8)
        }
        return null
    }
}

class FakeSecureStorageFactory : SecureStorageFactory {
    private var masterControllerDB: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    override fun open(name: ByteArray): SecureStorage {
        if (masterControllerDB[name.toString(Charsets.UTF_8)] == null)
            masterControllerDB[name.toString(Charsets.UTF_8)] = mutableMapOf()
        val ret = FakeSecureStorage(masterControllerDB[name.toString(Charsets.UTF_8)]!!)
        return ret
    }
}