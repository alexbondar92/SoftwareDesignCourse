package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import com.authzee.kotlinguice4.getInstance
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.tests.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.assertThrows
import java.nio.charset.Charset
import java.time.Duration
import java.time.Duration.ofSeconds
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class OtherTests3 {
    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())
    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()
    private val messageFactory = injector.getInstance<MessageFactory>()


    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    @Test
    fun `after login, a user is logged in`() {
        val token = courseApp.login("gal", "hunter2")
                .thenCompose { courseApp.login("imaman", "31337") }
                .thenCompose { courseApp.login("matan", "s3kr1t") }
                .join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal").join() },
                present(isTrue))
    }

    @Test
    fun `throws NoSuchEntityException after login with wrong password`(){
        val username="gal"
        val password="gal_password"
        courseApp.login(username, password)
                .thenCompose { token -> courseApp.login("aviad", "shiber!$75").thenApply { token } }
                .thenCompose { courseApp.logout(it) }
                .join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.login(username, "wrong_password").joinException() }
        }
    }

    @Test
    fun `throws UserAlreadyLoggedInException after re-login`(){
        val username="gal"
        val password="gal_password"
        courseApp.login(username, password)
                .thenCompose { token -> courseApp.login("aviad", "shiber!$75").thenApply { token } }
                .join()

        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.login(username, password).joinException() }
        }
    }

    @Test
    fun `throws InvalidTokenException after logout with invalid token`(){
        val username="aviad"
        val password="aviad_password"
        val ronToken = courseApp.login(username, password)
                .thenCompose { courseApp.login("ron", password)}
                .thenCompose { t -> courseApp.logout(t).thenApply { t } }
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout("").joinException() }
        }
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout("bad_token").joinException() }
        }
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout(ronToken).joinException() }
        }
    }

    /**
     * the test checks that after registering to the system we can login again after logout
     * also the test CHECKS with exhaustive search that no assumptions are made
     * regarding the password & username charSet.
     */
    @Test
    fun `login after register`(){
        val printableAsciiRange = ' '..'~'
        for(char in printableAsciiRange){
            val username= "Aviad$char"
            val password=username+"Password"
            val ronToken=courseApp.login(username,password).join()
            courseApp.logout(ronToken).join()
            courseApp.login(username,password).join()
        }
    }

    @Test
    fun `throws InvalidTokenException after checking user login status with invalid token`(){       // TODO ("that is really some shitty thing... they use the old token in new session")
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn("","notExistingUser").joinException()}
        }
        val username="ron"
        val password="ron_password"

        val ronToken = courseApp.login("aviad","aviad_password")
                .thenCompose { courseApp.login(username,password)}
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn("bad_token","notExistingUser").joinException()}
        }

        courseApp.logout(ronToken)
                .thenCompose { courseApp.login(username,password) }
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn(ronToken,username).joinException()}
        }
    }

    @Test
    fun `user login and then logout`(){
        val username="aviad"
        val password="aviad_password"
        val aviadToken= courseApp.login(username, password).join()
        val adminToken=courseApp.login("admin","123456").join()
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserLoggedIn(aviadToken, username).join() },
                present(equalTo(true)))
        courseApp.logout(aviadToken).join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserLoggedIn(adminToken, username).join() },
                present(equalTo(false)))
    }

    @Test
    fun `returns null if user does not exist`(){
        Assertions.assertNull(
                courseApp.login("aviad","aviad_password")
                        .thenCompose { courseApp.isUserLoggedIn(it,"notExsitingUser")}
                        .join()
        )
    }

    @Test
    fun `test regex`() {
        val channelMatch = "#dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val channelNoMatch = "dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val channelNoMatch2 = "#@dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val empty = ""
        val regex = Regex("((#)([0-9]|[a-z]|[A-Z]|(#)|(_))*)")
        assertThat(regex matches channelMatch, isTrue)
        assertThat(regex matches channelNoMatch, isFalse)
        assertThat(regex matches channelNoMatch2, isFalse)
        assertThat(regex matches empty, isFalse)
    }

    @Test
    fun `login exceptions`() {              // TODO ("the exception that is thrown is UserAlreadyLoggedInException but expected to be NoSuchEntityException")
        courseApp.login("admin", "admin").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.login("admin", "wrong_password").joinException() }
        }

        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.login("admin", "admin").joinException() }
        }
    }

    @Test
    fun `logout exceptions`() {
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.login("admin", "admin")
                        .thenCompose { courseApp.logout(it + "b") }
                        .joinException()
            }
        }
    }

    @Test
    fun `logout no exceptions`() {
        runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.login("admin", "admin")
                    .thenCompose { courseApp.logout(it) }
                    .join()
        }
    }

    @Test
    fun `isUserLoggedIn exceptions`() {
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(adminToken+adminToken, userToken).joinException() }
        }
    }

    @Test
    fun `test number of valid users`() {
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.totalUsers().join()
        },
                equalTo(0L))
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.makeAdministrator(adminToken, "user")
                    .thenCompose { courseApp.channelJoin(userToken, "#channel") }
                    .thenCompose { courseApp.channelJoin(adminToken, "#channel") }
                    .thenCompose { courseAppStatistics.totalUsers() }
                    .join()
        },
                equalTo(2L))

        val userToken2 = courseApp.login("user2", "user2_pas").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.totalUsers().join()
        },
                equalTo(3L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, "#channel")
                    .thenCompose { courseApp.logout(userToken2) }
                    .thenCompose { courseAppStatistics.totalUsers() }
                    .join()
        },
                equalTo(3L))
    }

    @Test
    fun `test number of valid active users`() {
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.loggedInUsers().join()
        },
                equalTo(0L))
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.makeAdministrator(adminToken, "user")
                    .thenCompose { courseApp.channelJoin(userToken, "#channel") }
                    .thenCompose { courseApp.channelJoin(adminToken, "#channel") }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        val userToken2 = courseApp.login("user2", "user2_pas").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.loggedInUsers().join()
        },
                equalTo(3L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, "#channel")
                    .thenCompose { courseApp.logout(userToken2) }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, "#channel")
                    .thenCompose { courseApp.channelPart(userToken, "#channel") }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(0L))
    }

    @Test
    fun `test numberOfActiveUsersInChannel exceptions`() {
        val channel = "#channel"
        val invalidToken = "invalidToken"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(invalidToken, channel).joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(adminToken, channel).joinException() }
        }
        courseApp.channelJoin(adminToken, channel)
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(userToken, channel).joinException() }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        val userToken2 = courseApp.login("second","pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken2, channel) }
                    .join()
        },
                equalTo(1L))
    }

    @Test
    fun `test numberOfTotalUsersInChannel exceptions`() {
        val channel = "#channel"
        val invalidToken = "invalidToken"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(invalidToken, channel).joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(adminToken, channel).joinException() }
        }
        courseApp.channelJoin(adminToken, channel).join()
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(userToken, channel).joinException() }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        val userToken2 = courseApp.login("second","pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken2, channel) }
                    .join()
        },
                equalTo(2L))
    }

    @Test
    fun `isUserInChannel test`(){
        val channel = "#channel"
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()
        val userToken2 = courseApp.login("user222", "user_pass222").join()


        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken, channel)
                    .thenCompose { courseApp.channelJoin(userToken, channel) }
                    .thenCompose { courseApp.isUserInChannel(userToken, channel, "admin") }
                    .join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, channel, "user").join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, channel, "user222").join()
        },
                isFalse)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(userToken, channel)
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isFalse)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.isUserInChannel(userToken2, channel, "user") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `channelKick exceptions`(){
        val channel = "#channel"
        val invalidToken = "invalidToken"

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(invalidToken, channel, "bl").joinException() }
        }

        val adminToken = courseApp.login("admin", "admin").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(adminToken, channel, "bl").joinException() }
        }
        val userToken = courseApp.login("user", "user_pass").join()

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(adminToken, channel)
                        .thenCompose { courseApp.channelJoin(userToken, channel) }
                        .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                        .joinException()
            }
        }


        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.login("bla", "bla")
                        .thenCompose { courseApp.channelKick(adminToken, channel, "bb") }
                        .joinException()
            }
        }
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(adminToken, channel, "bla").joinException() }
        }
    }

    @Test
    fun channelKickTest() {
        val channel = "#channel"
        val adminToken = courseApp.login("admin", "admin").join()
        courseApp.channelJoin(adminToken, channel).join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelKick(adminToken, channel, "user")
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isFalse)

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(userToken, channel)
                        .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                        .joinException()
            }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelMakeOperator(adminToken, channel, "user")
                    .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                    .thenCompose { courseApp.isUserInChannel(userToken, channel, "admin") }
                    .join()
        },
                isFalse)

        val userToken2 = courseApp.login("user222", "user2_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.channelKick(userToken, channel, "user") }
                    .thenCompose { courseApp.isUserInChannel(userToken2, channel, "user") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `channelMakeOperator exceptions`() {
        val channel = "#channel"
        val invalidToken = "token"
        val invalidChannel = "channel"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(invalidToken, channel, "user").joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(adminToken, channel)
                        .thenCompose { courseApp.channelMakeOperator(invalidToken, channel, "user") }
                        .joinException()
            }
        }
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(adminToken, invalidChannel, "admin").joinException() }
        }

        val userToken = courseApp.login("user", "user_pass").join()
        val userToken2 = courseApp.login("user222", "user222_pass").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(userToken, channel)
                        .thenCompose { courseApp.channelJoin(userToken2, channel) }
                        .thenCompose { courseApp.channelMakeOperator(adminToken, invalidChannel, "user") }
                        .joinException()
            }
        }

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken, channel, "user222").joinException() }
        }

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.makeAdministrator(adminToken, "user")
                        .thenCompose { courseApp.channelMakeOperator(userToken, channel, "user222") }
                        .joinException()
            }
        }
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken, channel, "b").joinException() }
        }

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelMakeOperator(userToken, channel, "user")
                        .thenCompose { courseApp.channelMakeOperator(userToken, channel, "user222") }
                        .thenCompose { courseApp.login("user22ddd2", "usedddr222_pass") }
                        .thenCompose { courseApp.channelMakeOperator(userToken2, channel, "b") }
                        .joinException()
            }
        }

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken2, channel, "user22ddd2").joinException() }
        }
    }

    @Test
    fun `user can join a channel and then leave`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)
        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(aviadToken,"#1")
                    .thenCompose { courseApp.isUserInChannel(adminToken,"#1","aviad") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `users join a channel, and the channel is destroyed when empty`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)

        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(aviadToken,"#1")
                    .thenCompose { courseApp.isUserInChannel(adminToken,"#1","aviad") }
                    .join()
        },
                isFalse)
        //channel should have destroyed by now, let's try to re-use his name without getting exception
        courseApp.channelPart(adminToken,"#1")
                .thenCompose { courseApp.channelJoin(adminToken,"#1") }
                .join()
    }

    @Test
    fun `channelPart throws InvalidTokenException If the auth token is invalid`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)

        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThrowsWithTimeout<Unit, InvalidTokenException>({ courseApp.channelPart("invalidToken","#1").joinException()})
    }

    @Test
    fun `channelPart throws NoSuchEntityException If token identifies a user who is not a member of channel, or channel does exist`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()
        courseApp.channelJoin(adminToken,"#1").join()
        courseApp.channelJoin(adminToken,"#2").join()
        courseApp.channelJoin(aviadToken,"#1").join()
        courseApp.channelJoin(ronToken,"#2").join()
        courseApp.logout(ronToken).join()
        //verify that the users joined
        assertThat(courseApp.isUserInChannel(aviadToken,"#1","aviad").join(), isTrue)
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)
        courseApp.channelPart(aviadToken,"#1").join()
        assertThat(courseApp.isUserInChannel(adminToken,"#1","aviad").join(), isFalse)
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.channelPart(aviadToken,"#1").joinException()})
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.channelPart(aviadToken,"#nonExistingChannel").joinException()})
    }

    @Test
    fun channelJoinTest() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()
        courseApp.channelJoin(adminToken,"#1").join()
        courseApp.channelJoin(aviadToken,"#1").join()
        courseApp.channelJoin(adminToken,"#2").join()
        courseApp.channelJoin(ronToken,"#2").join()
        courseApp.channelJoin(aviadToken,"#2").join()
        courseApp.logout(ronToken).join()
        assertThrowsWithTimeout<Unit, InvalidTokenException>({ courseApp.channelJoin(ronToken,"#nonExistingChannel").joinException()})
        assertThrowsWithTimeout<Unit, NameFormatException>({ courseApp.channelJoin(adminToken,"123#nonExistingChannel").joinException()})
        assertThrowsWithTimeout<Unit, NameFormatException>({ courseApp.channelJoin(adminToken,"badNaming").joinException()})
        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({ courseApp.channelJoin(aviadToken,"#notExistingChannel").joinException()})
    }

    @Test
    fun makeAdminTest() {
        val admin= courseApp.login("admin","admin").join()
        val aviad=courseApp.login("aviad","aviad123").join()
        val ron=courseApp.login("ron","ron123").join()

        assertThrowsWithTimeout<Unit, InvalidTokenException>({
            courseApp.makeAdministrator(admin,"aviad") //only admin can create a channel so let's call channel Join with the new admin
                    .thenCompose { courseApp.channelJoin(aviad,"#1") }
                    .thenCompose { courseApp.makeAdministrator("INVALIDToken","#1") }
                    .joinException()
        })

        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({ courseApp.makeAdministrator(ron,"ron").joinException()})
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.makeAdministrator(aviad,"NotExistingUser").joinException()})
        //we can even make logout users admins
        courseApp.logout(ron)
                .thenCompose { courseApp.makeAdministrator(admin,"ron") }
                .join()
    }

    @Test
    fun `numberOfTotalUsersInChannel get updated after join and part`(){
        val admin= courseApp.login("admin","admin").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(admin,"#1")
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(1L))

        (1..511).forEach{
            courseApp.login("$it","password")
                    .thenCompose { courseApp.channelJoin(it,"#1") }
                    .join()
        }
        assertThat(courseApp.numberOfTotalUsersInChannel(admin,"#1").join(), equalTo(512L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(admin,"#1")
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(511L))
    }

    @Test
    fun `numberOfActiveUsersInChannel get updated after join and part`(){
        val admin= courseApp.login("admin","admin").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(admin,"#1")
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(1L))

        lateinit var token:String
        (1..511).forEach{
            token=courseApp.login("$it","password").join()
            courseApp.channelJoin(token,"#1").join()
        }
        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(admin, "#1").join() }, equalTo(512L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(admin,"#1")
                    .thenCompose { courseApp.logout(token) }
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(admin, "#1") }
                    .join()
        },
                equalTo(510L))
    }

    @Test
    fun `making sure cant join operator more than once`() {
        val firstUser = "aviad"
        val aviad = courseApp.login(firstUser, "shiber").join()
        val secondUser = "ron"
        val ron = courseApp.login(secondUser, "ron").join()
        val channel = "#SoftwareDesign"

        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({
            courseApp.channelJoin(aviad, channel) //only admin can create a channel so let's call channel Join with the new admin
                    .thenCompose { courseApp.channelJoin(ron, channel) }
                    .thenCompose { courseApp.channelMakeOperator(aviad, channel, secondUser) }
                    .thenCompose { courseApp.channelMakeOperator(aviad, channel, secondUser) }
                    .thenCompose { courseApp.channelMakeOperator(ron, channel, secondUser) }
                    .thenCompose { courseApp.channelPart(ron, channel) }
                    .thenCompose { courseApp.channelJoin(ron, channel) }
                    //an Exception should be thrown if because
                    //ron should  not an operator anymore(maybe the test were able to add it twice)
                    .thenCompose { courseApp.channelMakeOperator(ron, channel, firstUser) }
                    .joinException()
        })
    }

    @Test
    fun `get10TopUsersTest primary order only`() {
        val tokens = (0..50).map {Pair(courseApp.login(it.toString(), it.toString()).join(), it.toString())}
        (0..50).forEach {courseApp.makeAdministrator(tokens[0].first, it.toString()).join()}

        val best = tokens.shuffled().take(20)
        (0..0).forEach{ courseApp.channelJoin(best[15].first, "#$it").join() }
        (0..40).forEach{ courseApp.channelJoin(best[0].first, "#$it").join() }
        (0..30).forEach{ courseApp.channelJoin(best[3].first, "#$it").join() }
        (0..40).forEach{ courseApp.channelJoin(best[0].first, "#$it").join() }
        (0..33).forEach{ courseApp.channelJoin(best[1].first, "#$it").join() }
        (0..12).forEach{ courseApp.channelJoin(best[13].first, "#$it").join() }
        (0..31).forEach{ courseApp.channelJoin(best[2].first, "#$it").join() }
        (0..21).forEach{ courseApp.channelJoin(best[7].first, "#$it").join() }
        (0..30).forEach{ courseApp.channelJoin(best[3].first, "#$it").join() }
        (0..25).forEach{ courseApp.channelJoin(best[4].first, "#$it").join() }
        (0..22).forEach{ courseApp.channelJoin(best[6].first, "#$it").join() }
        (0..15).forEach{ courseApp.channelJoin(best[11].first, "#$it").join() }
        (0..21).forEach{ courseApp.channelJoin(best[7].first, "#$it").join() }
        (0..20).forEach{ courseApp.channelJoin(best[8].first, "#$it").join() }
        (0..18).forEach{ courseApp.channelJoin(best[9].first, "#$it").join() }
        (0..16).forEach{ courseApp.channelJoin(best[10].first, "#$it").join() }
        (0..23).forEach{ courseApp.channelJoin(best[5].first, "#$it").join() }
        (0..13).forEach{ courseApp.channelJoin(best[12].first, "#$it").join() }
        (0..8).forEach{ courseApp.channelJoin(best[14].first, "#$it").join() }
        tokens.forEach {courseApp.logout(it.first).join()}
        (100..150).forEach {courseApp.login(it.toString(), it.toString()).join()}

        val output = courseAppStatistics.top10UsersByChannels().join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            output
        },
                equalTo(best.take(10).map { it.second }))
    }
}