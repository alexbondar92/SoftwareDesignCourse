package il.ac.technion.cs.softwaredesign

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
import il.ac.technion.cs.softwaredesign.exceptions.NoSuchEntityException
import il.ac.technion.cs.softwaredesign.exceptions.UserAlreadyLoggedInException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.charset.Charset
import java.time.Duration
import java.time.Duration.ofSeconds

class CourseAppTest {
    private val courseAppInitializer = CourseAppInitializerImpl()
    private val courseApp: CourseApp
    private val courseAppReboot: CourseApp

    init {
        val storageFactory = FakeSecureStorageFactory()
        val storage = storageFactory.open("main".toByteArray(Charset.defaultCharset()))
        courseAppInitializer.setup()
        courseApp = CourseAppImpl(storage)
        courseAppReboot = CourseAppImpl(storage)
    }

    @Test
    fun `check if user is logged in`() {
        val loginToken = courseApp.login("user1", "pass1")

        assert(courseApp.isUserLoggedIn(loginToken, "user1")!!)
    }

    @Test
    fun `login and logout user`() {
        val loginToken1 = courseApp.login("user1", "pass1")
        val loginToken2 = courseApp.login("user2", "pass2")

        courseApp.logout(loginToken2)

        assert(courseApp.isUserLoggedIn(loginToken1, "user2") == false)
    }

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
    fun `log in user twice should throw exception`(){
        courseApp.login("matan", "s3kr1t")
        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "s3kr1t") }
        }
    }

    @Test
    fun `log in user twice and other users log in between should throw exception`(){
        courseApp.login("matan", "s3kr1t")
        courseApp.login("imaman", "31337")
        courseApp.login("gal", "hunter2")
        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "s3kr1t") }
        }
    }

    @Test
    fun `user log in then log out isUserLoggedIn returns False`(){
        val token = courseApp.login("matan", "s3kr1t")
        val token2 = courseApp.login("imaman", "31337")
        courseApp.logout(token2)
        assertThat(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "imaman") },
                present(isFalse))

    }

    @Test
    fun `user never been in the system isUserLoggedIn returns null`(){
        val token = courseApp.login("matan", "s3kr1t")

        assertNull(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "imaman") })

    }

    @Test
    fun `log out a none exist user `(){
        val STRING_LENGTH = 100
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = courseApp.login("matan", "s3kr1t")
        var differentToken : String = token
        while (differentToken == token) {   //creating a random token for consistency and validity
            differentToken = (1..STRING_LENGTH)
                    .map { kotlin.random.Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("");
        }

        assertThrows<InvalidTokenException> {runWithTimeout(ofSeconds(10)) { courseApp.logout(differentToken) }}

    }

    @Test
    fun `registered user log in with wrong password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.logout(token)

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password") }
        }
    }

    @Test
    fun `registered user log in with another logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.login("imaman", "another_user_password")
        courseApp.logout(token)

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "another_user_password") }
        }
    }

    @Test
    fun `registered user log in with another registered not logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        val token2 = courseApp.login("imaman", "another_user_password")
        courseApp.logout(token)
        courseApp.logout(token2)

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password") }
        }
    }

    @Test
    fun `user stays logged in after reboot, assumes his token stays active`(){
        val token = courseApp.login("matan", "s3kr1t")

        assertThat(runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan") },
                present(isTrue))
    }

    @Test
    fun `an authentication token is invalidated after logout, stays that way after reboot`(){
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.logout(token)

        assertThrows<InvalidTokenException> {
            runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan") == false }
        }
    }

    @Test
    fun `password is valid`(){
        val token1 = courseApp.login("matan", "StrongPass")
        courseApp.logout(token1)

        assertDoesNotThrow { courseApp.login("matan", "StrongPass") }
    }

    @Test
    fun `password is invalid`(){
        val token1 = courseApp.login("matan", "StrongPass")
        courseApp.logout(token1)

        assertThrows<NoSuchEntityException> { courseApp.login("matan", "OtherStrongPass") }
    }

    @Test
    fun `token is illegal at logout`(){
        val token = courseApp.login("matan", "StrongPass")

        assertThrows<InvalidTokenException> { courseApp.logout("WrongToken"+ token) }
    }

    @Test
    fun `token is illegal at is isUserLoggedin`(){
        val token = courseApp.login("matan", "StrongPass")

        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn("WrongToken"+ token, "matan") }
    }

    @Test
    fun `Unregistered user returns null upon isUserLogIn check`(){
        val token = courseApp.login("matan", "StrongPass")

        assert(courseApp.isUserLoggedIn(token, "natam") == null)
    }

    @Test
    fun `token is invalid after logout`(){
        val token1 = courseApp.login("matan2", "StrongPass")
        val token2 = courseApp.login("matan", "StrongPass")
        courseApp.logout(token2)

        assert(token1 != token2)
        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token2, "matan2") }
    }

    @Test
    fun `enable to login same user after reboot`(){
        courseApp.login("matan", "StrongPass")

        assertThrows<UserAlreadyLoggedInException> { courseAppReboot.login("matan", "StrongPass") }
    }

    @Test
    fun `system is persistent after reboot`(){
        val tokens = ArrayList<String>(11)
        for (i in 0..10){
            tokens.add(courseApp.login("user$i", "pass$i"))
        }
        for (i in 0..5){
            courseApp.logout(tokens[i])
        }

        for (i in 0..5){
            assertThrows<InvalidTokenException> { courseAppReboot.isUserLoggedIn(tokens[i], "matan$i") }
            assertDoesNotThrow { courseAppReboot.login("matan$i", "StrongPass$i") }
        }
        for (i in 6..10){
            assertDoesNotThrow { courseAppReboot.isUserLoggedIn(tokens[i], "matan$i") }
        }
    }

    @Test
    fun `corrupted(prefix & suffix) token pass over`(){
        val token = courseApp.login("matan", "StrongPass")

        for (i in 1..20) {      // for testing couple of random suffix & prefix of the token
            val prefixToken = token.substring(0, (0..(token.length - 1)).shuffled().first())
            val suffixToken = token.substring((1..token.length).shuffled().first(), token.length)
            assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(prefixToken, "matan") }
            assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(suffixToken, "matan") }
        }
    }

    @Test
    fun `login very long username and password`(){
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"

        for (i in 1..50) {
            val username = (1..(500..1000).random()).map { allowedChars.random() }.joinToString("")
            val password = (1..(500..1000).random()).map { allowedChars.random() }.joinToString("")
            assertDoesNotThrow { courseApp.login(username, password) }
        }
    }

    @Test
    fun `trying to login with wrong pass`(){
        val token = courseApp.login("matan", "StrongPass")
        courseApp.logout(token)

        assertThrows<NoSuchEntityException> { courseApp.login("matan", "WrongPass") }
    }

    @Test
    fun `selected amount of users are logged in at once`(){
        assert(false)
        val amount = 100
        runWithTimeout(ofSeconds(10)) {
            for (i in 1..amount) {
                println(i)
                val username = "user" + i
                val password = "StrongPass" + i
                courseApp.login(username, password)
            }
            //the following check is too heavy time-wise, and not needed.
            //why we left it here? to get some ideas for future stress testing..
            /*
            val token = innerCourseApp.login("heroUser", "password")

            for (i in 1..amount) {
                assertDoesNotThrow { innerCourseApp.isUserLoggedIn(token, "user" + i) }
            }
            */
        }
    }

    @Test
    fun `first user in the system becomes administrator automatically`() {

    }

    @Test
    fun `makeAdministrator gets invalid token`() {

    }

    @Test
    fun `makeAdministrator gets token that is not associated to administrator`() {

    }

    @Test
    fun `makeAdministrator gets invalid user`() {

    }

//    @Test
//    fun `makeAdministrator gets user that is already administrator`() {
//
//    }

    @Test
    fun `administrator retain their status after relogging`() {

    }

    @Test
    fun `administrator retain their status after system reboot`() {

    }

    @Test
    fun `make random number of administrator in the system`() {

    }

    @Test
    fun `make administrator of logged out user`() {

    }

    @Test
    fun `create channel`() {

    }

    @Test
    fun `create channel with illegal name`() {

    }

    @Test
    fun `invalid token(not associated to no one) tries to to join to channel`() {

    }

    @Test
    fun `invalid token(not of admin) tries to to create new channel`() {

    }

    @Test
    fun `first user in the channel is a Operator`() {

    }

    @Test
    fun `add up to 512 users to the channel`() {

    }

    @Test
    fun `leave channel with illegal token(existing channel)`() {

    }

    @Test
    fun `leave channel with illegal token(not existing channel)`() {

    }

    @Test
    fun `trying to leave channel with token that is not member of the channel`() {

    }

    @Test
    fun `leave chanel with valid token but the channel does not exist`() {

    }

    @Test
    fun `regural member leaves channel`() {

    }

    @Test
    fun `operator leave the channel`() {

    }

    @Test
    fun `administrator that is not an operator of this channel leaves the channel`() {

    }

    @Test
    fun `check join and leave channel`() {

    }

    @Test
    fun `not operators in the channel`() {

    }

    @Test
    fun `last user(regular) leaves the channel`() {

    }

    @Test
    fun `last user(operator) leaves the channel`() {

    }

    @Test
    fun `last user(administrator) leaves the channel`() {

    }

    @Test
    fun `trying to join to channel that was deleted`() {

    }

    @Test
    fun `make random number of channels`() {

    }

    @Test
    fun `try to make operator with illegal token(regular)`() {

    }

    @Test
    fun `try to make operator with illegal token(not of admin or operator)`() {

    }

    @Test
    fun `try to make operator with illegal token(is admin but not operator)`() {

    }

    @Test
    fun `try to make operator with illegal token(operator of other channel)`() {

    }

    @Test
    fun `admin makes himself as operator in the channel`() {

    }

    @Test
    fun `operator makes himself as an operator at the same channel`() {

    }

    @Test
    fun `try to make operator of user that is not a member to this channel`() {

    }

    @Test
    fun `try to make operator of admin that is not a member to this channel`() {

    }

    @Test
    fun `try to make operator with with channel that not exits(never been)`() {

    }

    @Test
    fun `try to make operator with with channel that not exits(was deleted)`() {

    }

    @Test
    fun `try to make operator of user that is not a memeber to this channel`() {

    }

    @Test
    fun `try to make operator of user that is not in the system`() {

    }

    @Test
    fun `make operator of user that in member of the channel, but is logged out`() {

    }

    @Test
    fun `try to kink user with illegal token(regular)`() {

    }

    @Test
    fun `try to kick user with illegal token(not operator)`() {

    }

    @Test
    fun `try to kick user with illegal token(admin)`() {

    }

    @Test
    fun `kick user from channel`() {

    }

    @Test
    fun `kick another operator from channel`() {

    }

    @Test
    fun `kick administrator from channel`() {

    }

    @Test
    fun `kick another operator that is admin(as well) from channel`() {

    }

    @Test
    fun `operator kick himself from the channel`() {

    }

    @Test
    fun `operator kick himself from the channel, and he was the last one`() {

    }

    @Test
    fun `try to kick from a channel that is not exists`() {

    }

    @Test
    fun `try to kick from a deleted channel that is not exists`() {

    }

    @Test
    fun `try to kick illegal user`() {

    }

    @Test
    fun `try to kick non member user`() {

    }

    @Test
    fun `try to kick user that was in member of the channel`() {

    }

    @Test
    fun `get number of active users in channel with zero active users`() {

    }

    @Test
    fun `get number of active users in channel by admin that is not part of the channel`() {

    }

    @Test
    fun `get number of active users in channel by user in the channel`() {

    }

    @Test
    fun `try to get number of active users with illegal token`() {

    }

    @Test
    fun `try to get number of active users with channel that is not exits`() {

    }

    @Test
    fun `try to get number of active users with channel that was deleted`() {

    }

    @Test
    fun `try to get number of active users with token that is not a member of the channel and not admin`() {

    }

    @Test
    fun `get number of total users in channel by admin that is not part of the channel`() {

    }

    @Test
    fun `get number of total users in channel by user in the channel`() {

    }

    @Test
    fun `try to get total of active users with illegal token`() {

    }

    @Test
    fun `try to get total of active users with channel that is not exits`() {

    }

    @Test
    fun `try to get total of active users with channel that was deleted`() {

    }

    @Test
    fun `try to get total of active users with token that is not a member of the channel and not admin`() {

    }

    @Test
    fun `joining user to 128 channels`() {

    }

    @Test
    fun `stress test for channels in the system`() {

    }
}
