package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.tests.isFalse
import il.ac.technion.cs.softwaredesign.tests.isTrue
import il.ac.technion.cs.softwaredesign.tests.joinException
import il.ac.technion.cs.softwaredesign.tests.runWithTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.assertThrows
import java.time.Duration.ofSeconds
import kotlin.random.Random

class CourseAppTest {
    private val injector = Guice.createInjector(CourseAppModule(), FakeSecureStorageModule())

    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppReboot = injector.getInstance<CourseApp>()

    init {
        courseAppInitializer.setup()
    }

    @Test
    @Order(1)
    fun `check if user is logged in`() {
        val loginToken = courseApp.login("user1", "pass1").get()

        assert(courseApp.isUserLoggedIn(loginToken, "user1").join()!!)
    }

    @Test
    @Order(2)
    fun `login and logout user`() {
        val loginToken1 = courseApp.login("user1", "pass1").get()
        val loginToken2 = courseApp.login("user2", "pass2").get()

        courseApp.logout(loginToken2)

        assert(courseApp.isUserLoggedIn(loginToken1, "user2").join() == false)
    }

    @Test
    @Order(3)
    fun `after login, a user is logged in`() {
        courseApp.login("gal", "hunter2").get()
        courseApp.login("imaman", "31337").get()

        val token = courseApp.login("matan", "s3kr1t").get()

        assertThat(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal").join() },
                present(isTrue))
    }

