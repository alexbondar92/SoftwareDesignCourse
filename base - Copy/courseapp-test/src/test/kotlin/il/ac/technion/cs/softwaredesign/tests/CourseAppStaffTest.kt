package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import il.ac.technion.cs.softwaredesign.CourseAppModule
import il.ac.technion.cs.softwaredesign.CourseAppStatistics
import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
import il.ac.technion.cs.softwaredesign.exceptions.UserNotAuthorizedException
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration.ofSeconds


class CourseAppStaffTest {
    private val injector = Guice.createInjector(CourseAppModule(), SecureStorageModule())

    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    init {
        courseAppInitializer.setup()
    }

    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()

    @Test
    fun `after login, a user is logged in`() {
        courseApp.login("gal", "hunter2")
        courseApp.login("imaman", "31337")

        val token = courseApp.login("matan", "s3kr1t")

        assertThat(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal") },
                present(isTrue))
    }

    @Test
    fun `an authentication token is invalidated after logout`() {
        val token = courseApp.login("matan", "s3kr1t")

        courseApp.logout(token)

        assertThrows<InvalidTokenException> {
            runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "matan") }
        }
    }

    @Test
    fun `administrator can create channel and is a member of it`() {
        val administratorToken = courseApp.login("admin", "admin")
        courseApp.channelJoin(administratorToken, "#mychannel")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(administratorToken, "#mychannel", "admin")
        },
                isTrue)
    }

    @Test
    fun `non-administrator can not make administrator`() {
        courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "1234")
        courseApp.login("gal", "hunter2")

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(ofSeconds(10)) { courseApp.makeAdministrator(nonAdminToken, "gal") }
        }
    }

    @Test
    fun `non-administrator can join existing channel and be made operator`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "1234")

        courseApp.channelJoin(adminToken, "#test")
        courseApp.channelJoin(nonAdminToken, "#test")
        courseApp.channelMakeOperator(adminToken, "#test", "matan")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#mychannel", "matan")
        },
                isTrue)
    }

    @Test
    fun `user is not in channel after parting from it`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "1234")
        courseApp.channelJoin(adminToken, "#mychannel")
        courseApp.channelJoin(nonAdminToken, "#mychannel")
        courseApp.channelPart(nonAdminToken, "#mychannel")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#mychannel", "matan")
        },
                isFalse)
    }

    @Test
    fun `user is not in channel after being kicked`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "4321")
        courseApp.channelJoin(adminToken, "#236700")
        courseApp.channelJoin(nonAdminToken, "#236700")
        courseApp.channelKick(adminToken, "#236700", "matan")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, "#236700", "matan")
        },
                isFalse)
    }

    @Test
    fun `total user count in a channel is correct with a single user`() {
        val adminToken = courseApp.login("admin", "admin")
        courseApp.channelJoin(adminToken, "#test")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.numberOfTotalUsersInChannel(adminToken, "#test")
        },
                equalTo(1L))
    }

    @Test
    fun `active user count in a channel is correct with a single user`() {
        val adminToken = courseApp.login("admin", "admin")
        courseApp.channelJoin(adminToken, "#test")

        assertThat(runWithTimeout(ofSeconds(10)) {
            courseApp.numberOfActiveUsersInChannel(adminToken, "#test")
        },
                equalTo(1L))
    }

    @Test
    fun `logged in user count is correct when no user is logged in`() {
        assertThat(runWithTimeout(ofSeconds(10)) { courseAppStatistics.loggedInUsers() }, equalTo(0L))
    }

    @Test
    fun `total user count is correct when no users exist`() {
        assertThat(runWithTimeout(ofSeconds(10)) { courseAppStatistics.totalUsers() }, equalTo(0L))
    }

    @Test
    fun `top 10 channel list does secondary sorting by name`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "4321")
        courseApp.makeAdministrator(adminToken, "matan")


        courseApp.channelJoin(adminToken, "#test")
        courseApp.channelJoin(nonAdminToken, "#other")

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10ChannelsByUsers(), containsElementsInOrder("#other", "#test"))
        }
    }

    @Test
    fun `top 10 channel list counts only logged in users`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "4321")
        courseApp.makeAdministrator(adminToken, "matan")

        courseApp.channelJoin(adminToken, "#test")
        courseApp.channelJoin(nonAdminToken, "#other")
        courseApp.logout(nonAdminToken)

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10ActiveChannelsByUsers(),
                    containsElementsInOrder("#test", "#other"))
        }
    }

    @Test
    fun `top 10 user list does secondary sorting by name`() {
        val adminToken = courseApp.login("admin", "admin")
        val nonAdminToken = courseApp.login("matan", "4321")
        courseApp.makeAdministrator(adminToken, "matan")
        courseApp.channelJoin(adminToken, "#test")
        courseApp.channelJoin(nonAdminToken, "#other")

        runWithTimeout(ofSeconds(10)) {
            assertThat(courseAppStatistics.top10UsersByChannels(), containsElementsInOrder("admin", "matan"))
        }
    }
}