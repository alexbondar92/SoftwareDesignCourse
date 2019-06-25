package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice

import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.exceptions.NoSuchEntityException
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.old.courseapp.CourseAppModule
import il.ac.technion.cs.softwaredesign.tests.old.courseapp.FakeSecureStorageModule


import org.junit.jupiter.api.*


import org.junit.jupiter.api.Order

import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.CompletableFuture


class CourseBotTests {
    private val injector = Guice.createInjector(CourseAppModule(), CourseBotTestModule(), FakeSecureStorageModule())
    private var courseApp :CourseApp
    private var bots : CourseBots
    private var messageFactory : MessageFactory
    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
        courseApp = injector.getInstance()
        bots = injector.getInstance()
        messageFactory = injector.getInstance()
    }




    @Test
    @Order(1)
    fun `doing nothing here`(){

    }

    @Test
    @Order(2)
    fun `Can create a bot and add make it join channels`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel")
                    .thenCompose { bots.bot() }
                    .join()
        bot.join("#channel").join()
        assertEquals(listOf("#channel"), bot.channels().join())
    }

    @Test
    @Order(3)
    fun `Can list bot in a channel, default name checked`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        val bot = bots.bot().join()
        bot.join("#channel").join()

        assertEquals(listOf("Anna0"),bots.bots("#channel").join())
    }

    @Test
    @Order(4)
    fun `bot joins a channel twice, affects as joining once`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel")
                .thenCompose { bots.bot() }
                .join()

        bot.join("#channel").join()
        bot.join("#channel").join()
        assertEquals(listOf("#channel"), bot.channels().join())
    }

    @Test
    @Order(5)
    fun `bot joins a channel once, then leave it have empty list of channels`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel")
                .thenCompose { bots.bot() }
                .join()

        bot.join("#channel").join()
        bot.part("#channel").join()
        assertEquals(listOf<String>(), bot.channels().join())
    }

    @Test
    @Order(6)
    fun `bot joins a channel once, then leave it twice throws exception`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel")
                .thenCompose { bots.bot() }
                .join()

        bot.join("#channel").join()
        bot.part("#channel").join()

        assertThrows<NoSuchEntityException>{bot.part("#channel").join()}
    }

    @Test
    @Order(6)
    fun `bot joins few channels and returns all channels in order of joining`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel1")
                .thenCompose { bots.bot() }
                .join()
        courseApp.channelJoin(token, "#channel2").join()


        bot.join("#channel2").join()
        bot.join("#channel1").join()

        assertEquals(listOf("#channel2", "#channel1"), bot.channels().join())
    }

    @Test
    @Order(7)
    fun `bot joins few channels and returns all channels after creating another bot with the same name`() {
        val token = courseApp.login("gal", "hunter2").join()
        val bot = courseApp.channelJoin(token, "#channel1")
                .thenCompose { bots.bot() }
                .join()
        courseApp.channelJoin(token, "#channel2").join()


        bot.join("#channel2").join()
        bot.join("#channel1").join()
        val otherBotDifName = bots.bot("Anna0").join()

        assertEquals(listOf("#channel2", "#channel1"), otherBotDifName.channels().join())
    }

    @Test
    @Order(8)
    fun `Can list bots in a channel, default name checked`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        val bot1 = bots.bot().join()
        bot1.join("#channel").join()
        val bot2 = bots.bot().join()
        bot2.join("#channel").join()
        val bot3 = bots.bot().join()
        bot3.join("#channel").join()

        assertEquals(listOf("Anna0","Anna1","Anna2"),bots.bots("#channel").join())
    }

    @Test
    @Order(9)
    fun `Can list bots in a channel, bot names are sorted by creation time`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        val bot1 = bots.bot("NotAnna").join()
        bot1.join("#channel").join()
        val bot2 = bots.bot().join()
        bot2.join("#channel").join()
        val bot3 = bots.bot().join()
        bot3.join("#channel").join()

        assertEquals(listOf("NotAnna","Anna0","Anna1"),bots.bots("#channel").join())
    }

    @Test
    @Order(10)
    fun `Can list bots in all system, bot names are sorted by creation time`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        val bot1 = bots.bot("NotAnna").join()
        bot1.join("#channel").join()
        val bot2 = bots.bot().join()
        bot2.join("#channel").join()
        val bot3 = bots.bot().join()
        bot3.join("#channel").join()

        assertEquals(listOf("NotAnna","Anna0","Anna1"),bots.bots().join())
    }

    @Test
    @Order(11)
    fun `Can list bots in all system, bots in different channels maintain correct order`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel1").join()
        courseApp.channelJoin(adminToken, "#channel2").join()
        val bot1 = bots.bot("NotAnna").join()
        bot1.join("#channel1").join()
        val bot2 = bots.bot().join()
        bot2.join("#channel2").join()
        val bot3 = bots.bot().join()
        bot3.join("#channel1").join()

        assertEquals(listOf("NotAnna","Anna0","Anna1"),bots.bots().join())
    }

    @Test
    @Order(12)
    fun `Can list bots in all system, bots have not default names, bots in different channels maintain correct order`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel1").join()
        courseApp.channelJoin(adminToken, "#channel2").join()
        val bot1 = bots.bot("NotAnna").join()
        bot1.join("#channel1").join()
        val bot2 = bots.bot("random00").join()
        bot2.join("#channel2").join()
        val bot3 = bots.bot("whatwhat").join()
        bot3.join("#channel1").join()

        assertEquals(listOf("NotAnna","random00","whatwhat"),bots.bots().join())
    }

    @Test
    @Order(13)
    fun `The bot accurately tracks keywords - activate count with channel equals null`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel = "#channel"
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel).join()
        val bot = bots.bot().join()
        bot.join(channel).join()
        bot.beginCount(channel, regex) .join()
        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel).join()
        courseApp.channelSend(token, channel, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(regex =  regex) }.join())
    }

    @Test
    @Order(14)
    fun `The bot accurately tracks keywords - activate count with channel `() {
        val regex = ".*ello.*[wW]orl.*"
        val channel = "#channel"
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel).join()
        val bot = bots.bot().join()
        bot.join(channel).join()
        bot.beginCount(channel, regex) .join()
        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel).join()
        courseApp.channelSend(token, channel, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel,regex =  regex) }.join())
    }

    @Test
    @Order(15)
    fun `beginCount gets null regex and null mediaType`() {
        val channel = "#channel"
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel).join()
        val bot = bots.bot().join()
        bot.join(channel).join()

        assertThrows<IllegalArgumentException>{bot.beginCount(channel) .join()}
    }

    @Test
    @Order(16)
    fun `bot tracks messages in 2 channels, each channel got a matching message`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel1 = "#channel1"
        val channel2 = "#channel2"

        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel1).join()
        courseApp.channelJoin(adminToken, channel2).join()

        val bot = bots.bot().join()
        bot.join(channel1).join()
        bot.beginCount(channel1, regex) .join()
        bot.join(channel2).join()
        bot.beginCount(channel2, regex) .join()

        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel1).join()
        courseApp.channelSend(token, channel1, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()
        courseApp.channelJoin(token, channel2).join()
        courseApp.channelSend(token, channel2, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel1,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel2,regex =  regex) }.join())
        assertEquals(2L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(regex =  regex) }.join())
    }


    @Test
    @Order(17)
    fun `bot tracks messages in 2 channels, 1 message in first channel`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel1 = "#channel1"
        val channel2 = "#channel2"

        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel1).join()
        courseApp.channelJoin(adminToken, channel2).join()

        val bot = bots.bot().join()
        bot.join(channel1).join()
        bot.beginCount(channel1, regex) .join()
        bot.join(channel2).join()
        bot.beginCount(channel2, regex) .join()

        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel1).join()
        courseApp.channelSend(token, channel1, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()
        //courseApp.channelJoin(token, channel2).join()
        //courseApp.channelSend(token, channel2, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel1,regex =  regex) }.join())
        assertEquals(0L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel2,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(regex =  regex) }.join())
    }

    @Test
    @Order(18)
    fun `bot tracks messages in 2 channels, 1 message in second channel`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel1 = "#channel1"
        val channel2 = "#channel2"

        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel1).join()
        courseApp.channelJoin(adminToken, channel2).join()

        val bot = bots.bot().join()
        bot.join(channel1).join()
        bot.beginCount(channel1, regex) .join()
        bot.join(channel2).join()
        bot.beginCount(channel2, regex) .join()

        val token = courseApp.login("matan", "s3kr3t").join()
        //courseApp.channelJoin(token, channel1).join()
        //courseApp.channelSend(token, channel1, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()
        courseApp.channelJoin(token, channel2).join()
        courseApp.channelSend(token, channel2, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(0L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel1,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel2,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(regex =  regex) }.join())
    }

    @Test
    @Order(19)
    fun `bot ignores messages that are not matching by regular expression`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel = "#channel"


        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel).join()

        val bot = bots.bot().join()
        bot.join(channel).join()
        bot.beginCount(channel, regex) .join()

        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel).join()
        courseApp.channelSend(token, channel, messageFactory.create(MediaType.TEXT, "notHELLOWORLD!".toByteArray()).join()).join()

        assertEquals(0L, bots.bot("Anna0").thenCompose { bot2 -> bot2.count(channel = channel,regex =  regex) }.join())
    }

    @Test
    @Order(20)
    fun `2 bots track a message from the same channel`() {
        val regex = ".*ello.*[wW]orl.*"
        val channel = "#channel"


        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, channel).join()

        val bot1 = bots.bot().join()
        bot1.join(channel).join()
        bot1.beginCount(channel, regex) .join()
        val bot2 = bots.bot().join()
        bot2.join(channel).join()
        bot2.beginCount(channel, regex) .join()

        val token = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(token, channel).join()
        courseApp.channelSend(token, channel, messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()).join()

        assertEquals(1L, bots.bot("Anna0").thenCompose { bot -> bot.count(channel = channel,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna1").thenCompose { bot -> bot.count(channel = channel,regex =  regex) }.join())
    }

    @Test
    @Order(21)
    fun `A user in the channel can ask the bot to do calculation`() {
        val messages = mutableListOf<String>()
        val listener: ListenerCallback = { _, message ->
            messages.add(message.contents.toString(Charsets.UTF_8))
            CompletableFuture.completedFuture(Unit)
        }


        courseApp.login("gal", "hunter2")
                .thenCompose { adminToken ->
                    courseApp.channelJoin(adminToken, "#channel")
                            .thenCompose {
                                bots.bot().thenCompose { bot ->
                                    bot.join("#channel")
                                            .thenApply { bot.setCalculationTrigger("calculate") }
                                }
                            }
                            .thenCompose { courseApp.login("matan", "s3kr3t") }
                            .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                            .thenCompose { token -> courseApp.addListener(token, listener).thenApply { token } }
                            .thenCompose { token -> courseApp.channelSend(token, "#channel", messageFactory.create(MediaType.TEXT, "calculate 20 * 2 + 2".toByteArray(Charsets.UTF_8)).join()) }
                }.join()
        assertEquals(2, messages.size)
        assertEquals(42, messages[1])
    }





}