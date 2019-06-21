package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds

class CourseBotStaffTest {
    private val injector = Guice.createInjector(CourseAppModule(), CourseBotModule(), SecureStorageModule())

    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    private val courseApp = injector.getInstance<CourseApp>()
    private val bots = injector.getInstance<CourseBots>()
    private val messageFactory = injector.getInstance<MessageFactory>()

    @Test
    fun `Can create a bot and add make it join channels`() {
        val token = courseApp.login("gal", "hunter2").join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            val bot = courseApp.channelJoin(token, "#channel")
                    .thenCompose { bots.bot() }
                    .join()
            bot.join("#channel").join()
            bot.channels().join()
        }, equalTo(listOf("#channel")))
    }

    @Test
    fun `Can list bots in a channel`() {
        courseApp.login("gal", "hunter2")
                .thenCompose { adminToken ->
                    courseApp.channelJoin(adminToken, "#channel")
                            .thenCompose { bots.bot().thenCompose { it.join("#channel") } }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            bots.bots("#channel").join()
        }, equalTo(listOf("Anna0")))
    }

    @Test
    fun `A user in the channel can ask the bot to do calculation`() {
        val listener = mockk<ListenerCallback>(relaxed = true)

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
                            .thenCompose { token -> courseApp.channelSend(token, "#channel", messageFactory.create(MediaType.TEXT, "calculate 20 * 2 + 2".toByteArray()).join()) }
                }.join()

        verify {
            listener.invoke("#channel@matan", any())
            listener.invoke("#channel@Anna0", match { it.contents.toString().toInt() == 42 })
        }
    }

    @Test
    fun `A user in the channel can tip another user`() {
        courseApp.login("gal", "hunter2")
                .thenCompose { adminToken ->
                    courseApp.channelJoin(adminToken, "#channel")
                            .thenCompose {
                                bots.bot()
                                        .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                                        .thenCompose { bot -> bot.setTipTrigger("tip") }
                            }
                            .thenCompose { courseApp.login("matan", "s3kr3t") }
                            .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                            .thenCompose { token -> courseApp.channelSend(token, "#channel", messageFactory.create(MediaType.TEXT, "tip 10 gal".toByteArray()).join()) }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            bots.bot("Anna0")
                    .thenCompose { it.richestUser("#channel") }
                    .join()
        }, present(equalTo("gal")))
    }

    @Test
    fun `The bot accurately tracks keywords`() {
        courseApp.login("gal", "hunter2")
                .thenCompose { adminToken ->
                    courseApp.channelJoin(adminToken, "#channel")
                            .thenCompose {
                                bots.bot()
                                        .thenCompose { bot -> bot.join("#channel").thenApply { bot } }
                                        .thenCompose { bot -> bot.beginCount(".*ello.*[wW]orl.*") }
                            }
                            .thenCompose { courseApp.login("matan", "s3kr3t") }
                            .thenCompose { token -> courseApp.channelJoin(token, "#channel").thenApply { token } }
                            .thenCompose { token -> courseApp.channelSend(token, "#channel", messageFactory.create(MediaType.TEXT, "hello, world!".toByteArray()).join()) }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            bots.bot("Anna0").thenCompose { bot -> bot.count(".*hell.*worl.*") }.join()
        }, equalTo(1L))
    }

    @Test
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

        assertThat(runWithTimeout(ofSeconds(10)) {
            val survey = bot.runSurvey("#channel", "What is your favorite flavour of ice-cream?",
                    listOf("Cranberry",
                            "Charcoal",
                            "Chocolate-chip Mint")).join()
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(regularUserToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            courseApp.channelSend(adminToken, "#channel", messageFactory.create(MediaType.TEXT, "Chocolate-chip Mint".toByteArray()).join())
            bot.surveyResults(survey).join()
        }, containsElementsInOrder(0L, 0L, 2L))
    }
}