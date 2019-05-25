package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.storage.SecureStorage

class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()

        bind<DataStoreIo>().`in`<Singleton>()

        bind<SecureStorage>().to<FakeSecureStorage>().`in`<Singleton>()         // TODO ("ask about it...)

        bind<CourseApp>().to<CourseAppImpl>()

        bind<CourseAppStatistics>().to<CourseAppStatisticsImpl>()
    }
}