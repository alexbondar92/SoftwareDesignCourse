package il.ac.technion.cs.softwaredesign.tests.old.courseapp

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.messages.MessageFactoryImpl

class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()

        bind<DataStoreIo>().`in`<Singleton>()

        bind<CourseApp>().to<CourseAppImpl>().`in`<Singleton>()

        bind<MessageFactory>().to<MessageFactoryImpl>().`in`<Singleton>()
    }
}