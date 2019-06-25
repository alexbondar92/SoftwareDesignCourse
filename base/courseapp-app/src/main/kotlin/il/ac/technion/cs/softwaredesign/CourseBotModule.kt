package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule

class CourseBotModule : KotlinModule() {
    override fun configure() {
        bind<CourseBot>().to<CourseBotImpl>()
    }
}