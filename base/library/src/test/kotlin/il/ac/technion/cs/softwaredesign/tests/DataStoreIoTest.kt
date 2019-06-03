package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.DataStoreIo
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.FakeSecureStorage
import il.ac.technion.cs.softwaredesign.FakeSecureStorageFactory
import org.junit.jupiter.api.Test
import java.time.Duration.*

class DataStoreIoTest {

    @Test
    fun `returns null for unused key`() {
        val storage = DataStoreIo(FakeSecureStorageFactory())

        val ret = storage.read(key = "Unknown Key")

        assert(ret == null)
    }

    @Test
    fun `write and read is consistent`() {
        val storage = DataStoreIo(FakeSecureStorageFactory())

        val value = "Some Data Sent to Storage"
        storage.write("My Key", value)

        assert(value == storage.read("My Key"))
    }

    @Test
    fun `read returns data for 1 ms per byte`() {
        val storage = DataStoreIo(FakeSecureStorageFactory())

        val value = "Long Long Long Long Long Long Long Long String"
        val valueLength = value.length.toLong()
        storage.write("My Key", value)

        assertThat(runWithTimeout(ofSeconds(valueLength)) { storage.read("My Key") ==  value },
                present(isTrue))
    }
}