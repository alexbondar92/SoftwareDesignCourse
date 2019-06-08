package il.ac.technion.cs.softwaredesign

import kotlin.math.abs

class RemoteAvlTree {
    private var treeName: String
    private var storage: DataStoreIo
    private var root: RemoteNode? = null

    constructor(treeName: String, storage: DataStoreIo) {
        this.treeName = treeName
        this.storage = storage
        updateRoot()
    }

    private fun getRootKey(): String?{
        val str = storage.read(("T${this.treeName}")).get()
        if (str == null || str == "null")
            return null
        return str
    }

    private fun updateRoot() {
        var rootKey = getRootKey()
        if (rootKey == null)
            this.root = null
        else {
            val tempList = getRootKey()!!.split("%")
            rootKey = tempList[0]
            this.root = RemoteNode(storage, this.treeName, rootKey)
        }
    }

    private fun setRoot(newRoot: RemoteNode?) {
        this.root = newRoot
        if (newRoot == null)
            storage.write(("T${this.treeName}"), "null")
        else {
            newRoot.setParent(null)
            newRoot.setHeight()
            storage.write(("T${this.treeName}"), newRoot.getMainKey())
        }
    }

    fun insert(mainKey: String, secondaryKey: String): Boolean {
        updateRoot()
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
                val goLeft = currentNode.compareTo(newNode) == 1
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

    private fun rebalance(node: RemoteNode?) {      //TODO:("distinct insert/delete if possible maybe do it while debugging")
        var tmpNode = node
        while (tmpNode != null) {
            tmpNode.setHeight()
            tmpNode = rebalanceAux(tmpNode)
        }
    }

    private fun rebalanceAux(node: RemoteNode): RemoteNode? {    //TODO:("distinct insert/delete")
        var nextNode = node

        val balanceValue = node.getBalance()

        //decide which rotation to do if needed:
        if (balanceValue == -2)
            nextNode = if(node.getLeft()!!.getBalance() <= 0)
                rotateRight(node)
            else
                rotateLeftThenRight(node)

        else if (balanceValue == 2)
            nextNode = if(node.getRight()!!.getBalance() >= 0)
                rotateLeft(node)
            else
                rotateRightThenLeft(node)

        return if (nextNode.getParent() != null) {
            nextNode.getParent()
        } else {
            setRoot(nextNode)
            null
        }
    }

    private fun setHeights(papa : RemoteNode){
        papa.getLeft()?.setHeight()
        papa.getRight()?.setHeight()
        papa.setHeight()
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
            if (b.getParent()!!.getRight() != null && a.compareTo(b.getParent()!!.getRight()!!) == 0)
                b.getParent()!!.setRight(b)
            else
                b.getParent()!!.setLeft(b)
        }
        setHeights(b)   //update triangle heights after connection pointers.
        return b
    }

    private fun rotateLeftThenRight(node: RemoteNode): RemoteNode {
        node.setLeft(rotateLeft(node.getLeft()!!))
        val n = rotateRight(node)
        setHeights(n)
        return n
    }

    private fun rotateLeft(a: RemoteNode): RemoteNode {
        val b: RemoteNode? = a.getRight()
        b!!.setParent(a.getParent())        // look again on this...
        // set son of parent of b ... new son b
        a.setRight(b.getLeft())
        if (a.getRight() != null)
            a.getRight()!!.setParent(a)
        b.setLeft(a)
        a.setParent(b)
        if (b.getParent() != null) {
            if (b.getParent()!!.getLeft() != null && a.compareTo(b.getParent()!!.getLeft()!!) == 0)
                b.getParent()!!.setLeft(b)
            else
                b.getParent()!!.setRight(b)
        }
        setHeights(b)
        return b
    }

    private fun rotateRightThenLeft(node: RemoteNode): RemoteNode {
        node.setRight(rotateRight(node.getRight()!!))
        val n =  rotateLeft(node)
        setHeights(n)
        return n
    }

