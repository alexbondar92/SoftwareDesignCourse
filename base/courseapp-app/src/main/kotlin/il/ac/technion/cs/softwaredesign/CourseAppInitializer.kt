package il.ac.technion.cs.softwaredesign

import java.util.*
import kotlin.collections.ArrayList

/**
 * This class will be instantiated once, during system start, on an empty data-store.
 *
 * When testing, it will be instantiated once per test.
 */
class CourseAppInitializer {
    private val million = 1000000
    companion object {
        private var UserToPasswordMap = HashMap<String, String>()
        private var UserToTokenMap = HashMap<String, Int>()
        private var FreeTokens = LinkedList<Int>()
        private var ArrayToken = ArrayList<Boolean>() // if null -> not used; false -> invalid token(used in the past); true -> in use
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
        ArrayToken.clear()

        for (i in 1..million)
        FreeTokens.add(i)
    }
}