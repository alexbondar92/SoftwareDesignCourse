//package il.ac.technion.cs.softwaredesign.lib_tests
//
//import com.authzee.kotlinguice4.getInstance
//import com.google.inject.Guice
//import il.ac.technion.cs.softwaredesign.*
//import org.junit.jupiter.api.*
//
//import org.junit.jupiter.api.Assertions.*
//
//internal class LinkedListTest {
//    private val injector = Guice.createInjector(FakeStorageModule())
//
//    private val factory = injector.getInstance<LinkedListFactory>()
//    val lib1 = factory.newLinkedList()
//    val lib2 = factory.newLinkedList()
//
//    @BeforeEach
//    fun prepDb(){
//        lib1.add("1")
//        lib1.add("2")
//        lib1.add("3")
//        lib1.add("4")
//        lib1.add("5")
//        lib1.add("6")
//    }
//
//    @AfterEach
//    fun clearDb(){
//        FakeStorage("".toByteArray()).clear()
//    }
//
//    @Test
//    fun getFirst() {
//        assertNull(lib2.getFirst())
//        assertNull(lib2.getNext())
//        assertEquals(lib1.getFirst(), "1")
//        assertEquals(lib1.getNext(), "2")
//        assertEquals(lib1.getNext(), "3")
//        assertEquals(lib1.getNext(), "4")
//        assertEquals(lib1.getNext(), "5")
//        assertEquals(lib1.getNext(), "6")
//        assertNull(lib1.getNext())
//        assertEquals(lib1.getFirst(), "1")
//        assertEquals(lib1.getNext(), "2")
//        assertEquals(lib1.getNext(), "3")
//        assertEquals(lib1.getFirst(), "1")
//        assertEquals(lib1.getNext(), "2")
//        assertEquals(lib1.getNext(), "3")
//    }
//
//    @Test
//    fun `add a lot`() {
//        for (i in 1..1000) {
//            lib2.add(i.toString())
//        }
//    }
//
//    @Test
//    fun remove() {
//        lib1.remove("3")
//        assertEquals(lib1.getFirst(), "1")
//        assertEquals(lib1.getNext(), "2")
//        assertEquals(lib1.getNext(), "4")
//        lib1.remove("1")
//        assertEquals(lib1.getFirst(), "2")
//        assertEquals(lib1.getNext(), "4")
//        assertEquals(lib1.getNext(), "5")
//        lib1.remove("6")
//        assertNull(lib1.getNext())
//        assertEquals(lib1.getFirst(), "2")
//        assertEquals(lib1.getNext(), "4")
//        lib1.remove("4")
//        assertNull(lib1.getNext())
//
//        lib2.add("sd")
//        lib2.add("sa")
//        lib2.remove("sa")
//        lib2.remove("sd")
//        assertNull(lib2.getFirst())
//        assertNull(lib2.getNext())
//        lib2.add("sd")
//        lib2.add("sa")
//        lib2.remove("sd")
//        lib2.remove("sa")
//        assertNull(lib2.getFirst())
//        assertNull(lib2.getNext())
//    }
//
//    @Test
//    fun empty() {
//        assert(lib2.isEmpty())
//        assert(lib1.nonEmpty())
//    }
//
//    @Test
//    fun `restoring list`(){
//        var id = ""
//        if (true){
//            val lib = factory.newLinkedList()
//            id = lib.getId()
//            lib.add("hello")
//            lib.add("world")
//        }
//        val restored = factory.restoreLinkedList(id)
//        assertEquals(restored.getFirst(), "hello")
//        assertEquals(restored.getNext(), "world")
//    }
//
//    @Test
//    fun count(){
//        assertEquals(lib1.count(), 6)
//        lib1.add("7")
//        assertEquals(lib1.count(), 7)
//        lib1.remove("4")
//        assertEquals(lib1.count(), 6)
//        lib1.remove("4")
//        assertEquals(lib1.count(), 6)
//    }
//
//    @Test
//    fun contains(){
//        assert(lib1.contains("2"))
//        assertFalse(lib1.contains("7"))
//    }
//    @Test
//    fun forEach(){
//        var value = 0
//        for (arg in lib1){
//            value++
//            assertEquals(arg.toInt(), value)
//        }
//    }
//}