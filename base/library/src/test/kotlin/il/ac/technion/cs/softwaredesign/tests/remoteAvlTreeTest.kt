package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.DataStoreIo
import il.ac.technion.cs.softwaredesign.FakeSecureStorage
import il.ac.technion.cs.softwaredesign.RemoteAvlTree
import org.junit.jupiter.api.Test

class RemoteAvlTreeTest {

    @Test
    fun `insert - basic 1`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        tree.insert(10.toString(), 20.toString())
        tree.delete(10.toString(), 20.toString())

        assert(tree.top10() == listOf<String>())
    }

    @Test
    fun `insert and delete - basic 1`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        tree.insert(10.toString(), 20.toString())
        tree.insert(11.toString(), 21.toString())
        tree.delete(10.toString(), 20.toString())

        assert(tree.top10().size == 1)
    }

    @Test
    fun `insert - basic 2`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        for (i in 1..10) {
            tree.insert(i.toString(), i.toString())
        }

        assert(tree.top10().size == 10)
    }

    @Test
    fun `insert 20 elements`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        for (i in 1..20) {
            tree.insert(i.toString(), i.toString())
            println("insert: $i")
        }

        val list = tree.top10()
        print(list)
        assert(tree.top10().size == 10)
    }

//    @Test
//    fun `insert 100 elements`() {
//        val dataStore = DataStoreIo(FakeSecureStorage())
//        val treeName = "MyTree"
//        val tree = RemoteAvlTree(treeName, dataStore)
//
//        for (i in 1..100) {
//            tree.insert(i.toString(), i.toString())
//            println("insert: $i")
//        }
//
//        assert(tree.top10().size == 10)
//    }

    @Test
    fun `insert 6 elements and remove part of them`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        for (i in 1..6) {
            tree.insert(i.toString(), i.toString())
            println("insert: $i")
        }
        for (i in 2..4) {
            tree.delete(i.toString(), i.toString())
            println("delete: $i")
        }

        assert(tree.top10().size == 3)
    }

    @Test
    fun `insert 10 elements and remove part of them`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val tree = RemoteAvlTree(treeName, dataStore)

        for (i in 1..10) {
            tree.insert(i.toString(), i.toString())
            println("insert: $i")
        }
        for (i in 6..7) {
            tree.delete(i.toString(), i.toString())
            println("delete: $i")
        }
        tree.delete(1.toString(), 1.toString())
        println("delete: 1")
        tree.delete(4.toString(), 4.toString())
        println("delete: 4")

        assert(tree.top10().size == 6)
    }
}