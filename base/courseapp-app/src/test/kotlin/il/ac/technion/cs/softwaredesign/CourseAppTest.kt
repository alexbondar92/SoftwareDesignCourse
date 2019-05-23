package il.ac.technion.cs.softwaredesign

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.exceptions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.charset.Charset
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.random.Random

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
                    .map { Random.nextInt(0, charPool.size) }
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
        val adminToken = courseApp.login("firstUser", "pass")
        val userToken = courseApp.login("regUser", "pass2")

        assertDoesNotThrow { courseApp.makeAdministrator(adminToken, "regUser") }
    }

    @Test
    fun `makeAdministrator gets invalid token throws InvalidTokenException`() {
        val STRING_LENGTH = 20
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = courseApp.login("matan", "s3kr1t")
        var differentToken : String = token
        while (differentToken == token) {   //creating a random token for consistency and validity
            differentToken = (1..STRING_LENGTH)
                    .map { kotlin.random.Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("");
        }

        assertThrows<InvalidTokenException> {courseApp.makeAdministrator(differentToken, "matam")}
    }

    @Test
    fun `makeAdministrator gets token that is not associated to administrator`() {
        val adminToken = courseApp.login("firstUser", "pass")
        val userToken = courseApp.login("regUser", "pass2")
        val otherUserToken = courseApp.login("otherUser", "pass3")

        assertThrows<UserNotAuthorizedException> { courseApp.makeAdministrator(userToken, "otherUser") }
    }

    @Test
    fun `makeAdministrator gets invalid user`() {
        val adminToken = courseApp.login("firstUser", "pass")
        val userToken = courseApp.login("regUser", "pass2")

        assertThrows<UserNotAuthorizedException> { courseApp.makeAdministrator(adminToken, "InvalidUser") }
    }

    @Test
    fun `makeAdministrator makes administrator, an admin again`() {
        val adminToken = courseApp.login("firstUser", "pass")
        val userToken = courseApp.login("regUser", "pass2")

        courseApp.makeAdministrator(adminToken, "regUser")

        assertThrows<UserNotAuthorizedException> { courseApp.makeAdministrator(userToken, "firstUser") }
    }

    @Test
    fun `administrator retain their status after relogging`() {
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.makeAdministrator(token, "matan")
        courseApp.logout(token)
        val secondToken = courseApp.login("matan", "s3kr1t")
        courseApp.login("Ron", "s3kwwwr1t")
        assertDoesNotThrow{ courseApp.makeAdministrator(secondToken,"Ron")}
    }

    @Test
    fun `administrator retain their status after system reboot`() {
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.makeAdministrator(token, "matan")
        courseApp.login("Ron", "s3kwwwr1t")
        assertDoesNotThrow{ courseAppReboot.makeAdministrator(token,"Ron")}
    }

    @Test
    fun `make random number of administrator in the system`() {
        val numOfAdmins = Random.nextInt(1, 100)
        val tokenDict = mutableMapOf<Int, String>()
        for (i in 1..100) {
            tokenDict[i] = courseApp.login("user$i", "pass")
        }

        assertDoesNotThrow {
            for (i in 1..numOfAdmins) {
                courseApp.makeAdministrator(tokenDict[1]!!, "user$i")
            }
        }
    }

    @Test
    fun `make administrator of logged out user`() {
        val token = courseApp.login("matan", "s3kr1t")
        val ronToken = courseApp.login("Ron", "s3kwwwr1t")
        courseApp.logout(ronToken)
        assertDoesNotThrow{
            courseApp.makeAdministrator(token, "Ron")
        }
    }

    @Test
    fun `create channel`() {
        val adminToken = courseApp.login("admin", "pass")

        assertDoesNotThrow {courseApp.channelJoin(adminToken, "#greatChannel")}
    }

    @Test
    fun `create channel with illegal name`() {
        val adminToken = courseApp.login("admin", "pass")

        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "wrongName") }
        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "wrong#Name") }
        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "###dollarSign$$") }
    }

    @Test
    fun `invalid token(not associated to no one) tries to join to channel`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss")
        courseApp.channelJoin(adminT, "#AvlLovers")
        assertThrows<InvalidTokenException> {courseApp.channelJoin("different$adminT", "#AvlLovers")}
    }

    @Test
    fun `invalid token(not of admin) tries to create new channel`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss")
        val notAdminToken = courseApp.login("Person", "passssss")
        assertThrows<UserNotAuthorizedException> {courseApp.channelJoin(notAdminToken, "#SomeChannel#___")}
    }

    @Test
    fun `first user in the channel is a Operator`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertDoesNotThrow{ courseApp.channelKick(adminToken, "#greatChannel", "regUsaer") }
    }

    @Test
    fun `add up to 512 users to the channel`() {
        val numInChannel = Random.nextInt(1, 512)
        val tokenDict = mutableMapOf<Int, String>()
        for (i in 1..512) {
            tokenDict[i] = courseApp.login("user$i", "pass")
        }

        assertDoesNotThrow {
            for (i in 1..numInChannel) {
                courseApp.channelJoin(tokenDict[1]!!, "user$i")
            }
        }
    }

    @Test
    fun `leave channel with illegal token(existing channel)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#TestChannel")
        val regToken = courseApp.login("reg", "pass")

        assertThrows<InvalidTokenException> { courseApp.channelKick("SomeOtherToken", "#TestChannel", "reg") }
    }

    @Test
    fun `leave channel with illegal token(not existing channel)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#TestChannel")
        val regToken = courseApp.login("reg", "pass")

        assertThrows<InvalidTokenException> { courseApp.channelKick("SomeOtherToken", "#NotExistingChannel", "reg") }
    }

    @Test
    fun `trying to leave channel with token that is not member of the channel`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss")
        courseApp.channelJoin(adminT, "#AvlLovers")
        val t1 = courseApp.login("p1", "Ifffs")
        assertThrows<NoSuchEntityException> {
            courseApp.channelPart(t1,"#AvlLovers")
        }
    }

    @Test
    fun `leave chanel with valid token but the channel does not exist`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss")
        courseApp.channelJoin(adminT, "#AvlLovers")
        assertThrows<NoSuchEntityException> {
            courseApp.channelPart(adminT,"#AILovers")
        }
    }

    @Test
    fun `regular member leaves channel`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertDoesNotThrow { courseApp.channelPart(regUserToken, "#greatChannel") }
    }

    @Test
    fun `operator leave the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")
        val operatorToken = courseApp.login("operator", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.channelJoin(operatorToken, "#greatChannel")
        courseApp.channelMakeOperator(adminToken, "#greatChannel", "operator")

        assertDoesNotThrow { courseApp.channelPart(operatorToken, "#greatChannel") }
    }

    @Test
    fun `administrator that is not an operator of this channel leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")
        val otherAdmin = courseApp.login("otherAdmin", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.makeAdministrator(adminToken, "otherAdmin")
        courseApp.channelJoin(otherAdmin, "#greatChannel")

        assertDoesNotThrow { courseApp.channelPart(otherAdmin, "#greatChannel") }
    }

    @Test
    fun ` channel does not contains operators`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertDoesNotThrow { courseApp.channelPart(adminToken, "#greatChannel") }

    }

    @Test
    fun `last user(regular) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.channelPart(adminToken, "#greatChannel")
        assertDoesNotThrow {courseApp.channelPart(regUserToken, "#greatChannel")}

    }

    @Test
    fun `last user(operator) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.channelMakeOperator(adminToken,"#greatChannel", "regUser" )
        courseApp.channelPart(adminToken, "#greatChannel")

        assertDoesNotThrow { courseApp.channelPart(regUserToken, "#greatChannel")}

    }

    @Test
    fun `last user(administrator) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")

        assertDoesNotThrow { courseApp.channelPart(adminToken, "#greatChannel")}
    }

    @Test
    fun `trying to join to channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelPart(adminToken, "#greatChannel")

        val regUserToken = courseApp.login("regUser", "pass")
        assertDoesNotThrow { courseApp.channelJoin(regUserToken, "#greatChannel")}
    }

    @Test
    fun `make random number of channels`() {
        val numOfChannels = Random.nextInt(1, 100)
        val adminToken = courseApp.login("admin", "pass")
        val STRING_LENGTH = 20
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
        val list  = mutableListOf<String>()

        assertDoesNotThrow {
            for (i in 1..numOfChannels) {
                var differentToken = "a"
                while (list.contains(differentToken)) {   //creating a random string as channel name
                    differentToken = (1..STRING_LENGTH)
                            .map { Random.nextInt(0, charPool.size) }
                            .map(charPool::get)
                            .joinToString("");
                }
                list.add(differentToken)
                courseApp.channelJoin(adminToken, "#$differentToken")
            }
        }
    }

    @Test
    fun `try to make operator with illegal token(regular)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")

        assertThrows<InvalidTokenException> {
            courseApp.channelMakeOperator("diff$adminToken", "#BadChannel", "someone" )
        }
    }

    @Test
    fun `try to make operator with illegal token(not of admin or operator)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")
        val regUserToken = courseApp.login("regUser", "pass")

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(regUserToken, "#HappyLittleChannel", "admin" )
        }
    }

    @Test
    fun `try to make operator with illegal token(is admin but not operator)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.makeAdministrator(adminToken, "regUser")
        courseApp.channelJoin(regUserToken, "#HappyLittleChannel")

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(regUserToken, "#HappyLittleChannel", "admin" )
        }
    }

    @Test
    fun `try to make operator with illegal token(operator of other channel)`() {
        val adminToken = courseApp.login("admin", "pass")
        val targetToken = courseApp.login("sadPerson", "passss1234s")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")
        courseApp.channelJoin(targetToken, "#HappyLittleChannel")

        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.makeAdministrator(adminToken, "regUser")
        courseApp.channelJoin(regUserToken, "#VerySadChannel")

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(regUserToken, "#HappyLittleChannel", "sadPerson" )
        }
    }

    @Test
    fun `admin makes himself as operator in the channel`() {
        val admin1Token = courseApp.login("admin1", "admin1")
        val admin2Token = courseApp.login("admin2", "admin2")
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel")
        courseApp.channelJoin(admin2Token, "#HappyLittleChannel")
        courseApp.makeAdministrator(admin1Token, "admin2")

        assertDoesNotThrow{
            courseApp.channelMakeOperator(admin2Token,"#HappyLittleChannel", "admin2")
            //checks if admin2 can kick admin1 from his channel:
            courseApp.channelKick(admin2Token,"#HappyLittleChannel", "admin1")

        }
    }

    @Test
    fun `operator makes himself as an operator at the same channel`() {
        val adminToken = courseApp.login("admin", "admin1")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")

        assertDoesNotThrow{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "admin")
        }
    }

    @Test
    fun `try to make operator of user that is not a member of this channel`() {
        val admin1Token = courseApp.login("admin1", "admin1")
        courseApp.login("regUser", "Usserrr")
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(admin1Token,"#HappyLittleChannel", "regUser")
        }
    }

    @Test
    fun `try to make operator of admin that is not a member of this channel`() {
        val admin1Token = courseApp.login("admin1", "admin1")
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel")
        courseApp.login("regUser", "Usserrr")
        courseApp.makeAdministrator(admin1Token, "regUser")

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(admin1Token,"#HappyLittleChannel", "regUser")
        }
    }

    @Test
    fun `try to make operator with with channel that not exits(never been)`() {
        val adminToken = courseApp.login("admin", "admin")

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#fakeNewsChannel", "someone")
        }
    }

    @Test
    fun `try to make operator with with channel that not exits(was deleted)`() {
        val adminToken = courseApp.login("admin", "admin")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")
        courseApp.channelPart(adminToken, "#HappyLittleChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "someone")
        }
    }

    @Test
    fun `try to make operator of user that is not in the system`() {
        val adminToken = courseApp.login("admin", "admin")
        courseApp.channelJoin(adminToken, "#HappyLittleChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "fakeUser")
        }
    }

    @Test
    fun `make operator of user that is a member of the channel, but is logged out`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")

        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.logout(regUserToken)

        assertDoesNotThrow{
            courseApp.channelMakeOperator(adminToken, "#greatChannel", "regUser" )
        }
    }

    @Test
    fun `try to kick user with illegal token(regular)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        assertThrows<InvalidTokenException> {
            courseApp.channelKick("1$adminToken", "#greatChannel", "someUser")
        }
    }

    @Test
    fun `try to kick user with illegal token(not operator)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelKick(regUserToken, "#greatChannel", "someUser")
        }
    }

    @Test
    fun `try to kick user with illegal token(admin)`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.makeAdministrator(adminToken, "regUser")

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelKick(regUserToken, "#greatChannel", "someUser")
        }
    }

    @Test
    fun `kick user from channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `kick another operator from channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.channelMakeOperator(adminToken, "#greatChannel", "regUser")

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `kick administrator from channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.makeAdministrator(adminToken,"regUser")

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `kick another operator that is admin(as well) from channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.makeAdministrator(adminToken,"regUser")
        courseApp.channelMakeOperator(regUserToken, "#greatChannel", "regUser")

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `operator kick himself from the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "admin")
        }
    }

    @Test
    fun `operator kick himself from the channel, and he was the last one`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "admin")
        }
    }

    @Test
    fun `try to kick from a channel that is not exists`() {
        val adminToken = courseApp.login("admin", "pass")

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#nope", "admin")
        }
    }

    @Test
    fun `try to kick from a deleted channel that is not exists`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelPart(adminToken, "#greatChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "admin")
        }
    }

    @Test
    fun `try to kick illegal user`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "nope")
        }
    }

    @Test
    fun `try to kick non member user`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#otherChannel")

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `try to kick user that was a member of the channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")
        courseApp.channelKick(adminToken, "#greatChannel", "regUser")

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser")
        }
    }

    @Test
    fun `isUserInChannel returns true for one user in channel`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertDoesNotThrow {
            val res = courseApp.isUserInChannel(adminToken, "#greatChannel", "admin"  )
            assertNotNull(res)
            assertTrue(res!!)
        }
    }

    @Test
    fun `isUserInChannel throws InvalidTokenException for non-existent token`() {

        assertThrows<InvalidTokenException> {
            courseApp.isUserInChannel("0", "#greatChannel", "admin"  )

        }
    }

    @Test
    fun `isUserInChannel throws NoSuchEntityException for non-existent channel`() {
        val adminToken = courseApp.login("admin", "pass")

        assertThrows<NoSuchEntityException> {
            courseApp.isUserInChannel(adminToken, "#greatChannel", "Asat"  )
        }
    }

    @Test
    fun `isUserInChannel throws UserNotAuthorizedException for non-member and non-administrator user`() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertThrows<UserNotAuthorizedException> {
            courseApp.isUserInChannel(regUserToken, "#greatChannel", "Asat"  )
        }
    }

    @Test
    fun `isUserInChannel return null if user not exist `() {
        val adminToken = courseApp.login("admin", "pass")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertNull {
            courseApp.isUserInChannel(adminToken, "#greatChannel", "Asat"  )
        }
    }

    @Test
    fun `isUserInChannel return false if user exists and not in channel `() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.login("regUser", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")

        assertDoesNotThrow {
            val res =   courseApp.isUserInChannel(adminToken, "#greatChannel", "regUser"  )
            assertNotNull(res)
            assertFalse(res!!)
        }
    }

    @Test
    fun `isUserInChannel return true  if user exists and  in channel `() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.login("regUser", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val regUserToken = courseApp.login("regUser", "pass")
        courseApp.channelJoin(regUserToken, "#greatChannel")

        assertDoesNotThrow {
            val res =   courseApp.isUserInChannel(regUserToken, "#greatChannel", "admin" )
            assertNotNull(res)
            assertTrue(res!!)
        }
    }

    private fun makeChannel(channelName: String,
                            godPair: Pair<String, String>,
                            members: List<Pair<String,String>>,
                            loggedOutMembersNames: List<String>) : Pair<String,MutableList<String>>{

        var tokenMembers = mutableListOf<String>()
        val godToken = courseApp.login(godPair.first, godPair.second) //assume all actions go throw him/her.

        for(pair in members) {

            val name = pair.first
            val password = pair.second
            val token = courseApp.login(name, password)

            courseApp.channelJoin(token, channelName)
            if(name in loggedOutMembersNames) {
                courseApp.channelPart(token, channelName)
            }
            else{
                tokenMembers.add( token)
            }
        }
        return Pair(godToken, tokenMembers)
    }

    @Test
    fun `get number of active users in channel with zero active users`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..10)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        courseApp.channelPart(channel.first, "#bestChannel")

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.first, "#bestChannel"), 0)
    }

    @Test
    fun `get number of active users in channel by admin that is not part of the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        courseApp.channelPart(channel.first, "#bestChannel")

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.first, "#bestChannel"), 5)
    }

    @Test
    fun `get number of active users in channel by user in the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        val adminT = channel.first
        courseApp.channelPart(adminT, "#bestChannel")

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.second[0], "#bestChannel"), 5)
    }

    @Test
    fun `try to get number of active users with illegal token`() {

        assertThrows<InvalidTokenException> {
            courseApp.numberOfActiveUsersInChannel("thereAreNoTokens", "what__123ever#")
        }
    }

    @Test
    fun `try to get number of active users with channel that is not exits`() {
        val adminToken = courseApp.login("admin", "pass")

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfActiveUsersInChannel(adminToken, "what__123ever#")
        }
    }

    @Test
    fun `try to get number of active users with channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelPart(adminToken, "#greatChannel")

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfActiveUsersInChannel(adminToken, "#greatChannel")
        }
    }

    @Test
    fun `try to get number of active users with token that is not a member of the channel and not admin`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val token = courseApp.login("regUser", "noAccessTOQuery")

        assertThrows<UserNotAuthorizedException> {
            courseApp.numberOfActiveUsersInChannel(token, "#greatChannel")
        }
    }

    @Test
    fun `get number of total users in channel by admin that is not part of the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        courseApp.channelPart(channel.first, "#bestChannel")

        assertEquals(courseApp.numberOfTotalUsersInChannel(channel.first, "#bestChannel"), 10)
    }

    @Test
    fun `get number of total users in channel by user in the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        val adminT = channel.first
        courseApp.channelPart(adminT, "#bestChannel")

        assertEquals(courseApp.numberOfTotalUsersInChannel(channel.second[0], "#bestChannel"), 10)
    }

    @Test
    fun `try to get total of active users with illegal token`() {
        assertThrows<InvalidTokenException> {
            courseApp.numberOfTotalUsersInChannel("thereAreNoTokens", "what__123ever#")
        }
    }

    @Test
    fun `try to get total of active users with channel that is not exits`() {
        val adminToken = courseApp.login("admin", "pass")

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfTotalUsersInChannel(adminToken, "what__123ever#")
        }
    }

    @Test
    fun `try to get total of active users with channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        courseApp.channelPart(adminToken, "#greatChannel")

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfTotalUsersInChannel(adminToken, "#greatChannel")
        }
    }

    @Test
    fun `try to get total of active users with token that is not a member of the channel and not admin`() {
        val adminToken = courseApp.login("admin", "pass")
        courseApp.channelJoin(adminToken, "#greatChannel")
        val token = courseApp.login("regUser", "noAccessTOQuery")

        assertThrows<UserNotAuthorizedException> {
            courseApp.numberOfTotalUsersInChannel(token, "#greatChannel")
        }
    }

/*
    @Test
    fun `joining user to 128 channels`() {

    }

    @Test
    fun `stress test for channels in the system`() {

    }
    */
}