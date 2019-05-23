package il.ac.technion.cs.softwaredesign

class RemoteAvlTree {
    private var treeName: String
    private var storage: DataStoreIo
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
            this.root = RemoteNode(storage, this.treeName, rootKey)
        }
    }

    private fun getRootKey(): String?{
        val str = storage.read(("T$this.treeName"))
        if (str == null || str == "null")
            return null
        return str
    }

    private fun setRoot(newRoot: RemoteNode?) {
        this.root = newRoot
        if (newRoot == null)
            storage.write(("T${this.treeName}"), "null")
        else {
            newRoot.setParent(null)
            storage.write(("T${this.treeName}"), newRoot.getMainKey())
        }
    }

    fun insert(mainKey: String, secondaryKey: String): Boolean {
        if (this.root == null) {
            this.root = RemoteNode(storage, this.treeName, mainKey, secondaryKey)
            setRoot(this.root!!)
        } else {
            val newNode = RemoteNode(storage, this.treeName, mainKey, secondaryKey)
            var currentNode: RemoteNode? = root
            var parentNode: RemoteNode
            while (true) {
                if (currentNode!!.compareTo(newNode) == 0) return false
                parentNode = currentNode
                val goLeft = currentNode > newNode
                currentNode = if (goLeft) currentNode.getLeft() else currentNode.getRight()
                if (currentNode == null) {
                    if (goLeft) {
                        parentNode.setLeft(newNode)
                        newNode.setParent(parentNode)
                    } else {
                        parentNode.setRight(newNode)
                        newNode.setParent(parentNode)
                    }
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
            if (b.getParent()!!.getLeft() == a)
                b.getParent()!!.setLeft(b)
            else
                b.getParent()!!.setRight(b)
        }
        setBalance(a, b)
        return b
    }

    private fun rotateRightThenLeft(node: RemoteNode): RemoteNode {
        node.setRight(rotateRight(node.getRight()!!))
        return rotateLeft(node)
    }

    //     fun delete(delKey: Int) {
    fun delete(mainKey: String, secondaryKey: String) {
        if (root == null) return
        var n:       RemoteNode? = root
        var parent:  RemoteNode? = root
        var delNode: RemoteNode? = null
        var child:   RemoteNode? = root
        while (child != null) {
            parent = n
            n = child
            child = if (n.compareUgly(mainKey, secondaryKey) <= 0) n.getRight() else n.getLeft()
            if (n.compareUgly(mainKey, secondaryKey) == 0)
                delNode = n
        }
        if (delNode != null) {
            if (delNode.getRight() == null) {   // the right son of delNode is null
                if (delNode == this.root) {
                    setRoot(delNode.getLeft())
                } else {        // there is a parent for delNode
                    val leftSon = delNode.getLeft()
                    if (leftSon != null)
                        leftSon.setParent(parent)

                    if (parent!!.getRight() == delNode){
                        parent!!.setRight(leftSon)
                    } else {
                        parent!!.setLeft(leftSon)
                    }
                    rebalance(parent)
                }
            } else if (n!!.compareTo(delNode.getRight()!!) == 0) {      // there is a successor for delNode at the sub tree & n is son of delNode
                n!!.setLeft(delNode.getLeft())
                delNode.getLeft()?.setParent(n)
                if (delNode == this.root) {
                    setRoot(n)
                } else {        // there is a parent for delNode
                    n.setParent(delNode.getParent())
                    if (delNode.getParent()!!.getRight() == delNode){
                        delNode.getParent()!!.setRight(n)
                    } else {
                        delNode.getParent()!!.setLeft(n)
                    }
                    rebalance(parent!!)
                }
            } else {        // the general case, hold assumptions: n have right son; n is the left son of his parent
//                val nRightSon = n!!.getRight()
//                val delNodeLeftSon = delNode.getLeft()
//                val delNodeRightSon = delNode.getRight()

                if (n!!.getRight() != null)
                    n!!.getRight()!!.setParent(parent)
                parent!!.setLeft(n!!.getRight())

                n.setLeft(delNode.getLeft())
                if (delNode.getLeft() != null)
                    delNode.getLeft()!!.setParent(n)
                n.setRight(delNode.getRight())
                if (delNode.getRight() != null)
                    delNode.getRight()!!.setParent(n)
                if (delNode == this.root) {
                    setRoot(n)
                } else {
                    if (delNode.getParent()!!.getRight() == delNode) {
                        delNode.getParent()!!.setRight(n)
                    } else {
                        delNode.getParent()!!.setLeft(n)
                    }
                }
                parent.refresh()
                rebalance(parent)
            }
            delNode.reset()
        }
    }

    private fun top10Aux(node: RemoteNode?, adder: (RemoteNode) -> Unit, cond : () -> Boolean) {
        if (node == null || !cond())
            return

        top10Aux(node.getRight(), adder, cond)
        if (!cond())
            return
        adder(node)
        top10Aux(node.getLeft(), adder, cond)
    }

    fun top10(): List<Int>{
        var list = mutableListOf<Int>()
        var adder: (RemoteNode) -> Unit = {x: RemoteNode -> list.add(x.getMainKey().toInt())}
        val cond: () -> Boolean = {list.size < 10 }
        top10Aux(this.root, adder, cond)
        return list
    }
}