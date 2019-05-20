package il.ac.technion.cs.softwaredesign

class RemoteAvlTree {
    private var treeName: String
    private var storage: DataStoreIo? = null
    private var root: RemoteNode?

    constructor(treeName: String, storage: DataStoreIo) {
        this.treeName = treeName
        this.storage = storage
        val rootKey = getRootKey()
        if (rootKey == null)
            this.root = null
        else {
            val tempList = getRootKey()!!.split("%")
            val rootKey = tempList[0]
            this.root = RemoteNode(this.treeName, rootKey)
        }
    }

    private fun getRootKey(): String?{
        return DataStoreIo.read(("T$this.treeName"))
    }

    private fun setRoot(root: RemoteNode) {
        DataStoreIo.write(("T${this.treeName}"), root.getMainKey())
    }

    fun insert(mainKey: String, secondaryKey: String): Boolean {
        if (this.root == null) {
            this.root = RemoteNode(this.treeName, mainKey, secondaryKey)
            setRoot(this.root!!)
        } else {
            val newNode = RemoteNode(this.treeName, mainKey, secondaryKey)
            var currentNode: RemoteNode? = root
            var parentNode: RemoteNode
            while (true) {
                if (currentNode!!.compareTo(newNode) == 0) return false
                parentNode = currentNode
                val goLeft = currentNode > newNode
                currentNode = if (goLeft) currentNode.getLeft() else currentNode.getRight()
                if (currentNode == null) {
                    if (goLeft)
                        parentNode.setLeft(newNode)
                    else
                        parentNode.setRight(newNode)
                    rebalance(parentNode)
                    break
                }
            }
        }
        return true
    }

    private fun rebalance(node: RemoteNode) {
        setBalance(node)
        var nextNode = node
        if (nextNode.getBalance() == -2)
            if (height(nextNode.getLeft()!!.getLeft()) >= height(nextNode.getLeft()!!.getRight()))
                nextNode = rotateRight(nextNode)
            else
                nextNode = rotateLeftThenRight(nextNode)
        else if (nextNode.getBalance() == 2)
            if (height(nextNode.getRight()!!.getRight()) >= height(nextNode.getRight()!!.getLeft()))
                nextNode = rotateLeft(nextNode)
            else
                nextNode = rotateRightThenLeft(nextNode)
        if (nextNode.getParent() != null)
            rebalance(nextNode.getParent()!!)
        else
            setRoot(nextNode)
    }

    private fun setBalance(vararg nodes: RemoteNode) {
        for (node in nodes)
            node.setBalance(height(node.getRight()) - height(node.getLeft()))
    }

    private fun height(node: RemoteNode?): Int {
        if (node == null)
            return -1
        return 1+ Math.max(height(node.getLeft()), height(node.getRight()))
    }

    private fun rotateRight(a: RemoteNode): RemoteNode {
        val b: RemoteNode? = a.getLeft()
        b!!.setParent(a.getParent())
        a.setLeft(b.getRight())
        if (a.getLeft() != null)
            a.getLeft()!!.setParent(a)
        b.setRight(a)
        a.setParent(b)
        if (b.getParent() != null) {
            if (b.getParent()!!.getRight() == a)
                b.getParent()!!.setRight(b)
            else
                b.getParent()!!.setLeft(b)
        }
        setBalance(a, b)
        return b
    }

    private fun rotateLeftThenRight(node: RemoteNode): RemoteNode {
        node.setLeft(rotateLeft(node.getLeft()!!))
        return rotateRight(node)
    }

    private fun rotateLeft(a: RemoteNode): RemoteNode {
        val b: RemoteNode? = a.getRight()
        b!!.setParent(a.getParent())
        a.setRight(b.getLeft())
        if (a.getRight() != null)
            a.getRight()!!.setParent(a)
        b.setLeft(a)
        a.setParent(b)
        if (b.getParent() != null) {
            if (b.getParent()!!.getRight() == a)
                b.getParent()!!.setRight(b)
            else
                b.getParent()!!.setLeft(b)
        }
        setBalance(a, b)
        return b
    }

    private fun rotateRightThenLeft(node: RemoteNode): RemoteNode {
        node.setRight(rotateRight(node.getRight()!!))
        return rotateLeft(node)
    }

//    fun delete(mainKey: String, SecondaryKey: String) {
//        if (root == null) return
//        var n:       RemoteNode? = root
//        var parent:  RemoteNode? = root
//        var delNode: RemoteNode? = null
//        var child:   RemoteNode? = root
//        while (child != null) {
//            parent = n
//            n = child
//            child = if (delKey >= n.key) n.right else n.left
//            if (delKey == n.key) delNode = n
//        }
//        if (delNode != null) {
//            delNode.key = n!!.key
//            child = if (n.left != null) n.left else n.right
//            if (root!!.key == delKey)
//                root = child
//            else {
//                if (parent!!.left == n)
//                    parent.left = child
//                else
//                    parent.right = child
//                rebalance(parent)
//            }
//        }
//    }
}