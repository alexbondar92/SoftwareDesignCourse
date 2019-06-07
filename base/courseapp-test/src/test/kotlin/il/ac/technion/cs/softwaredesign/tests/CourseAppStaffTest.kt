package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
import il.ac.technion.cs.softwaredesign.exceptions.UserNotAuthorizedException
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration.ofSeconds
import java.util.concurrent.CompletableFuture

class CourseAppStaffTest {
    private val injector = Guice.createInjector(CourseAppModule(), SecureStorageModule())

    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private val messageFactory = injector.getInstance<MessageFactory>()

    @Test
    fun `after login, a user is logged in`() {
        val token = courseApp.login("gal", "hunter2")
                .thenCompose { courseApp.login("imaman", "31337") }
                .thenCompose { courseApp.login("matan", "s3kr1t") }
                .join()

        assertThat(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal").join() },
                present(isTrue))
    }

    @Test
    fun `an authentication token is invalidated after logout`() {
        val token = courseApp.login("matan", "s3kr1t")
                .thenCompose { token -> courseApp.logout(token).thenApply { token } }
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(ofSeconds(10)) {
                courseApp.isUserLoggedIn(token, "matan").joinException()
            }
        }
    }

    @Test
    fun `administrator can create channel and is a member of it`() {
        val administratorToken = courseApp.login("admin", "admin")
                .thenCompose { token -> courseApp.channelJoin(token, "#mychannel").thenApply { token } }
                .join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(administratorToken, "#mychannel", "admin").join()
        }, isTrue)
    }

    @Test
    fun `non-administrator can not make administrator`() {
        val nonAdminToken = courseApp.login("admin", "admin")
                .thenCompose { courseApp.login("matan", "1234") }
                .thenCompose { token -> courseApp.login("gal", "hunter2").thenApply { token } }
                .join()

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(ofSeconds(10)) {
                courseApp.makeAdministrator(nonAdminToken, "gal").joinException()
            }
        }
    }

    @Test
    fun `non-administrator can join existing channel and be made operator`() {
        val adminToken = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("matan", "1234")
                            .thenApply { Pair(adminToken, it) }
                }
                .thenCompose { (adminToken, otherToken) ->
                    courseApp.channelJoin(adminToken, "#test")
                            .thenCompose { courseApp.channelJoin(otherToken, "#test") }
                            .thenCompose { courseApp.channelMakeOperator(adminToken, "#test", "matan") }
                            .thenApply { adminToken }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#mychannel", "matan").join()
        }, isTrue)
    }

    @Test
    fun `user is not in channel after parting from it`() {
        val (adminToken, _) = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("matan", "1234").thenApply { Pair(adminToken, it) }
                }.thenCompose { pair ->
                    val (adminToken, otherToken) = pair
                    courseApp.channelJoin(adminToken, "#mychannel")
                            .thenCompose { courseApp.channelJoin(otherToken, "#mychannel") }
                            .thenCompose { courseApp.channelPart(otherToken, "#mychannel") }
                            .thenApply { pair }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#mychannel", "matan").join()
        }, isFalse)
    }

    @Test
    fun `user is not in channel after being kicked`() {
        val (adminToken, _) = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("matan", "4321").thenApply { Pair(adminToken, it) }
                }.thenCompose { pair ->
                    val (adminToken, otherToken) = pair
                    courseApp.channelJoin(adminToken, "#236700")
                            .thenCompose { courseApp.channelJoin(otherToken, "#236700") }
                            .thenCompose { courseApp.channelKick(adminToken, "#236700", "matan") }
                            .thenApply { pair }
                }.join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#236700", "matan").join()
        }, isFalse)
    }

    @Test
    fun `total user count in a channel is correct with a single user`() {
        val adminToken = courseApp.login("admin", "admin")
                .thenCompose { token -> courseApp.channelJoin(token, "#test").thenApply { token } }
                .join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.numberOfTotalUsersInChannel(adminToken, "#test").join()
        }, equalTo(1L))
    }

    @Test
    fun `active user count in a channel is correct with a single user`() {
        val adminToken = courseApp.login("admin", "admin")
                .thenCompose { token -> courseApp.channelJoin(token, "#test").thenApply { token } }
                .join()

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.numberOfActiveUsersInChannel(adminToken, "#test").join()
        }, equalTo(1L))
    }

    @Test
    fun `logged in user count is correct when no user is logged in`() {
        assertThat(
                runWithTimeout(ofSeconds(10)) { courseAppStatistics.loggedInUsers().join() },
                equalTo(0L))
    }

    @Test
    fun `total user count is correct when no users exist`() {
        assertThat(
                runWithTimeout(ofSeconds(10)) { courseAppStatistics.totalUsers().join() },
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

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10ChannelsByUsers().join(),
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

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10ActiveChannelsByUsers().join(),
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

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10UsersByChannels().join(),
                    containsElementsInOrder("admin", "matan"))
        }
    }

    @Test
    fun `private message received successfully`() {
        val listener = mockk<ListenerCallback>()
        every { listener(any(), any()) }.returns(CompletableFuture.completedFuture(Unit))

        val (token, message) = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("gal", "hunter2").thenApply { Pair(adminToken, it) }
                }.thenCompose { (adminToken, nonAdminToken) ->
                    courseApp.addListener(nonAdminToken, listener)
                            .thenCompose { messageFactory.create(MediaType.TEXT, "hello, world\n".toByteArray()) }
                            .thenApply { message -> Pair(adminToken, message) }
                }.join()

        runWithTimeout(ofSeconds(10)) {
            courseApp.privateSend(token, "gal", message).join()
            assertEquals(0, courseAppStatistics.pendingMessages().join())
        }

        verify {
            listener(match { it == "@admin" },
                    match { it.contents contentEquals "hello, world\n".toByteArray() })
        }
        confirmVerified(listener)
    }

    @Test
    fun `channel message received successfully`() {
        val listener = mockk<ListenerCallback>()
        every { listener(any(), any()) }.returns(CompletableFuture.completedFuture(Unit))

        val (token, message) = courseApp.login("admin", "admin")
                .thenCompose { adminToken ->
                    courseApp.login("gal", "hunter2").thenApply { Pair(adminToken, it) }
                }.thenCompose { (adminToken, userToken) ->
                    courseApp.addListener(userToken, listener)
                            .thenCompose { courseApp.channelJoin(adminToken, "#jokes") }
                            .thenCompose { courseApp.channelJoin(userToken, "#jokes") }
                            .thenCompose { messageFactory.create(MediaType.TEXT, "why did the chicken cross the road?".toByteArray()) }
                            .thenApply { message -> Pair(adminToken, message) }
                }.join()

        runWithTimeout(ofSeconds(10)) { courseApp.channelSend(token, "#jokes", message).join() }

        verify {
            listener(match { it == "#jokes@admin" },
                    match { it.contents contentEquals "why did the chicken cross the road?".toByteArray() })
        }
        confirmVerified(listener)
    }

    @Test
    fun `there is 1 pending message after privateSend with no listener`() {
        courseApp.login("admin", "admin")
                .thenCompose { courseApp.login("gal", "hunter2") }
                .thenCompose { userToken ->
                    messageFactory.create(MediaType.TEXT, "how many programmers does it take to change a light-bulb?".toByteArray())
                            .thenCompose { msg -> courseApp.privateSend(userToken, "admin", msg) }
                }.join()

        assertEquals(1, runWithTimeout(ofSeconds(10)) { courseAppStatistics.pendingMessages().join() })
    }
}