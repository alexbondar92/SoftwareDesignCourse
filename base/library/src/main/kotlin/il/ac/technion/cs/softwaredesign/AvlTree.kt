package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.DataStoreIo

interface AvlTree<K> {
    fun insert(key: K): Boolean

    fun delete(delKey: K)

    fun printTree()

//    fun getData(key: K): V
}

interface Node<K>{
//    fun getData(): V?

//    fun setData(data: V)

    fun getKey(): K

    fun setKey(key: K)

    fun getLeft(): Node<K>

    fun setLeft(node: Node<K>)

    fun getRight(): Node<K>

    fun setRight(node: Node<K>)
}

class MyNode(var StorageKey: String) : Node<String>{
    override fun getKey(): String {
        return StorageKey
    }

    override fun setKey(key: String) {
        StorageKey = key
    }

    override fun getLeft(): Node<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setLeft(node: Node<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRight(): Node<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setRight(node: Node<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun parseValue(str: String): Triple<String, String, String>{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun composeValue(data: String, leftNode: String, rightNode: String){
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class MyAvlTree(var treeIndex: Int, var Storage: DataStoreIo) : AvlTree<String>{

    override fun insert(key: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(delKey: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun printTree() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getData(key: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getRoot(): MyNode?{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun rebalance(n: MyNode) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun rotateLeft(a: MyNode): MyNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun rotateRight(a: MyNode): MyNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun rotateLeftThenRight(n: MyNode): MyNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun rotateRightThenLeft(n: MyNode): MyNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun height(n: MyNode?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class AvlTree2 {
    private var root: Node? = null

    private class Node(var key: Int, var parent: Node?) {
        var balance: Int = 0
        var left : Node? = null
        var right: Node? = null
    }

    fun insert(key: Int): Boolean {
        if (root == null)
            root = Node(key, null)
        else {
            var n: Node? = root
            var parent: Node
            while (true) {
                if (n!!.key == key) return false
                parent = n
                val goLeft = n.key > key
                n = if (goLeft) n.left else n.right
                if (n == null) {
                    if (goLeft)
                        parent.left  = Node(key, parent)
                    else
                        parent.right = Node(key, parent)
                    rebalance(parent)
                    break
                }
            }
        }
        return true
    }

    fun delete(delKey: Int) {
        if (root == null) return
        var n:       Node? = root
        var parent:  Node? = root
        var delNode: Node? = null
        var child:   Node? = root
        while (child != null) {
            parent = n
            n = child
            child = if (delKey >= n.key) n.right else n.left
            if (delKey == n.key) delNode = n
        }
        if (delNode != null) {
            delNode.key = n!!.key
            child = if (n.left != null) n.left else n.right
            if (root!!.key == delKey)
                root = child
            else {
                if (parent!!.left == n)
                    parent.left = child
                else
                    parent.right = child
                rebalance(parent)
            }
        }
    }

    private fun rebalance(n: Node) {
        setBalance(n)
        var nn = n
        if (nn.balance == -2)
            if (height(nn.left!!.left) >= height(nn.left!!.right))
                nn = rotateRight(nn)
            else
                nn = rotateLeftThenRight(nn)
        else if (nn.balance == 2)
            if (height(nn.right!!.right) >= height(nn.right!!.left))
                nn = rotateLeft(nn)
            else
                nn = rotateRightThenLeft(nn)
        if (nn.parent != null) rebalance(nn.parent!!)
        else root = nn
    }

    private fun rotateLeft(a: Node): Node {
        val b: Node? = a.right
        b!!.parent = a.parent
        a.right = b.left
        if (a.right != null) a.right!!.parent = a
        b.left = a
        a.parent = b
        if (b.parent != null) {
            if (b.parent!!.right == a)
                b.parent!!.right = b
            else
                b.parent!!.left = b
        }
        setBalance(a, b)
        return b
    }

    private fun rotateRight(a: Node): Node {
        val b: Node? = a.left
        b!!.parent = a.parent
        a.left = b.right
        if (a.left != null) a.left!!.parent = a
        b.right = a
        a.parent = b
        if (b.parent != null) {
            if (b.parent!!.right == a)
                b.parent!!.right = b
            else
                b.parent!!.left = b
        }
        setBalance(a, b)
        return b
    }

    private fun rotateLeftThenRight(n: Node): Node {
        n.left = rotateLeft(n.left!!)
        return rotateRight(n)
    }

    private fun rotateRightThenLeft(n: Node): Node {
        n.right = rotateRight(n.right!!)
        return rotateLeft(n)
    }

    private fun height(n: Node?): Int {
        if (n == null) return -1
        return 1 + Math.max(height(n.left), height(n.right))
    }

    private fun setBalance(vararg nodes: Node) {
        for (n in nodes) n.balance = height(n.right) - height(n.left)
    }

    fun printKey() {
        printKey(root)
        println()
    }

    private fun printKey(n: Node?) {
        if (n != null) {
            printKey(n.left)
            print("${n.key} ")
            printKey(n.right)
        }
    }

    fun printBalance() {
        printBalance(root)
        println()
    }

    private fun printBalance(n: Node?) {
        if (n != null) {
            printBalance(n.left)
            print("${n.balance} ")
            printBalance(n.right)
        }
    }
}

fun main(args: Array<String>) {
    val tree = AvlTree2()
    println("Inserting values 1 to 10")
    for (i in 1..10) tree.insert(i)
    print("Printing key     : ")
    tree.printKey()
    print("Printing balance : ")
    tree.printBalance()
}