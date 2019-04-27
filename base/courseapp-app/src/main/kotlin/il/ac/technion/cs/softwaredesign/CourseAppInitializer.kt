package il.ac.technion.cs.softwaredesign

import java.util.*
import kotlin.collections.ArrayList

/**
 * This class will be instantiated once, during system start, on an empty data-store.
 *
 * When testing, it will be instantiated once per test.
 */
class CourseAppInitializer {
    private val million = 1000005
    public companion object {
        private var UserToPasswordMap = HashMap<String, String>()
        private var UserToTokenMap = HashMap<String, Int>()
        private var FreeTokens = LinkedList<Int>()
        private var ArrayToken = Array<String?>(million, null) // the index correspons with a token, null -> not exists, string -> username : String
    }
    /**
     * Initialize the data-store to some starting state.
     *
     * You may assume that when this method is called the data-store is completely empty.
     */
    fun setup(): Unit {
        UserToPasswordMap.clear()
        UserToTokenMap.clear()
        FreeTokens.clear()
        ArrayToken = Array<String?>(million, null)

        for (i in 5..million)
        FreeTokens.add(i)
    }
}