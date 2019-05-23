package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.DataStoreIo
import org.junit.jupiter.api.Test
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.FakeSecureStorage
import java.time.Duration.*

class DataStoreIoTest {

    @Test
    fun `returns null for unused key`() {
        val storage = DataStoreIo(FakeSecureStorage())

        val ret = storage.read(key = "Unknown Key")

        assert(ret == null)
    }

    @Test
    fun `write and read is consistent`() {
        val storage = DataStoreIo(FakeSecureStorage())

        val value = "Some Data Sent to Storage"
        storage.write("My Key", value)

        assert(value == storage.read("My Key"))
    }

    @Test
    fun `read returns data for 1 ms per byte`() {
        val storage = DataStoreIo(FakeSecureStorage())

        val value = "Long Long Long Long Long Long Long Long String"
        val valueLength = value.length.toLong()
        storage.write("My Key", value)

        assertThat(runWithTimeout(ofSeconds(valueLength)) { storage.read("Ky Key") ==  value },
                present(isTrue))
    }
}