package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import org.junit.jupiter.api.Test

class CourseAppStatisticsTest {

    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())

    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    private val courseApp = injector.getInstance<CourseApp>()
    private val appStatistics = injector.getInstance<CourseAppStatistics>()

    init {
        courseAppInitializer.setup()
    }

    @Test
    fun `empty CourseApp`() {
        assert(appStatistics.loggedInUsers().get() == 0.toLong())
        assert(appStatistics.totalUsers().get() == 0.toLong())
        assert(appStatistics.top10UsersByChannels().get() == listOf<String>())
        assert(appStatistics.top10ChannelsByUsers().get() == listOf<String>())
        assert(appStatistics.top10ActiveChannelsByUsers().get() == listOf<String>())
    }

    @Test
    fun `valid number of totalUsers`() {
        for (i in 1..100) {
            courseApp.login("user$i", "pass$i")
        }

        assert(appStatistics.totalUsers().get() == 100.toLong())
    }

    @Test
    fun `valid number of loggedUsers`(){
        for (i in 1..100) {
            courseApp.login("user$i", "pass$i")
        }

        assert(appStatistics.loggedInUsers().get() == 100.toLong())
    }

    @Test
    fun `valid number of totalUsers after logout`(){
        val dict = hashMapOf<Int, String>()

        for (i in 1..100) {
            dict[i] = courseApp.login("user$i", "pass$i").get()
        }
        for (i in 1..50) {
            courseApp.logout(dict[i]!!)
        }

        assert(appStatistics.loggedInUsers().get() == 50.toLong())
        assert(appStatistics.totalUsers().get() == 100.toLong())
    }

    @Test
    fun `get top 10 users - basic 1`(){
        val userDict = hashMapOf<Int, String>()

        for (i in 1..20) {
            userDict[i] = courseApp.login("user$i", "pass$i").get()
        }

        val list = appStatistics.top10UsersByChannels().get()
        assert(list == listOf("user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10"))
    }

    @Test
    fun `get top 10 users - basic 2`() {
        val userDict = hashMapOf<Int, String>()

        for (i in 1..100) {
            userDict[i] = courseApp.login("user$i", "pass$i").get()
        }
        for (c in 1..20) {
            var str = ""
            for (j in 1..c) {
                str += "X"
            }
            courseApp.channelJoin(userDict[1]!!, "#channel$str").get()
            for (i in 20..30) {
                courseApp.channelJoin(userDict[i]!!, "#channel$str").get()
            }
        }

        assert(appStatistics.top10UsersByChannels().get() == listOf("user1", "user20", "user21", "user22", "user23", "user24", "user25", "user26", "user27", "user28"))
    }
}