    //     fun delete(delKey: Int) {
    fun delete(mainKey: String, secondaryKey: String) {
        updateRoot()
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
        if (delNode != null) {  //if found node for deletion.
            if (delNode.getRight() == null) {   // the right son of delNode is null
                if (this.root != null && delNode.compareTo(this.root!!) == 0) { //new root, because node to delete is old root.
                    setRoot(delNode.getLeft())  //no need to update height.
                } else {        // there is a parent for delNode, not a root.
                    if (delNode.getLeft() != null)
                        delNode.getLeft()!!.setParent(parent)

                    parent!!.refresh()
                    if (parent.getRight() != null && parent.getRight()!!.compareTo(delNode) == 0){
                        parent.setRight(delNode.getLeft())
                    } else {
                        parent.setLeft(delNode.getLeft())
                    }
                    parent.refresh()
                    rebalance(parent)
                }
            } else if (n!!.compareTo(delNode.getRight()!!) == 0) {      // there is a successor for delNode at the sub tree & n is son of delNode
                n.setLeft(delNode.getLeft())
                delNode.getLeft()?.setParent(n)
                if (this.root != null && delNode.compareTo(this.root!!) == 0) {
                    setRoot(n)
                } else {        // there is a parent for delNode
                    n.setParent(delNode.getParent())
                    if (delNode.getParent()!!.getRight() != null && delNode.compareTo(delNode.getParent()!!.getRight()!!) == 0){
                        delNode.getParent()!!.setRight(n)
                    } else {
                        delNode.getParent()!!.setLeft(n)
                    }
                    parent!!.refresh()
                    rebalance(parent)
                }
            } else {        // the general case, hold assumptions: n have right son; n is the left son of his parent
                if (n.getRight() != null)
                    n.getRight()!!.setParent(parent)
                parent!!.setLeft(n.getRight())

                delNode.refresh()
                n.refresh()

                n.setLeft(delNode.getLeft())
                if (delNode.getLeft() != null)
                    delNode.getLeft()!!.setParent(n)

                delNode.refresh()
                n.refresh()

                n.setRight(delNode.getRight())
                if (delNode.getRight() != null)
                    delNode.getRight()!!.setParent(n)
                if (this.root != null && delNode.compareTo(this.root!!) == 0) {
                    setRoot(n)
                } else {
                    if (delNode.getParent()!!.getRight() != null &&  delNode.compareTo(delNode.getParent()!!.getRight()!!) == 0) {
                        delNode.getParent()!!.setRight(n)
                    } else {
                        delNode.getParent()!!.setLeft(n)
                    }
                }

                n.setParent(delNode.getParent())

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
        updateRoot()
        val list = mutableListOf<Int>()
        val adder: (RemoteNode) -> Unit = { x: RemoteNode -> list.add(x.getMainKey().toInt())}
        val cond: () -> Boolean = {list.size < 10 }
        top10Aux(this.root, adder, cond)
        return list
    }

    fun toKeyList() : List<Long> {
        val list = mutableListOf<Long>()
        val adder: (RemoteNode) -> Unit = { x: RemoteNode -> list.add(x.getMainKey().toLong())}
        tokKeyListAux(this.root, adder)

        return list
    }

    private fun tokKeyListAux(node: RemoteNode?, adder: (RemoteNode) -> Unit) {
        if (node == null)
            return

        tokKeyListAux(node.getRight(), adder)
        adder(node)
        tokKeyListAux(node.getLeft(), adder)
    }

    private fun balance(node: RemoteNode?) : Int {
        if(node == null) return 0
        return node.getBalance()
    }

    private fun balancedAux(node: RemoteNode?): Boolean {
        if (node == null ) return true
        return balancedAux(node.getRight()) &&
                balancedAux(node.getLeft()) &&
                abs(balance(node)) < 2
    }

    fun balanced() : Boolean = balancedAux(this.root)
}