package il.ac.technion.cs.softwaredesign

import org.junit.jupiter.api.Test

class CourseAppStatisticsTest {

    private val courseAppInitializer = CourseAppInitializerImpl()
    private val appStatistics: CourseAppStatistics
    private val storage: DataStoreIo = DataStoreIo(FakeSecureStorage())

    init {
        appStatistics = CourseAppStatisticsImpl(storage)
        courseAppInitializer.setup()
    }

    @Test
    fun `empty CourseApp`() {
        assert(appStatistics.loggedInUsers() == 0.toLong())
        assert(appStatistics.totalUsers() == 0.toLong())
        assert(appStatistics.top10UsersByChannels() == listOf<String>())
        assert(appStatistics.top10ChannelsByUsers() == listOf<String>())
        assert(appStatistics.top10ActiveChannelsByUsers() == listOf<String>())
    }

    @Test
    fun `valid number of totalUsers`() {
        val courseApp = CourseAppImpl(storage)

        for (i in 1..100) {
            println("i is: $i")
            courseApp.login("user$i", "pass$i")
        }

        assert(appStatistics.totalUsers() == 100.toLong())
    }

    @Test
    fun `valid number of loggedUsers`(){
        val courseApp = CourseAppImpl(storage)

        for (i in 1..100) {
            courseApp.login("user$i", "pass$i")
        }

        assert(appStatistics.loggedInUsers() == 100.toLong())
    }

    @Test
    fun `valid number of totalUsers after logout`(){
        val courseApp = CourseAppImpl(storage)
        val dict = hashMapOf<Int, String>()

        for (i in 1..100) {
            dict[i] = courseApp.login("user$i", "pass$i")
        }
        for (i in 1..50) {
            courseApp.logout(dict[i]!!)
        }

        assert(appStatistics.loggedInUsers() == 50.toLong())
        assert(appStatistics.totalUsers() == 100.toLong())
    }

    @Test
    fun `get top 10 users - basic 1`(){
        val courseApp = CourseAppImpl(storage)
        val userDict = hashMapOf<Int, String>()

        for (i in 1..20) {
            println("logging: $i")
            userDict[i] = courseApp.login("user$i", "pass$i")
        }

        val list = appStatistics.top10UsersByChannels()
        assert(list == listOf("user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10"))
    }

    @Test
    fun `get top 10 users - basic 2`() {
        val courseApp = CourseAppImpl(storage)
        val userDict = hashMapOf<Int, String>()

        for (i in 1..100) {
            userDict[i] = courseApp.login("user$i", "pass$i")
        }
        for (c in 1..20) {
            var str = ""
            for (j in 1..c) {
                str += "X"
            }
            courseApp.channelJoin(userDict[1]!!, "#channel$str")
            for (i in 20..30) {
                courseApp.channelJoin(userDict[i]!!, "#channel$str")
            }
        }

        assert(appStatistics.top10UsersByChannels() == listOf("user1", "user20", "user21", "user22", "user23", "user24", "user25", "user26", "user27", "user28"))
    }
}