package il.ac.technion.cs.softwaredesign

/**
 * Dictionary of <[String], [String]>.
 * Implemented by [DictionaryImpl] that based on the secureStorage
 *
 * Every Dictionary has a unique id (allow it to use private sector on the storage),
 * and you can get it by [getId] to restore dictionary instance with [DictionaryFactory.restoreDictionary].
 * Dictionary has counter (Int) that initiated to 0 and modified only from [incCount].
 * secureStorage Latency - only for [read] and [contains] - the length of the value in millis
 *
 * @see DictionaryImpl to see how it works
 */
interface Dictionary {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun contains(key: String): Boolean
    fun isEmpty(): Boolean
    fun nonEmpty(): Boolean
    fun count(): Int // int field of the dictionary, initiated to 0, change only from incCount
    fun incCount(by: Int) // increase the int field by [by]. can be negative
    fun getId(): String // use to future restore the dictionary
}