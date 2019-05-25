package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()

        bind<DataStoreIo>().`in`<Singleton>()

        // Factory How?!?!?
        bind<SecureStorageFactory>().to<FakeSecureStorageFactory>().`in`<Singleton>()         // TODO ("ask about it...)

        bind<CourseApp>().to<CourseAppImpl>()

        bind<CourseAppStatistics>().to<CourseAppStatisticsImpl>()
    }
}