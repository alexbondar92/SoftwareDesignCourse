package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.KotlinModule

import il.ac.technion.cs.softwaredesign.CourseBots
import il.ac.technion.cs.softwaredesign.CourseBotsImpl


class CourseBotTestModule : KotlinModule(){
    override fun configure() {
        bind<CourseBots>().to<CourseBotsImpl>()
    }
}