package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage

class RemoteNode : Comparable<RemoteNode>{
    private var nodeStorageKey: String
    private var mainKey: String
    private var secondaryKey: String?
    private var balance: Int = 0
    private var leftNodeKey:  String? = null
    private var rightNodeKey: String? = null
    private var treeName: String
    private var parentKey: String? = null
    private var storage: DataStoreIo

    init {
        secondaryKey = "null"
    }

    constructor(storage: DataStoreIo, treeName: String, mainKey: String) {
        this.storage = storage
        this.treeName = treeName
        this.mainKey = mainKey
        this.nodeStorageKey = "$treeName%$mainKey"                      // the StorageKey for the node

        val str = storage.read((this.nodeStorageKey!!))
        if (str != null) {
            val tempList = str!!.split("%")                     // <secondaryKey>%<balance>%<parent>%<leftNodeKey>%<rightNodeKey>
            this.secondaryKey = tempList[0]
            this.balance = tempList[1].toInt()
            this.parentKey = tempList[2]
            this.leftNodeKey = tempList[3]
            this.rightNodeKey = tempList[4]
        } else {
            assert(false)
        }
    }

    constructor(storage: DataStoreIo, treeName: String, mainKey: String, secondaryKey: String) {
        this.storage = storage
        this.treeName = treeName
        this.mainKey = mainKey
        this.secondaryKey = secondaryKey
        this.nodeStorageKey = "$treeName%$mainKey"                      // the StorageKey for the node
        this.balance = 0
        this.leftNodeKey = null
        this.rightNodeKey = null
        this.parentKey = null

        flushNode()
    }

    fun getParent(): RemoteNode? {
        if (this.parentKey == "null" || this.parentKey == null)
            return null
        else
            return RemoteNode(storage, this.treeName, this.parentKey!!)
    }

    fun setParent(newParent: RemoteNode?) {
        if (newParent == null)
            this.parentKey = "null"
        else
            this.parentKey = newParent.mainKey
        flushNode()
    }

    fun getMainKey(): String = this.mainKey

//    fun getStorageKey(): String? = this.nodeStorageKey

    fun getBalance(): Int = this.balance

    fun setBalance(balance: Int) {
        this.balance = balance
        flushNode()
    }

    fun getLeft(): RemoteNode? {
        if (this.leftNodeKey == null || this.leftNodeKey == "null")
            return null
        else
            return RemoteNode(this.storage, this.treeName, this.leftNodeKey!!)
    }

    fun setLeft(node: RemoteNode?) {
        if (node == null)
            this.leftNodeKey = "null"
        else
            this.leftNodeKey = node.mainKey
        flushNode()
    }

    fun getRight(): RemoteNode? {
        if (this.rightNodeKey == null || this.rightNodeKey == "null")
            return null
        else
            return RemoteNode(this.storage, this.treeName, this.rightNodeKey!!)
    }

    fun setRight(node: RemoteNode?) {
        if (node == null)
            this.rightNodeKey = null
        else
            this.rightNodeKey = node.mainKey
        flushNode()
    }

    override fun compareTo(node: RemoteNode): Int {
        assert(this.secondaryKey != "null" )
        if (this.secondaryKey!!.toInt() > node.secondaryKey!!.toInt())
            return 1
        else if (this.secondaryKey!!.toInt() < node.secondaryKey!!.toInt())
            return -1
        else {
            if (this.mainKey.toInt() < node.mainKey.toInt())
                return 1
            else if (this.mainKey.toInt() > node.mainKey.toInt())
                return -1
        }
        return 0
    }

    private fun flushNode() {                       // <secondaryKey>%<balance>%<parent>%<leftNodeKey>%<rightNodeKey>
        storage.write(this.nodeStorageKey, ("$secondaryKey%$balance%$parentKey%$leftNodeKey%$rightNodeKey"))
    }

    fun compareUgly(mainKey: String, secondaryKey: String): Int {
        if (this.secondaryKey == "null") {
            print("KUKU")
        }
//        assert(this.secondaryKey != "null")
        if (this.secondaryKey!!.toInt() > secondaryKey.toInt())
            return 1
        else if (this.secondaryKey!!.toInt() < secondaryKey.toInt())
            return -1
        else {
            if (this.mainKey.toInt() < mainKey.toInt())
                return 1
            else if (this.mainKey.toInt() > mainKey.toInt())
                return -1
        }
        return 0
    }

    fun reset() {
        this.secondaryKey = "null"
        this.balance = 0
        this.parentKey = null
        this.leftNodeKey = null
        this.rightNodeKey = null

        flushNode()
    }

    fun refresh() {
        val tmpNode = RemoteNode(this.storage, this.treeName, this.mainKey)
        this.secondaryKey = tmpNode.secondaryKey
        this.balance = tmpNode.balance
        this.parentKey = tmpNode.parentKey
        this.leftNodeKey = tmpNode.leftNodeKey
        this.rightNodeKey = tmpNode.rightNodeKey
    }
}