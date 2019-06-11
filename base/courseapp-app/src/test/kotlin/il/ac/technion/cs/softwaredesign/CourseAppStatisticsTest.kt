package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.containsElementsInOrder
import il.ac.technion.cs.softwaredesign.tests.runWithTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture

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
    fun `get top10 channels by messages for system with zero messages and 128 channels`() {
        val adminToken = courseApp.login("admin", "pass").get()
        for (i in 1..128) {
            courseApp.channelJoin(adminToken, "#channel$i").get()
        }
        for (i in 1..118) {
            courseApp.channelPart(adminToken, "#channel$i").get()
        }

        val ret = listOf("#channel119", "#channel120", "#channel121", "#channel122", "#channel123", "#channel124", "#channel125", "#channel126", "#channel127", "#channel128")
        assertEquals(ret, appStatistics.top10ChannelsByMessages().get())
    }

    @Test
    fun `get top10 channels by messages for system with 1000 messaeges and 1000 channels`() {

    }

    @Test
    fun `logged in user count is correct when no user is logged in`() {
        assertThat(
                runWithTimeout(Duration.ofSeconds(10)) { appStatistics.loggedInUsers().join() },
                equalTo(0L))
    }

    @Test
    fun `total user count is correct when no users exist`() {
        assertThat(
                runWithTimeout(Duration.ofSeconds(10)) { appStatistics.totalUsers().join() },
                equalTo(0L))
    }

    @Test
    fun `top 10 channel list does secondary sorting by creation`() {
        courseApp.login("admin", "admin")
                .thenCompose { adminToken -> courseApp.login("matan", "4321").thenApply { Pair(adminToken, it) } }
                .thenCompose { (adminToken, otherToken) ->
                    courseApp.makeAdministrator(adminToken, "matan")
                            .thenCompose { courseApp.channelJoin(adminToken, "#test") }
                            .thenCompose { courseApp.channelJoin(otherToken, "#other") }
                }.join()

        runWithTimeout(Duration.ofSeconds(10)) {
            assertThat(appStatistics.top10ChannelsByUsers().join(),
                    containsElementsInOrder("#test", "#other"))
        }
    }

    @Test
    fun `top 10 channel list counts only logged in users`() {
        courseApp.login("admin", "admin")
                .thenCompose { adminToken -> courseApp.login("matan", "4321").thenApply { Pair(adminToken, it) } }
                .thenCompose { (adminToken, otherToken) ->
                    courseApp.makeAdministrator(adminToken, "matan")
                            .thenCompose { courseApp.channelJoin(adminToken, "#test") }
                            .thenCompose { courseApp.channelJoin(otherToken, "#other") }
                            .thenCompose { courseApp.logout(otherToken) }
                }.join()

        runWithTimeout(Duration.ofSeconds(10)) {
            assertThat(appStatistics.top10ActiveChannelsByUsers().join(),
                    containsElementsInOrder("#test", "#other"))
        }
    }

    @Test
    fun `top 10 user list does secondary sorting by creation`() {
        courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("matan", "4321").thenApply { Pair(adminToken, it) }
                }.thenCompose { (adminToken, otherToken) ->
                    courseApp.makeAdministrator(adminToken, "matan")
                            .thenCompose { courseApp.channelJoin(adminToken, "#test") }
                            .thenCompose { courseApp.channelJoin(otherToken, "#other") }
                }.join()

        runWithTimeout(Duration.ofSeconds(10)) {
            assertThat(appStatistics.top10UsersByChannels().join(),
                    containsElementsInOrder("admin", "matan"))
        }
    }

    @Test
    fun `private message received successfully`() {
        var sources = mutableListOf<String>()
        var messages = mutableListOf<Message>()
        val callback: ListenerCallback = { source, message ->
            sources.add(source)
            messages.add(message)
            CompletableFuture.completedFuture(Unit)
        }
        val (token, message) = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("gal", "hunter2").thenApply { Pair(adminToken, it) }
                }.thenCompose { (adminToken, nonAdminToken) ->
                    courseApp.addListener(nonAdminToken, callback)
                            .thenCompose { messageFactory.create(MediaType.TEXT, "hello, world\n".toByteArray()) }
                            .thenApply { message -> Pair(adminToken, message) }
                }.join()

        runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.privateSend(token, "gal", message).join()
            assertEquals(0, appStatistics.pendingMessages().join())
        }

        assertEquals(1, sources.size)
        assertEquals(1, messages.size)
        assertEquals("@admin", sources[0])
        assert("hello, world\n".toByteArray().contentEquals(messages[0].contents))
    }

    @Test
    fun `there is 1 pending message after privateSend with no listener`() {
        courseApp.login("admin", "admin")
                .thenCompose { courseApp.login("gal", "hunter2") }
                .thenCompose { userToken ->
                    messageFactory.create(MediaType.TEXT, "how many programmers does it take to change a light-bulb?".toByteArray())
                            .thenCompose { msg -> courseApp.privateSend(userToken, "admin", msg) }
                }.join()

        assertEquals(1, runWithTimeout(Duration.ofSeconds(10)) { appStatistics.pendingMessages().join() })
    }
}