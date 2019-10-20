package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo

import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.exceptions.NoSuchEntityException
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.old.courseapp.CourseAppModule
import il.ac.technion.cs.softwaredesign.tests.old.courseapp.FakeSecureStorageModule


import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Order

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture


import com.authzee.kotlinguice4.getInstance
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.messages.Message
import io.mockk.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun `2 bots track a message from the same channel`() {  //TODO
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

        //assertEquals(1L, bots.bot("Anna0").thenCompose { bot -> bot.count(channel = channel,regex =  regex) }.join())
        assertEquals(1L, bots.bot("Anna1").thenCompose { bot -> bot.count(channel = channel,regex =  regex) }.join())
    }

    @Test
    @Order(21)
    fun `A user in the channel can ask the bot to do calculation`() {//TODO
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
                            .thenCompose { token -> courseApp.channelSend(token, "#channel", messageFactory.create(MediaType.TEXT, "calculate 20 * 2 + 1".toByteArray(Charsets.UTF_8)).join()) }
                }.join()
        assertEquals(2, messages.size)
        assertEquals(41.0, messages[0].toDouble())
    }

    @Test
    @Order(22)
    fun `A user in the channel can tip another user`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()
        courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())

        val res = bot.richestUser("#channel").join()

        assertEquals("gal", res)
    }

    @Test
    @Order(23)
    fun `A user in the channel set trigger and gets old trigger null when first activated`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        val resString = bot.setTipTrigger("tip").join()

        assertEquals(null, resString)
    }

    @Test
    @Order(24)
    fun `A user in the channel set trigger and gets old trigger after previously set`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val oldTrigger = bot.setTipTrigger("tippy").join()
        assertEquals("tip", oldTrigger)
    }

    @Test
    @Order(25)
    fun `richestUser activated on a bad channel - bot is not in channel should throw exception`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        courseApp.channelJoin(adminToken, "#channel2").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()
        courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())

        assertThrows<NoSuchEntityException>{bot.richestUser("channel2")}
        assertThrows<NoSuchEntityException>{bot.richestUser("notExistChannel")}
    }


    @Test
    @Order(26)
    fun `seenTime returns null if a message by user was never seen and checks simple time ordering`() {
        val beforeTime = LocalDateTime.now()
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        courseApp.channelJoin(adminToken, "#channel2").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()
        courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())

        assertEquals(null, bot.seenTime("gal").join())
        assertNotNull( bot.seenTime("matan").join())
        val afterTime = LocalDateTime.now()
        assertTrue(bot.seenTime("matan").join()!! >= beforeTime)
        assertTrue(bot.seenTime("matan").join()!! <= afterTime)

    }

    @Test
    @Order(27)
    fun `mostActiveUser activated with a channel that bot is not a member of, throws exception`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        courseApp.channelJoin(adminToken, "#channel2").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()
       // courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())

        assertThrows<NoSuchEntityException> {bot.mostActiveUser("#channel2")}
        assertThrows<NoSuchEntityException> {bot.mostActiveUser("#notExistChannel")}

    }

    @Test
    @Order(28)
    fun `mostActiveUser activated with a channel that was not used for messaging, bot is a member, returns null`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        courseApp.channelJoin(adminToken, "#channel2").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()

        assertEquals(null, bot.mostActiveUser("#channel").join())
    }

    @Test
    @Order(29)
    fun `mostActiveUser returns most active user when user is in 2 channels`() {
        val adminToken = courseApp.login("gal", "hunter2").get()
        courseApp.channelJoin(adminToken, "#channel").get()
        courseApp.channelJoin(adminToken, "#channel2").get()
        val bot = bots.bot().get()
        bot.join("#channel").get()
        bot.setTipTrigger("tip").join()
        bot.join("#channel2").get()
        val otherToken = courseApp.login("matan", "s3kr3t").join()
        courseApp.channelJoin(otherToken, "#channel").join()
        courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())
        courseApp.channelSend(otherToken, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())

        val otherToken2 = courseApp.login("user2", "s3kr3t").join()
        courseApp.channelJoin(otherToken2, "#channel").join()
        courseApp.channelJoin(otherToken2, "#channel2").join()
        courseApp.channelSend(otherToken2, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())
        courseApp.channelSend(otherToken2, "#channel2", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())
        courseApp.channelSend(otherToken2, "#channel2", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join())
        //first user send 2 messages in "#channel", second user send 1 message in "#channel" and 2 messages in "#channel2"

        assertEquals("matan", bot.mostActiveUser("#channel").join())
        assertEquals("user2", bot.mostActiveUser("#channel2").join())
    }

    @Test
    @Order(30)
    fun `A user in the channel can ask the bot to do a survey`() {
        val adminToken = courseApp.login("gal", "hunter2")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val regularUserToken = courseApp.login("matan", "s3kr3t")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val bot = bots.bot()
                .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                .join()

        assertDoesNotThrow {
            val survey = bot.runSurvey("#channel", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(regularUserToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            assertEquals(mutableListOf(0L, 0L, 2L) ,bot.surveyResults(survey).join())
        }

    }

    @Test
    @Order(31)
    fun `A user in the channel can ask the bot to do a survey, change vote successfully`() {
        val adminToken = courseApp.login("gal", "hunter2")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val regularUserToken = courseApp.login("matan", "s3kr3t")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val bot = bots.bot()
                .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                .join()

        assertDoesNotThrow {
            val survey = bot.runSurvey("#channel", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(regularUserToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Cranberry".toByteArray()).join())
            assertEquals(mutableListOf(1L, 0L, 1L) ,bot.surveyResults(survey).join())
        }
    }

    @Test
    @Order(32)
    fun `A user in the channel can ask the bot to do a survey, change all votes successfully`() {
        val adminToken = courseApp.login("gal", "hunter2")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val regularUserToken = courseApp.login("matan", "s3kr3t")
                .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                .join()
        val bot = bots.bot()
                .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                .join()

        assertDoesNotThrow {
            val survey = bot.runSurvey("#channel", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(regularUserToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Cranberry".toByteArray()).join())
            courseApp.channelSend(regularUserToken, "#channel", messageFactory.create(MediaType.TEXT, "Charcoal".toByteArray()).join())
            assertEquals(mutableListOf(1L, 1L, 0L) ,bot.surveyResults(survey).join())
        }
    }

    @Test
    @Order(33)
    fun `runSurvey on a channel bot is not a member of`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        courseApp.channelJoin(adminToken, "#channel2").join()
        val bot = bots.bot()
                .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                .join()

        assertThrows<NoSuchEntityException> {
            bot.runSurvey("#channel2", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()

        }
        assertThrows<NoSuchEntityException> {
            bot.runSurvey("#notExist", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()

        }
    }

    @Test
    @Order(34)
    fun `surveyResults on a bad identifier throws exception`() {
        val adminToken = courseApp.login("gal", "hunter2").join()
        courseApp.channelJoin(adminToken, "#channel").join()
        courseApp.channelJoin(adminToken, "#channel2").join()
        val bot = bots.bot()
                .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                .join()


        val survey = bot.runSurvey("#channel", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()



        assertThrows<NoSuchEntityException> {
            bot.surveyResults("wrong$survey").join()

        }
    }

//    @Test
//    @Order(35)
//    fun `A bot successfully joins a channel`() {
//        val channel = "#channel"
//        val channels = bots.bot("John")
//                .thenCompose { bot -> bot.join(channel).thenApply { bot } }
//                .thenCompose { bot -> bot.channels() }.join()
//
//
//        assertThat(channels, equalTo(listOf(channel)))
//    }
//
//    @Test
//    @Order(36)
//    fun `throws NoSuchEntityException if the channel cant be joined or parted`() {
//
//        val channel = "#channel"
//        val botName = "John"
//
//        // Cant part a non-joined channel
//        assertThrows<NoSuchEntityException> {
//            bots.bot(botName)
//                    .thenCompose { bot -> bot.part(channel) }
//                    .joinException()
//        }
//
//        // Can't part a non-existing channel
//        assertThrows<NoSuchEntityException> {
//            bots.bot(botName)
//                    .thenCompose { bot -> bot.join(channel).thenApply { bot } }
//                    .thenCompose { bot -> bot.part("illegal") }
//                    .joinException()
//        }
//
//        // Can't part an already parted channel
//        assertThrows<NoSuchEntityException> {
//            bots.bot(botName)
//                    .thenCompose { bot ->
//                        bot.join(channel)
//                                .thenApply { bot }
//                                .thenCompose {
//                                    bot.part(channel)
//                                    bot.part(channel)
//                                }
//                    }
//                    .joinException()
//        }
//    }
//
//    @Test
//    @Order(37)
//    fun `A bot successfully leaves a channel`() {
//        val channel = "#channel"
//        val channels = bots.bot("John")
//                .thenCompose { bot -> bot.join(channel).thenApply { bot } }
//                .thenCompose { bot -> bot.part(channel).thenApply { bot } }
//                .thenCompose { bot -> bot.channels() }.join()
//
//        assertThat(channels, equalTo(listOf()))
//    }
//
//    @Test
//    @Order(38)
//    fun `Returns the right amount of channels per bot`() {
//        val numChannels = (1..20).shuffled().first()
//
//        val bot = bots.bot("Join").join()
//        val channelSet: MutableSet<String> = mutableSetOf()
//
//        for (i in (1..numChannels)) {
//            val name = "#SDFSDFZXCVSDFS###____DASD_AS_D###"
//            bot.join(name).thenApply { channelSet.add(name) }.join()
//        }
//
//        assertThat(bot.channels().join().toSet(), equalTo(channelSet.toSet()))
//    }
//
//    @Test
//    @Order(39)
//    fun `throws IllegalArgumentException if regex and mediaType are both null`() {
//
//        val bot = bots.bot("Join").join()
//        val channel1 = "#channel1"
//
//        assertThrows<IllegalArgumentException> {
//            bot.beginCount(channel1).joinException()
//        }
//    }
//
//    @Test
//    @Order(40)
//    fun `throws IllegalArgumentException if a begin count has not been registered with the provided arguments`() {
//        val bot = bots.bot("Join").join()
//        val channel1 = "#channel1"
//
//        assertThrows<IllegalArgumentException> {
//            bot.count(channel1).joinException()
//        }
//    }
//
//    @Test
//    @Order(41)
//    fun `Returns the correct number when count function is called`() {
//        val bot = bots.bot("Join").join()
//        val channel = "#channel"
//        val regex = "@[0-9a-z]*"
//        val user = courseApp.login("user", "1234").join()
//
//        bot.join(channel)
//                .thenCompose { bot.beginCount(channel, regex, MediaType.TEXT) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "@hello10".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.LOCATION, "@hello10".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "other message".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "@12world21".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .join()
//
//        assertThat(bot.count(channel, regex, MediaType.TEXT).join(), equalTo(2L))
//    }
//
//    @Test
//    @Order(42)
//    fun `counts for every channel if channel parameter is null`() {
//        val bot = bots.bot("Join").join()
//        val channel1 = "#channel1"
//        val channel2 = "#channel2"
//        val message1 = "test"
//
//        val user1 = courseApp.login("user1", "1234").join()
//        val user2 = courseApp.login("user2", "1234").join()
//
//        bot.join(channel1)
//                .thenCompose { bot.join(channel2) }
//                .thenCompose { courseApp.channelJoin(user1, channel1) }
//                .thenCompose { courseApp.channelJoin(user2, channel2) }
//                .thenCompose { bot.beginCount(null, message1) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user1, channel1, msg) }
//                .thenCompose { messageFactory.create(MediaType.LOCATION, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user2, channel2, msg) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "other message".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user1, channel1, msg) }
//                .join()
//
//        assertThat(bot.count(regex = message1).join(), equalTo(2L))
//    }
//
//    @Test
//    @Order(43)
//    fun `regex or mediaType as wildcards works properly`() {
//        val bot = bots.bot("Join").join()
//        val channel = "#channel"
//        val message1 = "test"
//        val message2 = "other test"
//
//        val user = courseApp.login("user", "1234").join()
//
//        bot.join(channel)
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { bot.beginCount(channel, message1) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.LOCATION, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "other message".toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .join()
//
//        assertThat(bot.count(channel, message1).join(), equalTo(2L))
//
//        bot.beginCount(channel, mediaType = MediaType.TEXT).join()
//
//        assertThat(bot.count(channel, mediaType = MediaType.TEXT).join(), equalTo(0L))
//
//        messageFactory.create(MediaType.TEXT, message1.toByteArray())
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.LOCATION, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.AUDIO, message1.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, message2.toByteArray()) }
//                .thenCompose { msg -> courseApp.channelSend(user, channel, msg) }
//
//        assertThat(bot.count(channel, mediaType = MediaType.TEXT).join(), equalTo(2L))
//    }
//
//    @Test
//    @Order(44)
//    fun `A calculation trigger successfully evaluates an arithmetic expression`() {
//        val bot = bots.bot("MathProfessor").join()
//        val channel = "#math"
//
//        val user = courseApp.login("user", "1234").join()
//
//        val callback: ListenerCallback = mockk(relaxed = true)
//        val msg = slot<Message>()
//        every {
//            callback(any(), any())
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//        every {
//            callback("#math@MathProfessor", capture(msg))
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//
//        courseApp.addListener(user, callback)
//
//        bot.setCalculationTrigger("calculate")
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "calculate (    2 * (8- 4) / 2 - 2 / 1)".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .join()
//
//
//        verify { callback(match { it == "#math@MathProfessor" }, any()) }
//        assertThat(String(msg.captured.contents), equalTo("2.0"))
//    }
//
//    @Test
//    @Order(45)
//    fun `A calculation trigger is overridden when set for a second time`() {
//        val bot = bots.bot("MathProfessor").join()
//        val channel = "#math"
//
//        val user = courseApp.login("user", "1234").join()
//
//        val callback: ListenerCallback = mockk(relaxed = true)
//        val msg = slot<Message>()
//        every {
//            callback(any(), any())
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//        every {
//            callback("#math@MathProfessor", capture(msg))
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//
//        courseApp.addListener(user, callback)
//
//        var previousPhrase: String? = null
//
//        bot.setCalculationTrigger("calculate")
//                .thenCompose { bot.setCalculationTrigger("evaluate").thenApply { previousPhrase = it } }
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "evaluate 3 + 4".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "calculate (    2 * (8- 4) / 2 - 2 / 1)".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .join()
//
//
//        assertThat(previousPhrase, equalTo("calculate"))
//        verify { callback(match { it == "#math@MathProfessor" }, any()) }
//        assertThat(String(msg.captured.contents), equalTo("2.0"))
//    }
//
//    @Test
//    @Order(46)
//    fun `Calculator mode is disabled when called with null trigger`() {
//        val bot = bots.bot("MathProfessor").join()
//        val channel = "#math"
//
//        val user = courseApp.login("user", "1234").join()
//
//        val callback: ListenerCallback = mockk(relaxed = true)
//        val msg = slot<Message>()
//        every {
//            callback(any(), any())
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//        every {
//            callback("#math@MathProfessor", capture(msg))
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//
//        courseApp.addListener(user, callback)
//
//        var previousPhrase: String? = null
//
//        bot.setCalculationTrigger("calculate")
//                .thenCompose { bot.setCalculationTrigger(null).thenApply { previousPhrase = it } }
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "calculate (    2 * (8- 4) / 2 - 2 / 1)".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .join()
//
//        assertThat(previousPhrase, equalTo("calculate"))
//        verify { callback(match { it == "#math@MathProfessor" }, any()) wasNot Called }
//        assertThat(msg.isCaptured, equalTo(false))
//    }
//
//    @Test
//    @Order(47)
//    fun `tip system is working properly and richestUse is properly updated`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#shop"
//
//        val user = courseApp.login("user", "1234").join()
//        val otherUser = courseApp.login("otherUser", "1234").join()
//
//        bot.setTipTrigger("tip")
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { courseApp.channelJoin(otherUser, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 1 user".toByteArray()) }
//                // user = 1001
//                // otherUser = 999
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 500 otherUser".toByteArray()) }
//                // user = 501
//                // otherUser = 1499
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .join()
//
//        assertThat(bot.richestUser(channel).join(), equalTo("otherUser"))
//
//        messageFactory.create(MediaType.TEXT, "tip 1500 user".toByteArray())
//                // user = 501
//                // otherUser = 1499
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .join()
//
//        assertThat(bot.richestUser(channel).join(), equalTo("otherUser"))
//
//        messageFactory.create(MediaType.TEXT, "tip 1499 nonExistingUser".toByteArray())
//                // user = 501
//                // otherUser = 1499
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .join()
//
//        assertThat(bot.richestUser(channel).join(), equalTo("otherUser"))
//
//        messageFactory.create(MediaType.TEXT, "tip 1499 user".toByteArray())
//                // user = 501
//                // otherUser = 1499
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .join()
//
//        assertThat(bot.richestUser(channel).join(), equalTo("user"))
//    }
//
//    @Test
//    @Order(48)
//    fun `throws NoSuchEntityException if the bot is not in the provided channel`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#shop"
//
//        val user = courseApp.login("user", "1234").join()
//        val otherUser = courseApp.login("otherUser", "1234").join()
//
//        bot.setTipTrigger("tip")
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { courseApp.channelJoin(otherUser, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 1 user".toByteArray()) }
//                // user = 1001
//                // otherUser = 999
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 500 otherUser".toByteArray()) }
//                // user = 501
//                // otherUser = 1499
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .thenCompose { bot.part(channel) }
//                .join()
//
//        assertThrows<NoSuchEntityException> {
//            bot.richestUser(channel).joinException()
//        }
//    }
//
//    @Test
//    @Order(49)
//    fun `returns null if no tipping has occured`() {
//        val bot = bots.bot("Cashier").join()
//
//        val value = bot.setTipTrigger("tip")
//                .thenCompose { bot.join("#channel") }
//                .thenCompose { bot.richestUser("#channel") }
//                .join()
//
//        assertThat(value, equalTo<String>(null))
//    }
//
//    @Test
//    @Order(50)
//    fun `returns null if no one user is the richest`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#shop"
//
//        val user = courseApp.login("user", "1234").join()
//        val otherUser = courseApp.login("otherUser", "1234").join()
//
//        bot.setTipTrigger("tip")
//                .thenCompose { bot.join(channel) }
//                .thenCompose { courseApp.channelJoin(user, channel) }
//                .thenCompose { courseApp.channelJoin(otherUser, channel) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 1 user".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(otherUser, channel, m) }
//                .thenCompose { messageFactory.create(MediaType.TEXT, "tip 1 otherUser".toByteArray()) }
//                .thenCompose { m -> courseApp.channelSend(user, channel, m) }
//                .join()
//
//        assertThat(bot.richestUser(channel).join(), equalTo<String>(null))
//    }
//
//    @Test
//    @Order(51)
//    fun `returns the correct time when seenTime is called`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#talks"
//
//        val user1 = courseApp.login("user1", "1234").join()
//        val user2 = courseApp.login("user2", "1234").join()
//        val user3 = courseApp.login("user3", "1234").join()
//
//        // Ensure messages get different time stamps
//        val msg1 = messageFactory.create(MediaType.TEXT, "hello!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg2 = messageFactory.create(MediaType.TEXT, "world!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg3 = messageFactory.create(MediaType.TEXT, "third!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg4 = messageFactory.create(MediaType.TEXT, "foo!".toByteArray()).join()
//
//        bot.join(channel)
//                .thenCompose { courseApp.channelJoin(user1, channel) }
//                .thenCompose { courseApp.channelJoin(user2, channel) }
//                .thenCompose { courseApp.channelJoin(user3, channel) }
//                .thenCompose { courseApp.channelSend(user1, channel, msg1) }
//                .thenCompose { courseApp.channelSend(user2, channel, msg2) }
//                .thenCompose { courseApp.channelSend(user3, channel, msg3) }
//                .thenCompose { courseApp.channelSend(user3, channel, msg4) }
//                .join()
//
//        assertThat(bot.seenTime("user1").join()
//                , equalTo(msg1.created))
//        assertThat(bot.seenTime("user2").join()
//                , equalTo(msg2.created))
//        assertThat(bot.seenTime("user3").join()
//                , equalTo(msg4.created))
//
//    }
//
//    @Test
//    @Order(52)
//    fun `returns null if the user has not sent any messages or the user does not exist`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#talks"
//
//        val user1 = courseApp.login("user1", "1234").join()
//        bot.join(channel)
//                .thenCompose { courseApp.channelJoin(user1, channel) }
//                .join()
//
//        assertThat(bot.seenTime("user1").join(), equalTo<LocalDateTime?>(null))
//        assertThat(bot.seenTime("non_existing").join(), equalTo<LocalDateTime?>(null))
//    }
//
//    @Test
//    @Order(53)
//    fun `returns the correct user when mostActiveUser is called`() {
//        val bot = bots.bot("Cashier").join()
//        val channel = "#talks"
//
//        val user1 = courseApp.login("user1", "1234").join()
//        val user2 = courseApp.login("user2", "1234").join()
//        val user3 = courseApp.login("user3", "1234").join()
//
//        // Ensure messages get different time stamps
//        val msg1 = messageFactory.create(MediaType.TEXT, "hello!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg2 = messageFactory.create(MediaType.TEXT, "world!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg3 = messageFactory.create(MediaType.TEXT, "third!".toByteArray()).join()
//        Thread.sleep(10)
//        val msg4 = messageFactory.create(MediaType.TEXT, "foo!".toByteArray()).join()
//
//        bot.join(channel)
//                .thenCompose { courseApp.channelJoin(user1, channel) }
//                .thenCompose { courseApp.channelJoin(user2, channel) }
//                .thenCompose { courseApp.channelJoin(user3, channel) }
//                .thenCompose { courseApp.channelSend(user1, channel, msg1) }
//                .thenCompose { courseApp.channelSend(user2, channel, msg2) }
//                .thenCompose { courseApp.channelSend(user3, channel, msg3) }
//                .join()
//
//        assertThat(bot.mostActiveUser(channel).join(), equalTo<String?>(null))
//
//        courseApp.channelSend(user2, channel, msg4)
//
//        assertThat(bot.mostActiveUser(channel).join(), equalTo("user2"))
//    }
//
//    @Test
//    @Order(54)
//    fun `Throws NoSuchEntityException if the surveying bot is not a part of the channel`() {
//        val bot = bots.bot("Join").join()
//        val channel1 = "#channel1"
//
//        val question = "What is the meaning of life?"
//        val a1 = "42"
//        val a2 = "The ultimate answer"
//        val a3 = "Taub"
//        val a4 = "All of the above"
//        val answers = listOf(a1, a2, a3, a4)
//
//        assertThrows<NoSuchEntityException> { bot.runSurvey(channel1, question, answers).joinException() }
//    }
//
//    @Test
//    fun `Throws NoSuchEntityException if a survey does not exist`() {
//        val bot = bots.bot("John").join()
//        assertThrows<NoSuchEntityException> { bot.surveyResults("invalid_survey").joinException() }
//    }
//
//    private fun runTestSurvey(): Triple<CourseBot, String, List<Long>> {
//        val bot = bots.bot("John").join()
//        val channel1 = "#facebook"
//
//        val user1 = courseApp.login("user1", "1234").join()
//        val user2 = courseApp.login("user2", "1234").join()
//
//        val question = "What is the meaning of life?"
//        val a1 = "42"
//        val a2 = "The ultimate answer"
//        val a3 = "Taub"
//        val a4 = "All of the above"
//        val answers = listOf(a1, a2, a3, a4)
//
//        val callback: ListenerCallback = mockk(relaxed = true)
//        val msg = slot<Message>()
//        every {
//            callback(any(), any())
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//
//        every {
//            callback("#facebook@John", capture(msg))
//        } answers {
//            CompletableFuture.completedFuture(Unit)
//        }
//
//        val survey = bot.join(channel1)
//                .thenForward { courseApp.channelJoin(user1, channel1) }
//                .thenForward { courseApp.channelJoin(user2, channel1) }
//                .thenForward { courseApp.addListener(user1, callback) }
//                .thenForward { courseApp.addListener(user2, callback) }
//                .thenCompose { bot.runSurvey(channel1, question, answers) }
//                .join()
//
//        verify(exactly = 2) { callback(match { it == "#facebook@John" }, any()) }
//        assertThat(String(msg.captured.contents), equalTo(question))
//
//        val results = courseApp.channelSend(user1, channel1, messageFactory.create(MediaType.TEXT, a1.toByteArray()).join())
//                .thenCompose { courseApp.channelSend(user2, channel1, messageFactory.create(MediaType.TEXT, a2.toByteArray()).join()) }
//                .thenCompose { courseApp.channelSend(user2, channel1, messageFactory.create(MediaType.TEXT, a4.toByteArray()).join()) }
//                .thenCompose { bot.surveyResults(survey) }
//                .join()
//
//        return Triple(bot, survey, results)
//    }
//
//    @Test
//    fun `successfully starts a survey and returns the results`() {
//        val (_, _, results) = runTestSurvey()
//        assertThat(results, equalTo(listOf(1L, 0L, 0L, 1L)))
//    }
//
//    @Test
//    fun `if a bot leaves a channel the survey stays but the results are reset`() {
//        val (bot, survey, results) = runTestSurvey()
//        assertThat(results, equalTo(listOf(1L, 0L, 0L, 1L)))
//
//        val newResults = bot.part("#facebook")
//                .thenCompose { bot.surveyResults(survey) }
//                .join()
//
//        assertThat(newResults, equalTo(listOf(0L, 0L, 0L, 0L)))
//    }
}
