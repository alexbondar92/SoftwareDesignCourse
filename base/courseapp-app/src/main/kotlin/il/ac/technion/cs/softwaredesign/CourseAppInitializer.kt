package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

/**
 * This class will be instantiated once, during system start, on an empty data-store.
 *
 * When testing, it will be instantiated once per test.
 */
interface CourseAppInitializer {
    /**
     * Initialize the data-store to some starting state.
     *
     * You may assume that when this method is called the data-store is completely empty.
     */
    fun setup(): CompletableFuture<Unit>
}