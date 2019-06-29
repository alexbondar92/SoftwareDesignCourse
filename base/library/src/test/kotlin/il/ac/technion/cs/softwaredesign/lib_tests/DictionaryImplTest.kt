package il.ac.technion.cs.softwaredesign.lib_tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import il.ac.technion.cs.softwaredesign.FakeStorage
import il.ac.technion.cs.softwaredesign.DictionaryFactory
import il.ac.technion.cs.softwaredesign.FakeStorageModule
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*

internal class DictionaryImplTest {
    private val injector = Guice.createInjector(FakeStorageModule())

    private val factory = injector.getInstance<DictionaryFactory>()
    val lib1 = factory.newDictionary()
    val lib2 = factory.newDictionary()
    var key = "1"
    var value = "16"

    @BeforeEach
    fun prepDb(){
        key = "1"
        value = "16"
        lib2.write(key, value)
    }

    @AfterEach
    fun clearDb(){
        FakeStorage("".toByteArray()).clear()
    }

    @Test
    fun read() {
        assertNull(lib1.read(key))
        assertEquals(lib2.read(key), "16")
    }

    @Test
    fun write() {
        value = "18"
        lib1.write(key, value)
        assertEquals(lib2.read(key), "16")
        assertEquals(lib1.read(key), "18")
    }

    @Test
    fun `write a lot`() {
        value = "18"
        for (i in 1..1000000){
            lib1.write(i.toString(), value)
        }
    }

    @Test
    fun is_exsisted() {
        assert(lib2.contains(key))
        key = "9"
        assertFalse(lib2.contains(key))
    }

    @Test
    fun `restoring dictionary`(){
        var id = ""
        if (true){
            val lib = factory.newDictionary()
            id = lib.getId()
            lib.write("hello", "world")
        }
        val restored = factory.restoreDictionary(id)
        assertEquals(restored.read("hello"), "world")
    }

    @Test
    fun count(){
        assertEquals(lib1.count(), 0)
        lib1.incCount(7)
        assertEquals(lib1.count(), 7)
        lib1.incCount(-10)
        assertEquals(lib1.count(), -3)
    }

    @Test
    fun `count a lot`(){
        for (i in 1..1000000){
            lib1.count()
            lib1.incCount(1)
        }
    }
}