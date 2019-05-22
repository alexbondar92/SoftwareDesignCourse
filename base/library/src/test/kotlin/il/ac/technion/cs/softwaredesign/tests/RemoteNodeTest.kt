package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.DataStoreIo
import il.ac.technion.cs.softwaredesign.FakeSecureStorage
import il.ac.technion.cs.softwaredesign.RemoteNode
import org.junit.jupiter.api.Test


class RemoteNodeTest {
    @Test
    fun `insert node - basic 1`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val mainKey = 10.toString()
        val secondaryKey = 20.toString()
        val treeName = "MyTree"

        val node = RemoteNode(dataStore, treeName, mainKey, secondaryKey)      // write the new node to the server
        val node2 = RemoteNode(dataStore, treeName, mainKey)                   // reade the node from the server

        assert(node.compareTo(node2) == 0)
    }

    @Test
    fun `set left son`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val node = RemoteNode(dataStore, treeName, 10.toString(), 20.toString())       // write the new node to the server
        val son = RemoteNode(dataStore, treeName, 11.toString(), 21.toString())  // write the new node to the server

        node.setLeft(son)
        val tmpSon = node.getLeft()

        assert(tmpSon!!.compareTo(son) == 0)
    }

    @Test
    fun `set right son`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val node = RemoteNode(dataStore, treeName, 10.toString(), 20.toString())       // write the new node to the server
        val son = RemoteNode(dataStore, treeName, 11.toString(), 21.toString())  // write the new node to the server

        node.setRight(son)
        val tmpSon = node.getRight()

        assert(tmpSon!!.compareTo(son) == 0)
    }

    @Test
    fun `set parent`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val node = RemoteNode(dataStore, treeName, 10.toString(), 20.toString())       // write the new node to the server
        val son = RemoteNode(dataStore, treeName, 11.toString(), 21.toString())  // write the new node to the server

        son.setParent(node)
        val tmpParent = son.getParent()

        assert(tmpParent!!.compareTo(node) == 0)
    }

    @Test
    fun `set balance`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val node = RemoteNode(dataStore, treeName, 10.toString(), 20.toString())       // write the new node to the server

        node.setBalance(2)
        val tmpNode = RemoteNode(dataStore, treeName, 10.toString())

        assert(tmpNode.getBalance() == 2)
    }

    @Test
    fun `reset node`() {
        val dataStore = DataStoreIo(FakeSecureStorage())
        val treeName = "MyTree"
        val node = RemoteNode(dataStore, treeName, 10.toString(), 20.toString())       // write the new node to the server

        node.reset()
        val tmpNode = RemoteNode(dataStore, treeName, 10.toString())

        assert(tmpNode.getMainKey() == node.getMainKey() && tmpNode.getBalance() == node.getBalance() &&
                tmpNode.getLeft() == node.getLeft() && tmpNode.getRight() == node.getRight() &&
                tmpNode.getParent() == node.getParent())
    }
}