    @Test
    @Order(4)
    fun `an authentication token is invalidated after logout`() {
        val token = courseApp.login("matan", "s3kr1t").get()

        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> {
            runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "matan").joinException() }
        }
    }

    @Test
    @Order(5)
    fun `log in user twice should throw exception`(){
        courseApp.login("matan", "s3kr1t").get()

        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "s3kr1t").joinException() }
        }
    }

    @Test
    @Order(6)
    fun `log in user twice and other users log in between should throw exception`(){
        courseApp.login("matan", "s3kr1t").get()
        courseApp.login("imaman", "31337").get()
        courseApp.login("gal", "hunter2").get()
        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "s3kr1t").joinException() }
        }
    }

    @Test
    @Order(7)
    fun `user log in then log out isUserLoggedIn returns False`(){
        val token = courseApp.login("matan", "s3kr1t").get()
        val token2 = courseApp.login("imaman", "31337").get()
        courseApp.logout(token2)
        assertThat(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "imaman").join() },
                present(isFalse))

    }

    @Test
    @Order(8)
    fun `user never been in the system isUserLoggedIn returns null`(){
        val token = courseApp.login("matan", "s3kr1t").get()

        assertNull(runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "imaman").join() })

    }

    @Test
    @Order(9)
    fun `log out a none exist user `(){
        val strLen = 100
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = courseApp.login("matan", "s3kr1t").get()
        var differentToken : String = token
        //creating a random token for consistency and validity
        while (differentToken == token) {
            differentToken = (1..strLen)
                    .map { Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
        }

        assertThrows<InvalidTokenException> {runWithTimeout(ofSeconds(10)) { courseApp.logout(differentToken).joinException() }}

    }

    @Test
    @Order(10)
    fun `registered user log in with wrong password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t").get()
        courseApp.logout(token).get()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password").joinException() }
        }
    }

    @Test
    @Order(11)
    fun `registered user log in with another logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t").get()
        courseApp.login("imaman", "another_user_password").get()
        courseApp.logout(token).get()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "another_user_password").joinException() }
        }
    }

    @Test
    @Order(12)
    fun `registered user log in with another registered not logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t").get()
        val token2 = courseApp.login("imaman", "another_user_password").get()
        courseApp.logout(token).get()
        courseApp.logout(token2).get()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password").joinException() }
        }
    }

    @Test
    @Order(13)
    fun `user stays logged in after reboot, assumes his token stays active`(){
        val token = courseApp.login("matan", "s3kr1t").get()

        assertThat(runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan").join() },
                present(isTrue))
    }

    @Test
    @Order(14)
    fun `an authentication token is invalidated after logout, stays that way after reboot`(){
        val token = courseApp.login("matan", "s3kr1t").get()
        courseApp.logout(token).get()

        assertThrows<InvalidTokenException> {
            runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan").joinException() == false }
        }
    }

    @Test
    @Order(15)
    fun `password is valid`(){
        val token1 = courseApp.login("matan", "StrongPass").get()
        courseApp.logout(token1).get()

        assertDoesNotThrow { courseApp.login("matan", "StrongPass").joinException() }
    }

    @Test
    @Order(16)
    fun `password is invalid`(){
        val token1 = courseApp.login("matan", "StrongPass").get()
        courseApp.logout(token1).get()

        assertThrows<NoSuchEntityException> { courseApp.login("matan", "OtherStrongPass").joinException() }
    }

    @Test
    @Order(17)
    fun `token is illegal at logout`(){
        val token = courseApp.login("matan", "StrongPass").get()

        assertThrows<InvalidTokenException> { courseApp.logout("WrongToken$token").joinException() }
    }

    @Test
    @Order(18)
    fun `token is illegal at is isUserLoggedin`(){
        val token = courseApp.login("matan", "StrongPass").get()

        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn("WrongToken$token", "matan").joinException() }
    }

    @Test
    @Order(19)
    fun `Unregistered user returns null upon isUserLogIn check`(){
        val token = courseApp.login("matan", "StrongPass").get()

        assert(courseApp.isUserLoggedIn(token, "natam").join() == null)
    }

    @Test
    @Order(20)
    fun `token is invalid after logout`(){
        val token1 = courseApp.login("matan2", "StrongPass").get()
        val token2 = courseApp.login("matan", "StrongPass").get()
        courseApp.logout(token2).get()

        assert(token1 != token2)
        assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(token2, "matan2").joinException() }
    }

    @Test
    @Order(21)
    fun `enable to login same user after reboot`(){
        courseApp.login("matan", "StrongPass").get()

        assertThrows<UserAlreadyLoggedInException> { courseAppReboot.login("matan", "StrongPass").joinException() }
    }

    @Test
    @Order(22)
    fun `system is persistent after reboot`(){
        val tokens = ArrayList<String>(11)
        for (i in 0..10){
            tokens.add(courseApp.login("user$i", "pass$i").get())
        }
        for (i in 0..5)
            courseApp.logout(tokens[i]).get()

        for (i in 0..5){
            assertThrows<InvalidTokenException> { courseAppReboot.isUserLoggedIn(tokens[i], "matan$i").joinException() }
            assertDoesNotThrow { courseAppReboot.login("matan$i", "StrongPass$i").joinException() }
        }
        for (i in 6..10){
            assertDoesNotThrow { courseAppReboot.isUserLoggedIn(tokens[i], "matan$i").joinException() }
        }
    }

    @Test
    @Order(23)
    fun `corrupted(prefix & suffix) token pass over`(){
        val token = courseApp.login("matan", "StrongPass").get()

        for (i in 1..20) {      // for testing couple of random suffix & prefix of the token
            val prefixToken = token.substring(0, (0..(token.length - 1)).shuffled().first())
            val suffixToken = token.substring((1..token.length).shuffled().first(), token.length)
            assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(prefixToken, "matan").joinException() }
            assertThrows<InvalidTokenException> { courseApp.isUserLoggedIn(suffixToken, "matan").joinException() }
        }
    }

    @Test
    @Order(24)
    fun `login very long username and password`(){
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"

        for (i in 1..50) {
            val username = (1..(500..1000).random()).map { allowedChars.random() }.joinToString("")
            val password = (1..(500..1000).random()).map { allowedChars.random() }.joinToString("")
            assertDoesNotThrow { courseApp.login(username, password).joinException() }
        }
    }

    @Test
    @Order(25)
    fun `trying to login with wrong pass`(){
        val token = courseApp.login("matan", "StrongPass").get()
        courseApp.logout(token).get()

        assertThrows<NoSuchEntityException> { courseApp.login("matan", "WrongPass").joinException() }
    }

    @Test
    @Order(26)
    fun `operator title is removed after leaving the channel` () {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#TestChannel").get()
        val operatorToken = courseApp.login("Oper", "pass").get()
        courseApp.channelJoin(operatorToken, "#TestChannel").get()

        courseApp.channelMakeOperator(adminToken, "#TestChannel", "Oper").get()
        courseApp.channelPart(operatorToken, "#TestChannel").get()
        courseApp.channelJoin(operatorToken, "#TestChannel").get()

        assertThrows<UserNotAuthorizedException> { courseApp.channelKick(operatorToken, "#TestChannel", "admin").joinException() }
    }

    @Test
    @Order(27)
    fun `first user in the system becomes administrator automatically`() {
        val adminToken = courseApp.login("firstUser", "pass").get()
        courseApp.login("regUser", "pass2").get()

        assertDoesNotThrow { courseApp.makeAdministrator(adminToken, "regUser").joinException() }
    }

    @Test
    @Order(28)
    fun `makeAdministrator gets invalid token throws InvalidTokenException`() {
        val strLen = 20
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val token = courseApp.login("matan", "s3kr1t").get()
        var differentToken : String = token
        while (differentToken == token) {   //creating a random token for consistency and validity
            differentToken = (1..strLen)
                    .map { kotlin.random.Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
        }

        assertThrows<InvalidTokenException> {courseApp.makeAdministrator(differentToken, "matam").joinException()}
    }

    @Test
    @Order(29)
    fun `makeAdministrator gets token that is not associated to administrator`() {
        courseApp.login("firstUser", "pass").get()
        val userToken = courseApp.login("regUser", "pass2").get()
        courseApp.login("otherUser", "pass3").get()

        assertThrows<UserNotAuthorizedException> { courseApp.makeAdministrator(userToken, "otherUser").joinException() }
    }

    @Test
    @Order(30)
    fun `makeAdministrator gets invalid user`() {
        val adminToken = courseApp.login("firstUser", "pass").get()
        courseApp.login("regUser", "pass2").get()

        assertThrows<NoSuchEntityException> { courseApp.makeAdministrator(adminToken, "InvalidUser").joinException() }
    }

    @Test
    @Order(31)
    fun `makeAdministrator makes administrator, an admin again`() {
        val adminToken = courseApp.login("firstUser", "pass").get()
        val userToken = courseApp.login("regUser", "pass2").get()

        courseApp.makeAdministrator(adminToken, "regUser").get()

        assertDoesNotThrow { courseApp.makeAdministrator(userToken, "firstUser").joinException() }
    }

    @Test
    @Order(32)
    fun `administrator retain their status after relogging`() {
        val token = courseApp.login("matan", "s3kr1t").get()
        courseApp.makeAdministrator(token, "matan").get()
        courseApp.logout(token).get()
        val secondToken = courseApp.login("matan", "s3kr1t").get()
        courseApp.login("Ron", "s3kwwwr1t").get()
        assertDoesNotThrow{ courseApp.makeAdministrator(secondToken,"Ron").joinException()}
    }

    @Test
    @Order(33)
    fun `administrator retain their status after system reboot`() {
        val token = courseApp.login("matan", "s3kr1t").get()
        courseApp.makeAdministrator(token, "matan").get()
        courseApp.login("Ron", "s3kwwwr1t").get()
        assertDoesNotThrow{ courseAppReboot.makeAdministrator(token,"Ron").joinException()}
    }

    @Test
    @Order(34)
    fun `make random number of administrator in the system`() {
        val numOfAdmins = Random.nextInt(1, 100)
        val tokenDict = mutableMapOf<Int, String>()
        for (i in 1..100) {
            tokenDict[i] = courseApp.login("user$i", "pass").get()
        }

        assertDoesNotThrow {
            for (i in 1..numOfAdmins) {
                courseApp.makeAdministrator(tokenDict[1]!!, "user$i").joinException()
            }
        }
    }

    @Test
    @Order(35)
    fun `make administrator of logged out user`() {
        val token = courseApp.login("matan", "s3kr1t").get()
        val ronToken = courseApp.login("Ron", "s3kwwwr1t").get()
        courseApp.logout(ronToken).get()
        assertDoesNotThrow{
            courseApp.makeAdministrator(token, "Ron").joinException()
        }
    }

    @Test
    @Order(36)
    fun `create channel`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertDoesNotThrow {courseApp.channelJoin(adminToken, "#greatChannel").joinException()}
    }

    @Test
    @Order(37)
    fun `create channel with illegal name`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "wrongName").joinException() }
        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "wrong#Name").joinException() }
        assertThrows<NameFormatException> { courseApp.channelJoin(adminToken, "###dollarSign$$").joinException() }
    }

    @Test
    @Order(38)
    fun `invalid token(not associated to no one) tries to join to channel`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss").get()
        courseApp.channelJoin(adminT, "#AvlLovers").get()
        assertThrows<InvalidTokenException> {courseApp.channelJoin("different$adminT", "#AvlLovers").joinException()}
    }

    @Test
    @Order(39)
    fun `invalid token(not of admin) tries to create new channel`() {
        courseApp.login("Ron", "IreallyLikeAvlss").get()
        val notAdminToken = courseApp.login("Person", "passssss").get()
        assertThrows<UserNotAuthorizedException> {courseApp.channelJoin(notAdminToken, "#SomeChannel#___").joinException()}
    }

    @Test
    @Order(40)
    fun `first user in the channel is a Operator`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertDoesNotThrow{ courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException() }
    }

    @Test
    @Order(41)
    fun `add up to 512 users to the channel`() {
        val numInChannel = Random.nextInt(1, 512)
        val tokenDict = mutableMapOf<Int, String>()
        for (i in 1..512) {
            tokenDict[i] = courseApp.login("user$i", "pass").get()
        }

        assertDoesNotThrow {
            for (i in 1..numInChannel) {
                var str = ""
                for (j in 1..i) {
                    str += "X"
                }
                courseApp.channelJoin(tokenDict[1]!!, "#channel$str").joinException()
            }
        }
    }

    @Test
    @Order(42)
    fun `leave channel with illegal token(existing channel)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#TestChannel").get()
        courseApp.login("reg", "pass").get()

        assertThrows<InvalidTokenException> { courseApp.channelKick("SomeOtherToken", "#TestChannel", "reg").joinException() }
    }

    @Test
    @Order(43)
    fun `leave channel with illegal token(not existing channel)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#TestChannel").get()
        courseApp.login("reg", "pass").get()

        assertThrows<InvalidTokenException> { courseApp.channelKick("SomeOtherToken", "#NotExistingChannel", "reg").joinException() }
    }

    @Test
    @Order(44)
    fun `trying to leave channel with token that is not member of the channel`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss").get()
        courseApp.channelJoin(adminT, "#AvlLovers").get()
        val t1 = courseApp.login("p1", "Ifffs").get()
        assertThrows<NoSuchEntityException> {
            courseApp.channelPart(t1,"#AvlLovers").joinException()
        }
    }

    @Test
    @Order(45)
    fun `leave channel with valid token but the channel does not exist`() {
        val adminT = courseApp.login("Ron", "IreallyLikeAvlss").get()
        courseApp.channelJoin(adminT, "#AvlLovers").get()
        assertThrows<NoSuchEntityException> {
            courseApp.channelPart(adminT,"#AILovers").joinException()
        }
    }

    @Test
    @Order(46)
    fun `regular member leaves channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertDoesNotThrow { courseApp.channelPart(regUserToken, "#greatChannel").joinException() }
    }

    @Test
    @Order(47)
    fun `operator leave the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        val operatorToken = courseApp.login("operator", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.channelJoin(operatorToken, "#greatChannel").get()
        courseApp.channelMakeOperator(adminToken, "#greatChannel", "operator").get()

        assertDoesNotThrow { courseApp.channelPart(operatorToken, "#greatChannel").joinException() }
    }

    @Test
    @Order(48)
    fun `administrator that is not an operator of this channel leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        val otherAdmin = courseApp.login("otherAdmin", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.makeAdministrator(adminToken, "otherAdmin").get()
        courseApp.channelJoin(otherAdmin, "#greatChannel").get()

        assertDoesNotThrow { courseApp.channelPart(otherAdmin, "#greatChannel").joinException() }
    }

    @Test
    @Order(49)
    fun ` channel does not contains operators`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertDoesNotThrow { courseApp.channelPart(adminToken, "#greatChannel").joinException() }

    }

    @Test
    @Order(50)
    fun `last user(regular) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.channelPart(adminToken, "#greatChannel").get()
        assertDoesNotThrow {courseApp.channelPart(regUserToken, "#greatChannel").joinException()}

    }

    @Test
    @Order(51)
    fun `last user(operator) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.channelMakeOperator(adminToken,"#greatChannel", "regUser" ).get()
        courseApp.channelPart(adminToken, "#greatChannel").get()

        assertDoesNotThrow { courseApp.channelPart(regUserToken, "#greatChannel").joinException()}

    }

    @Test
    @Order(52)
    fun `last user(administrator) leaves the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertDoesNotThrow { courseApp.channelPart(adminToken, "#greatChannel")}
    }

    @Test
    @Order(53)
    fun `trying to join to channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelPart(adminToken, "#greatChannel").get()

        val regUserToken = courseApp.login("regUser", "pass").get()
        assertThrows<UserNotAuthorizedException> { courseApp.channelJoin(regUserToken, "#greatChannel").joinException()}
    }

    @Test
    @Order(54)
    fun `make random number of channels`() {
        val numOfChannels = Random.nextInt(1, 100)
        val adminToken = courseApp.login("admin", "pass").get()
        val strLen = 20
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
        val list  = mutableListOf<String>()

        assertDoesNotThrow {
            for (i in 1..numOfChannels) {
                var differentToken = "a"
                while (list.contains(differentToken)) {   //creating a random string as channel name
                    differentToken = (1..strLen)
                            .map { Random.nextInt(0, charPool.size) }
                            .map(charPool::get)
                            .joinToString("")
                }
                list.add(differentToken)
                courseApp.channelJoin(adminToken, "#$differentToken").joinException()
            }
        }
    }

    @Test
    @Order(55)
    fun `try to make operator with illegal token(regular)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()

        assertThrows<InvalidTokenException> {
            courseApp.channelMakeOperator("diff$adminToken", "#BadChannel", "someone" ).joinException()
        }
    }

    @Test
    @Order(56)
    fun `try to make operator with illegal token(not of admin or operator)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(regUserToken, "#HappyLittleChannel", "admin" ).joinException()
        }
    }

    @Test
    @Order(57)
    fun `try to make operator with illegal token - is admin but not operator`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()
        val otherAdminToken = courseApp.login("otherAdmin", "pass").get()
        courseApp.makeAdministrator(adminToken, "otherAdmin").get()
        courseApp.channelJoin(otherAdminToken, "#HappyLittleChannel").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(otherAdminToken, "#HappyLittleChannel", "admin" ).joinException()
        }
    }

    @Test
    @Order(58)
    fun `try to make operator with illegal token(operator of other channel)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val targetToken = courseApp.login("sadPerson", "passss1234s").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()
        courseApp.channelJoin(targetToken, "#HappyLittleChannel").get()

        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.makeAdministrator(adminToken, "regUser").get()
        courseApp.channelJoin(regUserToken, "#VerySadChannel").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelMakeOperator(regUserToken, "#HappyLittleChannel", "sadPerson").joinException()
        }
    }

    @Test
    @Order(59)
    fun `admin makes himself as operator in the channel`() {
        val admin1Token = courseApp.login("admin1", "admin1").get()
        val admin2Token = courseApp.login("admin2", "admin2").get()
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel").get()
        courseApp.channelJoin(admin2Token, "#HappyLittleChannel").get()
        courseApp.makeAdministrator(admin1Token, "admin2").get()

        assertDoesNotThrow{
            courseApp.channelMakeOperator(admin2Token,"#HappyLittleChannel", "admin2").joinException()
            //checks if admin2 can kick admin1 from his channel:
            courseApp.channelKick(admin2Token,"#HappyLittleChannel", "admin1").joinException()

        }
    }

    @Test
    @Order(60)
    fun `operator makes himself as an operator at the same channel`() {
        val adminToken = courseApp.login("admin", "admin1").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()

        assertDoesNotThrow{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "admin").joinException()
        }
    }

    @Test
    @Order(61)
    fun `try to make operator of user that is not a member of this channel`() {
        val admin1Token = courseApp.login("admin1", "admin1").get()
        courseApp.login("regUser", "Usserrr").get()
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(admin1Token,"#HappyLittleChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(62)
    fun `try to make operator of admin that is not a member of this channel`() {
        val admin1Token = courseApp.login("admin1", "admin1").get()
        courseApp.channelJoin(admin1Token, "#HappyLittleChannel").get()
        courseApp.login("regUser", "Usserrr").get()
        courseApp.makeAdministrator(admin1Token, "regUser").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(admin1Token,"#HappyLittleChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(63)
    fun `try to make operator with with channel that not exits(never been)`() {
        val adminToken = courseApp.login("admin", "admin").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#fakeNewsChannel", "someone").joinException()
        }
    }

    @Test
    @Order(64)
    fun `try to make operator with with channel that not exits(was deleted)`() {
        val adminToken = courseApp.login("admin", "admin").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()
        courseApp.channelPart(adminToken, "#HappyLittleChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "someone").joinException()
        }
    }

    @Test
    @Order(65)
    fun `try to make operator of user that is not in the system`() {
        val adminToken = courseApp.login("admin", "admin").get()
        courseApp.channelJoin(adminToken, "#HappyLittleChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelMakeOperator(adminToken,"#HappyLittleChannel", "fakeUser").joinException()
        }
    }

    @Test
    @Order(66)
    fun `make operator of user that is a member of the channel, but is logged out`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()

        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.logout(regUserToken).get()

        assertDoesNotThrow{
            courseApp.channelMakeOperator(adminToken, "#greatChannel", "regUser" ).joinException()
        }
    }

    @Test
    @Order(67)
    fun `try to kick user with illegal token(regular)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        assertThrows<InvalidTokenException> {
            courseApp.channelKick("1$adminToken", "#greatChannel", "someUser").joinException()
        }
    }

    @Test
    @Order(68)
    fun `try to kick user with illegal token(not operator)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelKick(regUserToken, "#greatChannel", "someUser").joinException()
        }
    }

    @Test
    @Order(69)
    fun `try to kick user with illegal token(admin)`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.makeAdministrator(adminToken, "regUser").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.channelKick(regUserToken, "#greatChannel", "someUser").joinException()
        }
    }

    @Test
    @Order(70)
    fun `kick user from channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(71)
    fun `kick another operator from channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.channelMakeOperator(adminToken, "#greatChannel", "regUser").get()

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(72)
    fun `kick administrator from channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.makeAdministrator(adminToken,"regUser").get()

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(73)
    fun `kick another operator that is admin(as well) from channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.makeAdministrator(adminToken,"regUser").get()
        courseApp.channelMakeOperator(regUserToken, "#greatChannel", "regUser").get()

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(74)
    fun `operator kick himself from the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "admin").joinException()
        }
    }

    @Test
    @Order(75)
    fun `operator kick himself from the channel, and he was the last one`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertDoesNotThrow{
            courseApp.channelKick(adminToken, "#greatChannel", "admin").joinException()
        }
    }

    @Test
    @Order(76)
    fun `try to kick from a channel that is not exists`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#nope", "admin").joinException()
        }
    }

    @Test
    @Order(77)
    fun `try to kick from a deleted channel that is not exists`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelPart(adminToken, "#greatChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "admin").joinException()
        }
    }

    @Test
    @Order(78)
    fun `try to kick illegal user`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "nope").joinException()
        }
    }

    @Test
    @Order(79)
    fun `try to kick non member user`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(adminToken, "#otherChannel").get()
        courseApp.channelJoin(regUserToken, "#otherChannel").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(80)
    fun `try to kick user that was a member of the channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()
        courseApp.channelKick(adminToken, "#greatChannel", "regUser").get()

        assertThrows<NoSuchEntityException>{
            courseApp.channelKick(adminToken, "#greatChannel", "regUser").joinException()
        }
    }

    @Test
    @Order(81)
    fun `isUserInChannel returns true for one user in channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertDoesNotThrow {
            val res = courseApp.isUserInChannel(adminToken, "#greatChannel", "admin").joinException()
            assertNotNull(res)
            assertTrue(res!!)
        }
    }

    @Test
    @Order(82)
    fun `isUserInChannel throws InvalidTokenException for non-existent token`() {

        assertThrows<InvalidTokenException> {
            courseApp.isUserInChannel("0", "#greatChannel", "admin").joinException()

        }
    }

    @Test
    @Order(83)
    fun `isUserInChannel throws NoSuchEntityException for non-existent channel`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertThrows<NoSuchEntityException> {
            courseApp.isUserInChannel(adminToken, "#greatChannel", "Asat").joinException()
        }
    }

    @Test
    @Order(84)
    fun `isUserInChannel throws UserNotAuthorizedException for non-member and non-administrator user`() {
        val adminToken = courseApp.login("admin", "pass").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.isUserInChannel(regUserToken, "#greatChannel", "Asat").joinException()
        }
    }

    @Test
    @Order(85)
    fun `isUserInChannel return null if user not exist `() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assert (courseApp.isUserInChannel(adminToken, "#greatChannel", "Asat").joinException() == null)
    }

    @Test
    @Order(86)
    fun `isUserInChannel return false if user exists and not in channel `() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()

        assertDoesNotThrow {
            val res =   courseApp.isUserInChannel(adminToken, "#greatChannel", "regUser").joinException()
            assertNotNull(res)
            assertFalse(res!!)
        }
    }

    @Test
    @Order(87)
    fun `isUserInChannel return true if user exists and in channel`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val regUserToken = courseApp.login("regUser", "pass").get()
        courseApp.channelJoin(regUserToken, "#greatChannel").get()

        assertDoesNotThrow {
            val res =   courseApp.isUserInChannel(regUserToken, "#greatChannel", "admin").joinException()
            assertNotNull(res)
            assertTrue(res!!)
        }
    }

    private fun makeChannel(channelName: String,
                            godPair: Pair<String, String>,
                            members: List<Pair<String,String>>,
                            loggedOutMembersNames: List<String>) : Pair<String,MutableList<String>>{

        val tokenMembers = mutableListOf<String>()
        val godToken = courseApp.login(godPair.first, godPair.second).get() //assume all actions go throw him/her.
        courseApp.channelJoin(godToken, channelName).get()

        for(pair in members) {

            val name = pair.first
            val password = pair.second
            val token = courseApp.login(name, password).get()

            courseApp.channelJoin(token, channelName).get()
            if(name in loggedOutMembersNames) {
                courseApp.logout(token).get()
            }
            else{
                tokenMembers.add(token)
            }
        }
        return Pair(godToken, tokenMembers)
    }

    @Test
    @Order(88)
    fun `get number of active users in channel with zero active users`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..10)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut)
        courseApp.channelPart(channel.first, "#bestChannel").get()

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.first, "#bestChannel").get(), 0)
    }

    @Test
    @Order(89)
    fun `get number of active users in channel by admin that is not part of the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut)
        courseApp.channelPart(channel.first, "#bestChannel").get()

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.first, "#bestChannel").joinException(), 5)
    }

    @Test
    @Order(90)
    fun `get number of active users in channel by user in the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut)
        val adminT = channel.first
        courseApp.channelPart(adminT, "#bestChannel").get()

        assertEquals(courseApp.numberOfActiveUsersInChannel(channel.second[0], "#bestChannel").joinException(), 5)
    }

    @Test
    @Order(91)
    fun `try to get number of active users with illegal token`() {

        assertThrows<InvalidTokenException> {
            courseApp.numberOfActiveUsersInChannel("thereAreNoTokens", "what__123ever#").joinException()
        }
    }

    @Test
    @Order(92)
    fun `try to get number of active users with channel that is not exits`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfActiveUsersInChannel(adminToken, "what__123ever#").joinException()
        }
    }

    @Test
    @Order(93)
    fun `try to get number of active users with channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelPart(adminToken, "#greatChannel").get()

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfActiveUsersInChannel(adminToken, "#greatChannel").joinException()
        }
    }

    @Test
    @Order(94)
    fun `try to get number of active users with token that is not a member of the channel and not admin`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val token = courseApp.login("regUser", "noAccessTOQuery").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.numberOfActiveUsersInChannel(token, "#greatChannel").joinException()
        }
    }

    @Test
    @Order(95)
    fun `get number of total users in channel by admin that is not part of the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        courseApp.channelPart(channel.first, "#bestChannel").get()

        assertEquals(courseApp.numberOfTotalUsersInChannel(channel.first, "#bestChannel").joinException(), 10)
    }

    @Test
    @Order(96)
    fun `get number of total users in channel by user in the channel`() {
        val listNames = mutableListOf<Pair<String,String>>()
        for(i in 1..10)
            listNames.add(Pair("User$i", "Pass$i"))
        val listLoggedOut = mutableListOf<String>()
        for(i in 1..5)
            listLoggedOut.add("User$i")

        val channel = makeChannel("#bestChannel", Pair("admin", "admin"),listNames, listLoggedOut )
        val adminT = channel.first
        courseApp.channelPart(adminT, "#bestChannel").get()

        assertEquals(courseApp.numberOfTotalUsersInChannel(channel.second[0], "#bestChannel").joinException(), 10)
    }

    @Test
    @Order(97)
    fun `try to get total of active users with illegal token`() {
        assertThrows<InvalidTokenException> {
            courseApp.numberOfTotalUsersInChannel("thereAreNoTokens", "what__123ever#").joinException()
        }
    }

    @Test
    @Order(98)
    fun `try to get total of active users with channel that is not exits`() {
        val adminToken = courseApp.login("admin", "pass").get()

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfTotalUsersInChannel(adminToken, "what__123ever#").joinException()
        }
    }

    @Test
    @Order(98)
    fun `try to get total of active users with channel that was deleted`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        courseApp.channelPart(adminToken, "#greatChannel").get()

        assertThrows<NoSuchEntityException> {
            courseApp.numberOfTotalUsersInChannel(adminToken, "#greatChannel").joinException()
        }
    }

    @Test
    @Order(99)
    fun `try to get total of active users with token that is not a member of the channel and not admin`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#greatChannel").get()
        val token = courseApp.login("regUser", "noAccessTOQuery").get()

        assertThrows<UserNotAuthorizedException> {
            courseApp.numberOfTotalUsersInChannel(token, "#greatChannel").joinException()
        }
    }

    @Test
    @Order(100)
    fun `operator title is still valid after relogging`() {
        val adminToken = courseApp.login("admin", "pass").get()
        courseApp.channelJoin(adminToken, "#TestChannel").get()
        val operatorToken = courseApp.login("Oper", "pass").get()
        courseApp.channelJoin(operatorToken, "#TestChannel").get()

        courseApp.channelMakeOperator(adminToken, "#TestChannel", "Oper").get()
        courseApp.logout(operatorToken).get()
        val operatorToken2 = courseApp.login("Oper", "pass").get()

        assertDoesNotThrow { courseApp.channelKick(operatorToken2, "#TestChannel", "admin").joinException() }
    }

    @Test
    @Order(101)
    fun `selected amount of users are logged in at once - stress time test`() {
        val amount = 5000
        runWithTimeout(ofSeconds(10)) {
            for (i in 1..amount) {
                val username = "user$i"
                val password = "StrongPass$i"
                courseApp.login(username, password).get()
            }

            val token = courseAppReboot.login("heroUser", "password").get()
            for (i in 1..amount) {
                assertDoesNotThrow { courseAppReboot.isUserLoggedIn(token, "user$i").joinException() }
            }
        }
    }

    @Test
    @Order(102)
    fun `1,000,000 logged in users in system - stress test`() {
        val amount = 1000000
        runWithTimeout(ofSeconds(60*10)) {
            for (i in 1..amount) {
                val username = "user" + i
                val password = "StrongPass" + i
                courseApp.login(username, password).get()
                if (i % 1000 == 0) println("login: $i")
            }
            val token = courseAppReboot.login("heroUser", "password").get()
            for (i in 1..amount) {
                assertDoesNotThrow { courseAppReboot.isUserLoggedIn(token, "user" + i).joinException() }
            }
        }
    }

    @Test
    @Order(103)
    fun `joining user to 128 channels`() {
        val amount = 10
        val usersNumbers = 500
        val dict = HashMap<Int, String>()
        assertDoesNotThrow {
            for (i in 1..usersNumbers) {
                val username = "user$i"
                val password = "StrongPass$i"
                dict[i] = courseApp.login(username, password).get()
                for (j in 1..amount) {
                    var str = ""
                    for (t in 1..j) {
                        str += "X"
                    }
                    courseApp.channelJoin(dict[i]!!, "#channel$str").joinException()
                }
            }
        }
    }

    @Test
    @Order(104)
    fun `stress test for 1,000,000 users and 100,000 channels in the system`() {
        val amount = 1000000
        val dict = HashMap<Int, String>()
        runWithTimeout(ofSeconds(60*10)) {
            for (i in 1..amount) {
                val username = "user" + i
                val password = "StrongPass" + i
                dict[i] = courseApp.login(username, password).get()
                if (i % 1000 == 0) println("login: $i")
            }
            val token = courseAppReboot.login("heroUser", "password").get()
            for (i in 1..amount) {
                assertDoesNotThrow { courseAppReboot.isUserLoggedIn(token, "user" + i).joinException() }
            }


            for (i in 1..amount step 512) {
                for (channel in 1..128) {
                    courseApp.makeAdministrator(dict[1]!!, "user$i")
                    for (user in i..i+511) {
                        courseApp.channelJoin(dict[user]!!, "#channel${i}#$channel")
                    }
                }
                println ("i: $i")
            }
        }
    }
}
