package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
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

class OtherTests1 {

    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())
    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    init {
        courseAppInitializer.setup()
    }

    private val courseApp = injector.getInstance<CourseApp>()


    @Test
    fun `different users can use same password`() {
        val username1 = "user1"
        val password = "password"
        val username2 = "user2"
        val token1 = courseApp.login(username1, password).get()
        val token2 = courseApp.login(username2, password).get()

        assertThat(courseApp.isUserLoggedIn(token1, username2).get(), equalTo(true))
        assertThat(courseApp.isUserLoggedIn(token1, username1).get(), equalTo(true))

        courseApp.logout(token1).get()
        courseApp.logout(token2).get()
    }

    @Test
    fun `can logout after system reboot`() {
        var courseApp = injector.getInstance<CourseApp>()
        val username = "user"
        val password = "password"
        val token = courseApp.login(username, password).get()
        courseApp = injector.getInstance()

        courseApp.logout(token).get()
    }

    @Test
    fun `can not logging after system reboot`() {
        var courseApp = injector.getInstance<CourseApp>()
        val username = "user"
        val password = "password"
        courseApp.login(username, password)
        courseApp = injector.getInstance()

        assertThrows<UserAlreadyLoggedInException> { courseApp.login(username, password).joinException() }
    }

    @Test
    fun `throw NoSuchEntityException if password does not match username`() {
        val username = "user"
        val password1 = "password1"
        val password2 = "password2"
        val token = courseApp.login(username, password1).get()

        courseApp.logout(token).get()
        assertThrows<NoSuchEntityException> { courseApp.login(username, password2).joinException() }
    }

    //TODO: test order NoSuchEntityException UserAlreadyLoggedInException

    @Test
    fun `throw UserAlreadyLoggedInException if user already logged in`() {
        val courseApp = injector.getInstance<CourseApp>()
        val username = "user"
        val password1 = "password1"
        courseApp.login(username, password1).get()

        assertThrows<UserAlreadyLoggedInException> { courseApp.login(username, password1).joinException() }
    }

    @Test
    fun `throw InvalidTokenException if user twice log out`() {
        val courseApp = injector.getInstance<CourseApp>()
        val username = "user"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()
        assertThrows<InvalidTokenException> { courseApp.logout(token).joinException() }
    }

    @Test
    fun `throw InvalidTokenException if user tests if he is log in after logout`() {
        val courseApp = injector.getInstance<CourseApp>()
        val username = "user"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()
        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token, username).joinException() }
    }

    @Test
    fun `throw InvalidTokenException if user tests if not existing user is logged in after logout`() {
        val courseApp = injector.getInstance<CourseApp>()
        val username1 = "user"
        val password = "password"
        val username2 = "user1"
        val token = courseApp.login(username1, password).get()

        courseApp.logout(token).get()
        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token, username2).joinException() }
    }

    @Test
    fun `throw InvalidTokenException if user tests if logged in user is logged in after logout`() {
        val username1 = "user"
        val password = "password"
        val username2 = "user1"
        val token = courseApp.login(username1, password).get()

        courseApp.login(username2, password).get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token, username2).joinException() }
    }

    @Test
    fun `throw InvalidTokenException if user tests if logged out user is logged in after logout`() {
        val username1 = "user"
        val password = "password"
        val username2 = "user1"
        val token = courseApp.login(username1, password).get()

        courseApp.logout(courseApp.login(username2, password).get()).get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token, username2).joinException() }
    }

    @Test
    fun `isUserLoggedIn returns true if another user exists and log in`() {
        val username1 = "user1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "user2"
        val password2 = "password2"
        courseApp.login(username2, password2).get()

        assertThat(courseApp.isUserLoggedIn(token1, username2).get(), present(equalTo(true)))
    }

    @Test
    fun `isUserLoggedIn returns true on himself when log in`() {
        val username1 = "user1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()

        assertThat(courseApp.isUserLoggedIn(token1, username1).get(), present(equalTo(true)))
    }

    @Test
    fun `isUserLoggedIn returns false if another user exists and log out`() {
        val username1 = "user1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "user2"
        val password2 = "password2"
        val token2 = courseApp.login(username2, password2).get()
        courseApp.logout(token2)

        assertThat(courseApp.isUserLoggedIn(token1, username2).get(), present(equalTo(false)))
    }

    @Test
    fun `isUserLoggedIn returns null if another user does not exists`() {
        val username1 = "user1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "user2"

        assertThat(courseApp.isUserLoggedIn(token1, username2).get(), absent())
    }


    @Test
    fun `NoSuchEntityException thrown when not admin tries to makeAdministrator`() {
        val username1 = "user1"
        val password1 = "password1"
        val username2 = "user2"
        val token = courseApp.login(username1, password1).get()

        assertThrows<NoSuchEntityException> { courseApp.makeAdministrator(token, username2).joinException() }
    }

    // TODO: test exceptions order

    @Test
    fun `not existing token causes InvalidTokenException in channelJoin`() {
        val token = "token"
        val channel = "#Scorpions"

        assertThrows<InvalidTokenException> { courseApp.channelJoin(token, channel).joinException() }
    }

    @Test
    fun `token after logout causes InvalidTokenException in channelJoin`() {
        val username = "user"
        val password = "password"
        val channel = "#Aerosmith"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.channelJoin(token, channel).joinException() }
    }

    @Test
    fun `can create channel called #`() {
        createUserAndChannel("#")
    }

    @Test
    fun `can create channel called #Software_Design_#236700`() {
        createUserAndChannel("#Software_Design_#236700")
    }

    @Test
    fun `NameFormatException is thrown when creating Software_Design_#236700`() {
        assertThrows<NameFormatException> {
            createUserAndChannelJoinException("Software_Design_#236700")
        }
    }

    @Test
    fun `UserNotAuthorizedException thrown when not admin can not create chanel`() {
        val username = "user1"
        val password = "password1"
        courseApp.login(username, password).get()
        val channel = "#Amaranthe"

        assertThrows<UserNotAuthorizedException> { createUserAndChannelJoinException(channel) }
    }

    @Test
    fun `not admin user can channelJoin existing channel by name`() {
        val channel = "#Nightwish"
        createUserAndChannel(channel)

        val username = "user1"
        val password = "password1"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()
    }

    @Test
    fun `after exiting last user not admin can not join`() {
        val channel = "#Motanka"
        val tokenAdmin = createUserAndChannel(channel)
        val username = "username1"
        val password = "password1"
        val token = courseApp.login(username, password).get()

        courseApp.channelPart(tokenAdmin, channel).get()
        assertThrows<UserNotAuthorizedException> { courseApp.channelJoin(token, channel).joinException() }
    }

    @Test
    fun `not existing token causes InvalidTokenException in channelPart`() {
        val channel = "#Jinjer"
        val token = "token"

        createUserAndChannel(channel)

        assertThrows<InvalidTokenException> { courseApp.channelPart(token, channel).joinException() }
    }

    @Test
    fun `token after logout causes InvalidTokenException in channelPart`() {
        val username = "username1"
        val password = "password1"
        val channel = "#Jinjer"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()
        courseApp.channelJoin(token, channel).get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.channelPart(token, channel).joinException() }
    }

    @Test
    fun `NoSuchEntityException when channelPart user does not belong to channel`() {
        val channel = "#Kiss"

        createUserAndChannel(channel)

        val username = "username1"
        val password = "password1"
        val token = courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.channelPart(token, channel).joinException() }
    }

    @Test
    fun `NoSuchEntityException when channelPart on channel which does not exit`() {
        val channel = "#Kiss"
        val username = "username1"
        val password = "password1"
        val token = courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.channelPart(token, channel).joinException() }
    }

    @Test
    fun `not existing token causes InvalidTokenException in channelMakeOperator`() {
        val channel = "#Jinjer"
        val token = "token"
        val username = "username"

        createUserAndChannel(channel)

        assertThrows<InvalidTokenException> { courseApp.channelMakeOperator(token, channel, username).joinException() }
    }

    @Test
    fun `token after logout causes InvalidTokenException in channelMakeOperator`() {
        val username = "username1"
        val password = "password1"
        val channel = "#Jinjer"
        val username2 = "username2"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()
        courseApp.channelJoin(token, channel).get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.channelMakeOperator(token, channel, username2).joinException() }
    }

    @Test
    fun `NoSuchEntityException when channelMakeOperator on channel which does not exit`() {
        val channel = "#Kiss"
        val username = "username1"
        val password = "password1"
        val token = courseApp.login(username, password).get()
        val username2 = "username2"

        assertThrows<NoSuchEntityException> { courseApp.channelMakeOperator(token, channel, username2).joinException() }
    }

    // channelMakeOperator

    @Test
    fun `channelMakeOperator throws UserNotAuthorizedException when user is not an operator or administrator`() {
        val channel = "#ArchEnemy"
        createUserAndChannel(channel)
        val username = "username1"
        val password = "password1"
        val token = courseApp.login(username, password).get()
        val username2 = "username2"

        courseApp.channelJoin(token, channel).get()

        assertThrows<UserNotAuthorizedException> { courseApp.channelMakeOperator(token, channel, username2).joinException() }
    }

    @Test
    fun `channelMakeOperator throws UserNotAuthorizedException when user an administrator who is not makes himself`() {
        val channel = "#Helloween"
        val token = createUserAndChannel(channel)
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        courseApp.makeAdministrator(token, username1).get()
        val username2 = "username2"
        courseApp.channelJoin(token1, channel).get()

        assertThrows<UserNotAuthorizedException> { courseApp.channelMakeOperator(token1, channel, username2).joinException() }
    }

    @Test
    fun `channelMakeOperator throws UserNotAuthorizedException when administrator is not a member of channel`() {
        val channel = "#Helloween"
        val token = createUserAndChannel(channel)
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        courseApp.makeAdministrator(token, username1).get()

        assertThrows<UserNotAuthorizedException> { courseApp.channelMakeOperator(token1, channel, username1).joinException() }
    }

    @Test
    fun `channelMakeOperator throws NoSuchEntityException if username does not exist`() {
        val channel = "#Helloween"
        val token = createUserAndChannel(channel)
        val username = "username1"

        assertThrows<NoSuchEntityException> { courseApp.channelMakeOperator(token, channel, username).joinException() }
    }

    @Test
    fun `channelMakeOperator throws NoSuchEntityException if username is not a member of channel`() {
        val channel = "#Helloween"
        val token = createUserAndChannel(channel)
        val username = "username1"
        val password = "password1"
        courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.channelMakeOperator(token, channel, username).joinException() }
    }

    // TODO: test if actually adding happens when everything is safisfied

    @Test
    fun `after becoming operator can make other users operators`() {
        val channel = "#Edguy"
        val token = createUserAndChannel(channel)
        val username = "username1"
        val password = "password1"
        val token1 = courseApp.login(username, password).get()
        val username2 = "username2"
        val password2 = "password2"
        val token2 = courseApp.login(username2, password2).get()

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelMakeOperator(token, channel, username).get()

        courseApp.channelMakeOperator(token1, channel, username2).get()
    }

    @Test
    fun `after becoming operator can make kick other users`() {
        val channel = "#Edguy"
        val token = createUserAndChannel(channel)
        val username = "username1"
        val password = "password1"
        val token1 = courseApp.login(username, password).get()
        val username2 = "username2"
        val password2 = "password2"
        val token2 = courseApp.login(username2, password2).get()

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelMakeOperator(token, channel, username).get()

        courseApp.channelKick(token1, channel, username2).get()
    }

    // channelKick
    @Test
    fun `channelKick throws InvalidTokenException if token does not exist`() {
        val channel = "#Jinjer"
        val token = "token"
        val username = "username"

        assertThrows<InvalidTokenException> { courseApp.channelKick(token, channel, username).joinException() }
    }

    @Test
    fun `channelKick throws InvalidTokenException if token after logout`() {
        val channel = "#Jinjer"
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.logout(token).get()
        assertThrows<InvalidTokenException> { courseApp.channelKick(token, channel, username).joinException() }
    }

    @Test
    fun `channelKick throws NoSuchEntityException if channel does not exist`() {
        val channel = "#Jinjer"
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.channelKick(token, channel, username).joinException() }
    }

    @Test
    fun `channelKick throws NoSuchEntityException if channel existed but now does not exist`() {
        val channel = "#Jinjer"
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.channelKick(token, channel, username).joinException() }
    }

    @Test
    fun `channelKick throws NoSuchEntityException if channel does not exists`() {
        val channel = "#Jinjer"
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()
        val username1 = "username1"

        assertThrows<NoSuchEntityException> { courseApp.channelKick(token, channel, username1).joinException() }
    }

    @Test
    fun `channelKick throws UserNotAuthorizedException if token is member admin but not operator`() {
        val channel = "#Symfomania"
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "username2"
        val password2 = "password"
        val token2 = courseApp.login(username2, password2).get()

        courseApp.makeAdministrator(token1, username2).get()
        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        assertThrows<UserNotAuthorizedException> { courseApp.channelKick(token2, channel, username2).joinException() }
    }

    @Test
    fun `channelKick throws NoSuchEntityException if username does not exists`() {
        val channel = "#Symfomania"
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "username2"

        courseApp.channelJoin(token1, channel).get()
        assertThrows<NoSuchEntityException> { courseApp.channelKick(token1, channel, username2).joinException() }
    }

    @Test
    fun `channelKick throws NoSuchEntityException if username is not member of channel`() {
        val channel = "#Symfomania"
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "username2"
        val password2 = "password2"

        courseApp.login(username2, password2).get()
        courseApp.channelJoin(token1, channel).get()

        assertThrows<NoSuchEntityException> { courseApp.channelKick(token1, channel, username2).joinException() }
    }

    // isUserInChannel

    @Test
    fun `isUserInChannel throws InvalidTokenException if token does not exist`() {
        val channel = "#Jinjer"
        val token = "token"
        val username = "username"

        createUserAndChannel(channel)

        assertThrows<InvalidTokenException> { courseApp.isUserInChannel(token, channel, username).joinException() }
    }

    @Test
    fun `isUserInChannel throws InvalidTokenException if token after logout`() {
        val username = "username1"
        val password = "password1"
        val channel = "#Jinjer"
        val username2 = "username2"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()
        courseApp.channelJoin(token, channel).get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> { courseApp.isUserInChannel(token, channel, username2).joinException() }
    }

    @Test
    fun `after double joining and one part user is not in channel`() {
        val username = "username"
        val password = "password"
        val channel = "#SOAD"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()
        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
        courseApp.channelJoin(token, channel).get()
        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
        courseApp.channelPart(token, channel).get()
        assertThrows<NoSuchEntityException> { courseApp.isUserInChannel(token, channel, username).joinException() }
    }

    @Test
    fun `after double joining and one kick user is not in channel`() {
        val username = "username"
        val password = "password"
        val channel = "#SOAD"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()
        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
        courseApp.channelJoin(token, channel).get()
        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
        courseApp.channelKick(token, channel, username).get()
        assertThrows<NoSuchEntityException> { courseApp.isUserInChannel(token, channel, username).joinException() }
    }

    @Test
    fun `after double make operator, part and join user is not operator`() {
        val username0 = "username0"
        val password0 = "password0"
        val token0 = courseApp.login(username0, password0).get()
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val channel = "#Serj"

        courseApp.channelJoin(token0, channel).get()
        courseApp.channelJoin(token1, channel).get()
        courseApp.channelMakeOperator(token0, channel, username1).get()
        courseApp.channelMakeOperator(token0, channel, username1).get()
        courseApp.channelMakeOperator(token1, channel, username1).get()
        courseApp.channelPart(token1, channel).get()
        courseApp.channelJoin(token1, channel).get()
        assertThrows<UserNotAuthorizedException> { courseApp.channelMakeOperator(token1, channel, username1).joinException() }
    }

    @Test
    fun `after double make operator, kick and join user is not operator`() {
        val username0 = "username0"
        val password0 = "password0"
        val token0 = courseApp.login(username0, password0).get()
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val channel = "#Serj"

        courseApp.channelJoin(token0, channel).get()
        courseApp.channelJoin(token1, channel).get()
        courseApp.channelMakeOperator(token0, channel, username1).get()
        courseApp.channelMakeOperator(token0, channel, username1).get()
        courseApp.channelMakeOperator(token1, channel, username1).get()
        courseApp.channelKick(token1, channel, username1).get()
        courseApp.channelJoin(token1, channel).get()
        assertThrows<UserNotAuthorizedException> { courseApp.channelMakeOperator(token1, channel, username1).joinException() }
    }

    @Test
    fun `isUserInChannel throws NoSuchEntityException if channel does not exist`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val token = courseApp.login(username, password).get()

        assertThrows<NoSuchEntityException> { courseApp.isUserInChannel(token, channel, username).joinException() }
    }

    @Test
    fun `isUserInChannel throws NoSuchEntityException if channel was once but does not exist now`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()
        courseApp.channelPart(token, channel).get()

        assertThrows<NoSuchEntityException> { courseApp.isUserInChannel(token, channel, username).joinException() }
    }

    @Test
    fun `isUserInChannel throws UserNotAuthorizedException if user is not administrator or member of channel`() {
        val username = "username1"
        val password = "password1"
        val username2 = "username2"
        val channel = "#Rammstein"
        createUserAndChannel(channel)
        val token = courseApp.login(username, password).get()

        assertThrows<UserNotAuthorizedException> { courseApp.isUserInChannel(token, channel, username2).joinException() }
    }

    @Test
    fun `isUserInChannel return true for administrator if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
    }

    @Test
    fun `isUserInChannel return true for member if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username).get(), equalTo(true))
    }

    @Test
    fun `isUserInChannel return false for administrator if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val username1 = "username1"
        val password1 = "password1"
        val token = courseApp.login(username, password).get()
        courseApp.login(username1, password1).get()

        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username1).get(), equalTo(false))
    }

    @Test
    fun `isUserInChannel return false for member if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val username1 = "username1"
        val password1 = "password1"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()

        courseApp.login(username1, password1).get()
        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username1).get(), equalTo(false))
    }

    @Test
    fun `isUserInChannel return null for administrator if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val username1 = "username1"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username1).get(), absent())
    }

    @Test
    fun `isUserInChannel return null for member if username is member of channel`() {
        val username = "username"
        val password = "password"
        val channel = "#Rammstein"
        val username1 = "username1"

        createUserAndChannel(channel)

        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()

        assertThat(courseApp.isUserInChannel(token, channel, username1).get(), absent())
    }

    // numberOfActiveUsersInChannel

    @Test
    fun `numberOfActiveUsersInChannel throws InvalidTokenException if token does not exist`() {
        val token = "token"
        val channel = "#Motanka"

        assertThrows<InvalidTokenException> { courseApp.numberOfActiveUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfActiveUsersInChannel throws NoSuchEntityException if channel not exist`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()
        val channel = "#Motanka"

        assertThrows<NoSuchEntityException> { courseApp.numberOfActiveUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfActiveUsersInChannel throws NoSuchEntityException if channel was created but now not exist`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token, channel).get()
        courseApp.channelPart(token, channel).get()

        assertThrows<NoSuchEntityException> { courseApp.numberOfActiveUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfActiveUsersInChannel throws UserNotAuthorizedException if token is not admin or member`() {
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "username2"
        val password2 = "password2"
        val token2 = courseApp.login(username2, password2).get()
        val channel1 = "#Motanka"
        val channel2 = "#Karna"

        courseApp.channelJoin(token1, channel1).get()
        courseApp.channelJoin(token1, channel2).get()
        courseApp.channelJoin(token2, channel2).get()

        assertThrows<UserNotAuthorizedException> { courseApp.numberOfActiveUsersInChannel(token2, channel1).joinException() }
    }

    @Test
    fun `numberOfActiveUsersInChannel returns 0 for admin when 0 active users`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()
        courseApp.logout(token2).get()

        assertThat(courseApp.numberOfActiveUsersInChannel(token1, channel).get(), equalTo(0L))
    }

    @Test
    fun `numberOfActiveUsersInChannel returns 1 for admin when 1 active user`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()

        assertThat(courseApp.numberOfActiveUsersInChannel(token1, channel).get(), equalTo(1L))
    }

    @Test
    fun `numberOfActiveUsersInChannel returns 1 for member when 1 active user`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()

        assertThat(courseApp.numberOfActiveUsersInChannel(token2, channel).get(), equalTo(1L))
    }

    // numberOfTotalUsersInChannel

    @Test
    fun `numberOfTotalUsersInChannel throws InvalidTokenException if token does not exist`() {
        val token = "token"
        val channel = "#Motanka"

        assertThrows<InvalidTokenException> { courseApp.numberOfTotalUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfTotalUsersInChannel throws NoSuchEntityException if channel not exist`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()
        val channel = "#Motanka"

        assertThrows<NoSuchEntityException> { courseApp.numberOfTotalUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfTotalUsersInChannel throws NoSuchEntityException if channel was created but now not exist`() {
        val username = "username"
        val password = "password"
        val token = courseApp.login(username, password).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token, channel).get()
        courseApp.channelPart(token, channel).get()

        assertThrows<NoSuchEntityException> { courseApp.numberOfTotalUsersInChannel(token, channel).joinException() }
    }

    @Test
    fun `numberOfTotalUsersInChannel throws UserNotAuthorizedException if token is not admin or member`() {
        val username1 = "username1"
        val password1 = "password1"
        val token1 = courseApp.login(username1, password1).get()
        val username2 = "username2"
        val password2 = "password2"
        val token2 = courseApp.login(username2, password2).get()
        val channel1 = "#Motanka"
        val channel2 = "#Karna"

        courseApp.channelJoin(token1, channel1).get()
        courseApp.channelJoin(token1, channel2).get()
        courseApp.channelJoin(token2, channel2).get()

        assertThrows<UserNotAuthorizedException> { courseApp.numberOfTotalUsersInChannel(token2, channel1).joinException() }
    }

    @Test
    fun `numberOfTotalUsersInChannel returns 1 for admin when 1 logged out`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()
        courseApp.logout(token2).get()

        assertThat(courseApp.numberOfTotalUsersInChannel(token1, channel).get(), equalTo(1L))
    }

    @Test
    fun `numberOfTotalUsersInChannel returns 1 for admin when 1 logged in user`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()

        assertThat(courseApp.numberOfTotalUsersInChannel(token1, channel).get(), equalTo(1L))
    }

    @Test
    fun `numberOfTotalUsersInChannel returns 1 for member when 1 active user`() {
        val username1 = "username1"
        val password1 = "password1"
        val username2 = "username2"
        val password2 = "password2"
        val token1 = courseApp.login(username1, password1).get()
        val token2 = courseApp.login(username2, password2).get()
        val channel = "#Motanka"

        courseApp.channelJoin(token1, channel).get()
        courseApp.channelJoin(token2, channel).get()
        courseApp.channelPart(token1, channel).get()

        assertThat(courseApp.numberOfTotalUsersInChannel(token2, channel).get(), equalTo(1L))
    }

    private fun createUserAndChannel(channel: String): String {
        val username = "user"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).get()

        return token
    }

    private fun createUserAndChannelJoinException(channel: String): String {
        val username = "user"
        val password = "password"
        val token = courseApp.login(username, password).get()

        courseApp.channelJoin(token, channel).joinException()

        return token
    }
}