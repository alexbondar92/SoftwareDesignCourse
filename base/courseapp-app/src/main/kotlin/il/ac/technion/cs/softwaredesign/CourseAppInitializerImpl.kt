package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject

class CourseAppInitializerImpl: CourseAppInitializer {
    /**
     * Initialize the data-store to some starting state.
     *
     * You may assume that when this method is called the data-store is completely empty.
     */
    val storage: DataStoreIo

    @Inject
    constructor(storage: DataStoreIo) {
        this.storage = storage
    }

    override fun setup() {
        // TODO ("does we need to add here some shit?!?!")
    }
}