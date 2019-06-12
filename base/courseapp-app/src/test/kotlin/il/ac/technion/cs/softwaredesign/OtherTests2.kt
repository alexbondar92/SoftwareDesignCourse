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
import java.time.Duration.ofSeconds
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class OtherTests2 {
    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())

    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    init {
        courseAppInitializer.setup()
    }

    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()

    // Total users

    @Test
    fun `total users on empty system is 0`() {
        assertThat(courseAppStatistics.totalUsers().get(), equalTo(0L))
    }

    @Test
    fun `total users after log in is 1`() {
        val username = "username"
        val password = "password"

        courseApp.login(username, password).get()

        assertThat(courseAppStatistics.totalUsers().get(), equalTo(1L))
    }

    @Test
    fun `total users after log in and log out is 1`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()

        assertThat(courseAppStatistics.totalUsers().get(), equalTo(1L))
    }

    @Test
    fun `total users after log in, log out and log in is 1`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()
        courseApp.login(username, password).get()

        assertThat(courseAppStatistics.totalUsers().get(), equalTo(1L))
    }

    // Logged in users

    @Test
    fun `logged in users on empty system is 0`() {
        assertThat(courseAppStatistics.loggedInUsers().get(), equalTo(0L))
    }

    @Test
    fun `logged in users after log in is 1`() {
        val username = "username"
        val password = "password"

        courseApp.login(username, password).get()

        assertThat(courseAppStatistics.loggedInUsers().get(), equalTo(1L))
    }

    @Test
    fun `logged in users after log in and log out is 0`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()

        assertThat(courseAppStatistics.loggedInUsers().get(), equalTo(0L))
    }

    @Test
    fun `logged in users after log in, log out and log in is 1`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()
        courseApp.login(username, password).get()

        assertThat(courseAppStatistics.loggedInUsers().get(), equalTo(1L))
    }

    // top10ChannelsByUsers

    @Test
    fun `top10ChannelsByUsers on empty system is empty`() {
        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ChannelsByUsers after join and part is empty`() {
        val (username, token) = createAdmin(0)
        val channel = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))

        courseApp.channelPart(token, channel).get()
        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ChannelsByUsers after join and kick is empty`() {
        val (username, token) = createAdmin(0)
        val channel = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))

        courseApp.channelKick(token, channel, username).get()
        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ChannelsByUsers puts 2 logged out higher that 1 logged in`() {
        val userNamesTokens = createAdmins(3).toTypedArray()
        val channel1 = createChannelWithUsers(1, listOf(userNamesTokens[0], userNamesTokens[1]), listOf())
        val channel0 = createChannelWithUsers(0, listOf(userNamesTokens[2]), listOf())
        val expected = listOf(channel1, channel0)

        courseApp.logout(userNamesTokens[0].second).get()
        courseApp.logout(userNamesTokens[1].second).get()

        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ChannelsByUsers on two channels with 1 user sorted by creation order`() {
        val (username, token) = createAdmin(0)

        val channel1 = createChannelWithUsers(1, listOf(), listOf(Pair(username, token)))
        val channel0 = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))
        val expected = listOf(channel1, channel0)

        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ChannelsByUsers on 11 channels return best 10`() {
        val namesTokens = createUsers(6).toTypedArray()
        val channels = (0L..10L).map {
            createChannelWithUsers(
                    it,
                    namesTokens.slice(0 .. (it.toInt() / 2)),
                    listOf()
            )
        }.toTypedArray()

        val expected = listOf(
                channels[10],
                channels[8],
                channels[9],
                channels[6],
                channels[7],
                channels[4],
                channels[5],
                channels[2],
                channels[3],
                channels[0]
        )

        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ChannelsByUsers works with duplicate users`() {
        val (username0, token0) = createAdmin(0)
        val (username1, token1) = createAdmin(1)
        val channel0 = createChannelWithUsers(0, listOf(Pair(username0, token0)), listOf())
        val channel1 = createChannelWithUsers(1, listOf(Pair(username1, token1), Pair(username1, token1), Pair(username1, token1)), listOf())
        val expected = listOf(channel0, channel1)

        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ChannelsByUsers ranks 3 users higher higher that 2 operators`() {
        val userNamesTokens = createUsers(3).toTypedArray()
        val channel0 = createChannelWithUsers(0, listOf(), userNamesTokens.slice(0..1))
        val channel1 = createChannelWithUsers(1, userNamesTokens.toList(), listOf())
        val expected = listOf(channel1, channel0)

        assertThat(courseAppStatistics.top10ChannelsByUsers().get(), equalTo(expected))
    }

    // top10ActiveChannelsByUsers

    @Test
    fun `top10ActiveChannelsByUsers on empty system is empty`() {
        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ActiveChannelsByUsers after join and part is empty`() {
        val (username, token) = createAdmin(0)
        val channel = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))

        courseApp.channelPart(token, channel).get()
        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ActiveChannelsByUsers after join and log out contains channel`() {
        val (username, token) = createAdmin(0)
        val channel = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))

        courseApp.logout(token).get()
        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(listOf(channel)))
    }

    @Test
    fun `top10ActiveChannelsByUsers after join and kick is empty`() {
        val (username, token) = createAdmin(0)
        val channel = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))

        courseApp.channelKick(token, channel, username).get()
        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(listOf()))
    }

    @Test
    fun `top10ActiveChannelsByUsers puts 2 logged out lower that 1 logged in`() {
        val userNamesTokens = createAdmins(3).toTypedArray()
        val channel1 = createChannelWithUsers(1, listOf(userNamesTokens[0], userNamesTokens[1]), listOf())
        val channel0 = createChannelWithUsers(0, listOf(userNamesTokens[2]), listOf())
        val expected = listOf(channel0, channel1)

        courseApp.logout(userNamesTokens[0].second).get()
        courseApp.logout(userNamesTokens[1].second).get()

        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ActiveChannelsByUsers on two channels with 1 user sorted by creation order`() {
        val (username, token) = createAdmin(0)

        val channel1 = createChannelWithUsers(1, listOf(), listOf(Pair(username, token)))
        val channel0 = createChannelWithUsers(0, listOf(), listOf(Pair(username, token)))
        val expected = listOf(channel1, channel0)

        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ActiveChannelsByUsers on 11 channels return best 10`() {
        val namesTokens = createUsers(6).toTypedArray()
        val channels = (0L..10L).map {
            createChannelWithUsers(
                    it,
                    namesTokens.slice(0 .. (it.toInt() / 2)),
                    listOf()
            )
        }.toTypedArray()

        val expected = listOf(
                channels[10],
                channels[8],
                channels[9],
                channels[6],
                channels[7],
                channels[4],
                channels[5],
                channels[2],
                channels[3],
                channels[0]
        )

        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(expected))
    }


    @Test
    fun `top10ActiveChannelsByUsers works with duplicate users`() {
        val (username0, token0) = createAdmin(0)
        val (username1, token1) = createAdmin(1)
        val channel0 = createChannelWithUsers(0, listOf(Pair(username0, token0)), listOf())
        val channel1 = createChannelWithUsers(1, listOf(Pair(username1, token1), Pair(username1, token1), Pair(username1, token1)), listOf())
        val expected = listOf(channel0, channel1)

        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(expected))
    }

    @Test
    fun `top10ActiveChannelsByUsers ranks 3 users higher higher that 2 operators`() {
        val userNamesTokens = createUsers(3).toTypedArray()
        val channel0 = createChannelWithUsers(0, listOf(), userNamesTokens.slice(0..1))
        val channel1 = createChannelWithUsers(1, userNamesTokens.toList(), listOf())
        val expected = listOf(channel1, channel0)

        assertThat(courseAppStatistics.top10ActiveChannelsByUsers().get(), equalTo(expected))
    }

    // top10UsersByChannels

    @Test
    fun `top10UsersByChannels on empty system returns emptyList`() {
        assertThat(courseAppStatistics.top10UsersByChannels().get(), equalTo(listOf()))
    }

    @Test
    fun `top10UsersByChannels on system with one user not in channels returns this user`() {
        val (username, _) = createUser(0)

        assertThat(courseAppStatistics.top10UsersByChannels().get(), equalTo(listOf(username)))
    }

    @Test
    fun `top10UsersByChannels on system with 2 users not in channels returns user in order of creation`() {
        val (username1, _) = createUser(1)
        val (username0, _) = createUser(0)

        assertThat(courseAppStatistics.top10UsersByChannels().get(), equalTo(listOf(username1, username0)))
    }

    @Test
    fun `top10UsersByChannels on system with 2 users not in channels returns user in order of creation when one is logged out`() {
        val (username1, token1) = createUser(1)
        val (username0, _) = createUser(0)

        courseApp.logout(token1).get()

        assertThat(courseAppStatistics.top10UsersByChannels().get(), equalTo(listOf(username1, username0)))
    }

    @Test
    fun `top10UsersByChannels on system with 11 users returns top 10`() {
        val nameTokens = createUsers(11).toTypedArray()
        val expected = listOf(
                nameTokens[10].first,
                nameTokens[8].first,
                nameTokens[9].first,
                nameTokens[6].first,
                nameTokens[7].first,
                nameTokens[4].first,
                nameTokens[5].first,
                nameTokens[2].first,
                nameTokens[3].first,
                nameTokens[0].first
        )

        (0..11L / 2).forEach{
            createChannelWithUsers(
                    it,
                    nameTokens.slice((10 - it.toInt() * 2)..10),
                    listOf()
            )
        }

        assertThat(courseAppStatistics.top10UsersByChannels().get(), equalTo(expected))
    }

    private lateinit var adminToken: String

    private fun createUser(index: Long): Pair<String, String> {
        val username = "username%d".format(index)
        val password = "password%d".format(index)
        val token = courseApp.login(username, password).get()

        if (!::adminToken.isInitialized) {
            adminToken = token
        }

        return Pair(username, token)
    }

    private fun createAdmin(index: Long): Pair<String, String> {
        val (username, token) = createUser(index)

        courseApp.makeAdministrator(adminToken, username).get()

        return Pair(username, token)
    }

    private fun createChannelWithUsers(
            index: Long,
            userNamesTokens: Collection<Pair<String, String>>,
            operatorNamesTokens: Collection<Pair<String, String>>
    ): String {
        val channel = "#channel%d".format(index)

        courseApp.channelJoin(adminToken, channel).get()
        operatorNamesTokens.forEach{
            courseApp.channelJoin(it.second, channel).get()
            courseApp.channelMakeOperator(
                    adminToken,
                    channel,
                    it.first
            ).get()
        }

        userNamesTokens.forEach {
            courseApp.channelJoin(it.second, channel).get()
        }

        if (operatorNamesTokens.map { it.second != adminToken }.all { it }) {
            courseApp.channelPart(adminToken, channel).get()
        }

        if (userNamesTokens.map { it.second == adminToken }.any { it }) {
            courseApp.channelJoin(adminToken, channel).get()
        }

        return channel
    }

    private fun createUsers(number: Long): Collection<Pair<String, String>> {
        return (0 until number).map(::createUser).toList()
    }

    private fun createAdmins(number: Long): Collection<Pair<String, String>> {
        return (0 until number).map(::createAdmin).toList()
    }
}