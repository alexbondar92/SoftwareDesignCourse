package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton

class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()

        bind<DataStoreIo>().`in`<Singleton>()

        bind<CourseApp>().to<CourseAppImpl>()

        bind<CourseAppStatistics>().to<CourseAppStatisticsImpl>()
    }
}