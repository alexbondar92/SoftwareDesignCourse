package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.messages.MessageFactoryImpl

class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()

        bind<DataStoreIo>().`in`<Singleton>()

        bind<CourseApp>().to<CourseAppImpl>()

        bind<CourseAppStatistics>().to<CourseAppStatisticsImpl>()

        bind<MessageFactory>().to<MessageFactoryImpl>().`in`<Singleton>()
    }
}