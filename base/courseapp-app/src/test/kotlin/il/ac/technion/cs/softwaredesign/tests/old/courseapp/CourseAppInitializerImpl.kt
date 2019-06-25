package il.ac.technion.cs.softwaredesign.tests.old.courseapp

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import java.util.concurrent.CompletableFuture

class CourseAppInitializerImpl: CourseAppInitializer {
    /**
     * Initialize the data-store to some starting state.
     *
     * You may assume that when this method is called the data-store is completely empty.
     */
    private val storage: DataStoreIo

    @Inject
    constructor(storage: DataStoreIo) {
        this.storage = storage
    }

    override fun setup(): CompletableFuture<Unit> {
        // Empty
        return CompletableFuture.completedFuture(Unit)
    }
}