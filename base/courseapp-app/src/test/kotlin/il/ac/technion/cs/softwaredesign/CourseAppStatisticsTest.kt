package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.startsWith
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CourseAppStatisticsTest {

    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())

    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    private val courseApp = injector.getInstance<CourseApp>()
    private val appStatistics = injector.getInstance<CourseAppStatistics>()
    private val messageFactory = injector.getInstance<MessageFactory>()

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

    @Test
    fun `one user logged in, broadcast lead to one pending message`() {

    }

    @Test
    fun `one user logged in, private lead to one pending message`() {

    }

    @Test
    fun `one user logged in and in channel, channel lead to zero pending message`() {

    }

    @Test
    fun `one user logged in, 1000 broadcast lead to 1000 pending message`() {

    }

    @Test
    fun `one user logged in, 1000 private lead to 1000 pending message`() {

    }

    @Test
    fun `one user logged in and in channel, 1000 channel lead to zero pending message`() {

    }

    @Test
    fun `10000 user logged in, 1 broadcast lead to 10000 pending message`() {

    }

    @Test
    fun `10000 user logged in, 1000 private lead to 1000 pending message`() {

    }

    @Test
    fun `10000 user logged in and in channel, 1 channel lead to zero pending message`() {

    }

    // TODO ("does channel message in pending mode, is in did a message that needed to be counted as pendingMessages()")
    // TODO ("tests for channelMessages")

    @Test
    fun `get top10 channels by messages for empty system - before any opp`() {
        assertEquals(listOf<String>(), appStatistics.top10ChannelsByMessages().get())
    }

    @Test
    fun `get top10 channels by messages for empty system`() {
        val dict = hashMapOf<Int, String>()
        for (i in 1..100) {
            dict[i] = courseApp.login("user$i", "pass$i").get()
        }
        for (i in 2..50) {
            courseApp.logout(dict[i]!!)
        }

        for (i in 1..1000 ) {
            val message = messageFactory.create(MediaType.PICTURE, "Some Message No. $i".toByteArray()).get()
            courseApp.broadcast(dict[1]!!, message)
        }

        assertEquals(listOf<String>(), appStatistics.top10ChannelsByMessages().get())
    }

    @Test
    fun `get top10 for 7 channels with 1 message each`() {
        val adminToken = courseApp.login("admin", "pass").get()
        for (i in 1..100) {
            courseApp.channelJoin(adminToken, "#channel$i").get()
        }
        for (i in 1..93) {
            courseApp.channelPart(adminToken, "#channel$i").get()
        }

        val ret = listOf("#channel94", "#channel95", "#channel96", "#channel97", "#channel98", "#channel99", "#channel100")
        assertEquals(ret, appStatistics.top10ChannelsByMessages().get())
    }

    @Test
    fun `get top10 channels by messages for system with zero messages and 10000 channels`() {
        val adminToken = courseApp.login("admin", "pass").get()
        for (i in 1..10000) {
            if (i%1000 == 0)
                println("channel login: $i")
            courseApp.channelJoin(adminToken, "#channel$i").get()
        }
        for (i in 1..5000) {
//            if (i%1000 == 0)
                println("channel part: $i")
            courseApp.channelPart(adminToken, "#channel$i").get()
        }

        val ret = listOf("#channel5001", "#channel5002", "#channel5003", "#channel5004", "#channel5005", "#channel5006", "#channel5007", "#channel5008", "#channel5009", "#channel5010")
        assertEquals(ret, appStatistics.top10ChannelsByMessages().get())
    }

    @Test
    fun `get top10 channels by messages for system with 1000 messaeges and 1000 channels`() {

    }
}