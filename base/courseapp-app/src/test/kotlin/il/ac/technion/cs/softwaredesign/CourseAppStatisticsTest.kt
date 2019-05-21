package il.ac.technion.cs.softwaredesign

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import java.time.Duration

class CourseAppStatisticsTest {

    private val courseAppInitializer = CourseAppInitializerImpl()

    init {
        courseAppInitializer.setup()
    }

    private val CAstatistics: CourseAppStatistics = CourseAppStatisticsImpl()

    @Test
    fun `empty CourseApp`() {
        assert(CAstatistics.loggedInUsers() == 0.toLong())
        assert(CAstatistics.totalUsers() == 0.toLong())
        assert(CAstatistics.top10UsersByChannels() == listOf<String>())
        assert(CAstatistics.top10ChannelsByUsers() == listOf<String>())
        assert(CAstatistics.top10ActiveChannelsByUsers() == listOf<String>())
    }

    @Test
    fun `valid number of totalUsers`() {
        val CA = CourseAppImpl()

        for (i in 1..100) {
            CA.login("user$i", "pass$i")
        }

        assert(CAstatistics.totalUsers() == 10.toLong())
    }

    @Test
    fun `valid number of loggedUsers`(){
        val CA = CourseAppImpl()

        for (i in 1..100) {
            CA.login("user$i", "pass$i")
        }

        assert(CAstatistics.loggedInUsers() == 10.toLong())
    }

    @Test
    fun `valid number of totalUsers after logout`(){
        val CA = CourseAppImpl()
        val dict = hashMapOf<Int, String>()

        for (i in 1..100) {
            dict.put(i, CA.login("user$i", "pass$i"))
        }
        for (i in 1..50) {
            CA.logout(dict.get(i)!!)
        }

        assert(CAstatistics.loggedInUsers() == 50.toLong())
        assert(CAstatistics.totalUsers() == 100.toLong())
    }

    @Test
    fun `get top 10 users - basic 1`(){
        val CA = CourseAppImpl()
        val userDict = hashMapOf<Int, String>()

        for (i in 1..100) {
            userDict.put(i, CA.login("user$i", "pass$i"))
        }

        assert(CAstatistics.top10UsersByChannels() == listOf("user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10"))
    }

    @Test
    fun `get top 10 users - basic 2`(){
        val CA = CourseAppImpl()
        val userDict = hashMapOf<Int, String>()

        for (i in 1..100) {
            userDict.put(i, CA.login("user$i", "pass$i"))
        }
        for (c in 1..20) {
            CA.channelJoin(userDict.get(1)!!, "channel$c")
            for (i in 20..30) {
                CA.channelJoin("user$i", "channel$c")
            }
        }

        assert(CAstatistics.top10UsersByChannels() == listOf("user1", "user20", "user21", "user22", "user23", "user24", "user25", "user26", "user27", "user28"))
    }

    @Test
    fun `get top 10 channels - basic 1`
}