package il.ac.technion.cs.softwaredesign


const val ID_NUM_OF_BYTES = 8
const val MAX_NUM_PER_BYYTE = 128
val MAX_LIBRARY_NUM = Math.pow(MAX_NUM_PER_BYYTE.toDouble(), ID_NUM_OF_BYTES.toDouble()).toInt() - 1

fun intToByteArray(value: Int): ByteArray{
    val id = ByteArray(ID_NUM_OF_BYTES)
    var temp = value
    for (i in 0 until ID_NUM_OF_BYTES) {
        id[i] = temp.rem(MAX_NUM_PER_BYYTE).toByte()
        temp /= MAX_LIBRARY_NUM
    }
    return id
}
