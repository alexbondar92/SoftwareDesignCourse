package il.ac.technion.cs.softwaredesign.tests.old.courseapp

class RemoteNode : Comparable<RemoteNode>{
    private var nodeStorageKey: String
    private var mainKey: String
    private var secondaryKey: String?
    private var height: Int = 0
    private var leftNodeKey:  String? = null
    private var rightNodeKey: String? = null
    private var treeName: String
    private var parentKey: String? = null
    private var storage: DataStoreIo

    init {
        secondaryKey = "null"
    }

    /*
        this constructor return a node with the values saved in storage,
        there is an overhead of reading from storage.
     */
    constructor(storage: DataStoreIo, treeName: String, mainKey: String) {
        this.storage = storage
        this.treeName = treeName
        this.mainKey = mainKey
        this.nodeStorageKey = "$treeName%$mainKey"                      // the StorageKey for the node

        val str = storage.read((this.nodeStorageKey)).get()
        if (str != null) {
            val tempList = str.split("%")                     // <secondaryKey>%<height>%<parent>%<leftNodeKey>%<rightNodeKey>
            this.secondaryKey = tempList[0]
            this.height = tempList[1].toInt()
            this.parentKey = tempList[2]
            this.leftNodeKey = tempList[3]
            this.rightNodeKey = tempList[4]
        } else {
            assert(false)
        }
    }

    /*
        this constructor creating a new node and updates storage for persistence matter,
        there is an overhead of writing from storage.
     */
    constructor(storage: DataStoreIo, treeName: String, mainKey: String, secondaryKey: String) {
        this.storage = storage
        this.treeName = treeName
        this.mainKey = mainKey
        this.secondaryKey = secondaryKey
        this.nodeStorageKey = "$treeName%$mainKey"                      // the StorageKey for the node
        this.height = 0         //leafs have height 0.
        this.leftNodeKey = null
        this.rightNodeKey = null
        this.parentKey = null

        flushNode()     //writes to storage
    }

    fun getParent(): RemoteNode? {
        return if (this.parentKey == "null" || this.parentKey == null)
            null
        else
            RemoteNode(storage, this.treeName, this.parentKey!!)
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

    private fun getLeftHeight() : Int{
        var heightLeft = getLeft()?.height
        if(heightLeft == null)
            heightLeft = -1     //null as a son have height of -1.
        return heightLeft
    }

    private fun getRightHeight() : Int{
        var heightRight = getRight()?.height
        if(heightRight == null)
            heightRight = -1    //null as a son have height of -1.
        return heightRight
    }

    fun getBalance(): Int = getRightHeight() - getLeftHeight()

    /*
        call this function only!! if suns heights are already updated,
        height of a leaf is -1 by definition, and already been set to this value by calling second constructor.
     */
    fun setHeight(){

        height = 1+ Math.max(getLeftHeight(), getRightHeight())
        flushNode()
    }

    fun getLeft(): RemoteNode? {
        return if (this.leftNodeKey == null || this.leftNodeKey == "null")
            null
        else
            RemoteNode(this.storage, this.treeName, this.leftNodeKey!!) //reads from storage
    }

    fun setLeft(node: RemoteNode?) {
        if (node == null)
            this.leftNodeKey = "null"
        else
            this.leftNodeKey = node.mainKey
        flushNode()     //writes to storage
    }

    fun getRight(): RemoteNode? {
        return if (this.rightNodeKey == null || this.rightNodeKey == "null")
            null
        else
            RemoteNode(this.storage, this.treeName, this.rightNodeKey!!)    //reads from storage
    }

    fun setRight(node: RemoteNode?) {
        if (node == null)
            this.rightNodeKey = null
        else
            this.rightNodeKey = node.mainKey
        flushNode()        //writes to storage
    }

    override fun compareTo(other: RemoteNode): Int {
        assert(this.secondaryKey != "null" )
        if (this.secondaryKey!!.toInt() > other.secondaryKey!!.toInt())
            return 1
        else if (this.secondaryKey!!.toInt() < other.secondaryKey!!.toInt())
            return -1
        else {
            if (this.mainKey.toInt() < other.mainKey.toInt())
                return 1
            else if (this.mainKey.toInt() > other.mainKey.toInt())
                return -1
        }
        return 0
    }

    private fun flushNode() {                       // <secondaryKey>%<height>%<parent>%<leftNodeKey>%<rightNodeKey>
        storage.write(this.nodeStorageKey, ("$secondaryKey%$height%$parentKey%$leftNodeKey%$rightNodeKey"))
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
        this.height = -1    //not important...
        this.parentKey = null
        this.leftNodeKey = null
        this.rightNodeKey = null

        flushNode()
    }

    fun refresh() {
        val tmpNode = RemoteNode(this.storage, this.treeName, this.mainKey)
        this.secondaryKey = tmpNode.secondaryKey
        this.height = tmpNode.height
        this.parentKey = tmpNode.parentKey
        this.leftNodeKey = tmpNode.leftNodeKey
        this.rightNodeKey = tmpNode.rightNodeKey
    }
}