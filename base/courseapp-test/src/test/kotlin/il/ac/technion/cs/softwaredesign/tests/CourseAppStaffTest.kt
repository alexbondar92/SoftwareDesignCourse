package il.ac.technion.cs.softwaredesign.tests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration.ofSeconds
import il.ac.technion.cs.softwaredesign.storage.read
import il.ac.technion.cs.softwaredesign.storage.write
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.random.Random


class CourseAppStaffTest {
    private val courseAppInitializer = CourseAppInitializer()
    private val storageMock= mutableMapOf<String,String>()
    init {
        courseAppInitializer.setup()
        mockkStatic("il.ac.technion.cs.softwaredesign.storage.SecureStorageKt")
        val keySlot = slot<ByteArray>()
        val valueSlot = slot<ByteArray>()
        every{
            write(capture(keySlot),capture(valueSlot))
        } answers {
            val value = String(valueSlot.captured)
            val key = String(keySlot.captured)
            storageMock[key] = value
        }
        every{
            read(capture(keySlot))
        } answers {
            val key = String(keySlot.captured)
            val value = storageMock[key]
            Thread.sleep(value?.toByteArray()?.size?.toLong() ?: 0)
            storageMock[key]?.toByteArray()
        }
    }

    private val courseApp = CourseApp()

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

        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseApp.isUserLoggedIn(token, "matan") }
        }
    }

    @Test
    fun `log in user twice should throw exception`(){
        courseApp.login("matan", "s3kr1t")
        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "s3kr1t") }
        }
    }

    @Test
    fun `log in user twice and other users log in between should throw exception`(){
        courseApp.login("matan", "s3kr1t")
        courseApp.login("imaman", "31337")
        courseApp.login("gal", "hunter2")
        assertThrows<IllegalArgumentException> {
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
                .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("");
        }

        assertThrows<IllegalArgumentException> {runWithTimeout(ofSeconds(10)) { courseApp.logout(differentToken) }}

    }

    @Test
    fun `registered user log in with wrong password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.logout(token)

        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password") }
        }
    }

    @Test
    fun `registered user log in with another logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        courseApp.login("imaman", "another_user_password")
        courseApp.logout(token)

        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "another_user_password") }
        }
    }

    @Test
    fun `registered user log in with another registered not logged in user s password should throw exception`(){
        val token = courseApp.login("matan", "s3kr1t")
        val token2 = courseApp.login("imaman", "another_user_password")
        courseApp.logout(token)
        courseApp.logout(token2)

        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseApp.login("matan", "bad_password") }
        }
    }

    @Test
    fun `user stays logged in after reboot, assumes his token stays active`(){
        var courseAppReboot = CourseApp()
        val token = courseAppReboot.login("matan", "s3kr1t")

        courseAppReboot = CourseApp()   //the "REBOOT"

        assertThat(runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan") },
            present(isTrue))
    }

    @Test
    fun `an authentication token is invalidated after logout, stays that way after reboot`(){
        var courseAppReboot = CourseApp()
        val token = courseAppReboot.login("matan", "s3kr1t")
        courseAppReboot.logout(token)

        courseAppReboot = CourseApp()//the "REBOOT"

        assertThrows<IllegalArgumentException> {
            runWithTimeout(ofSeconds(10)) { courseAppReboot.isUserLoggedIn(token, "matan") }
        }
    }

    @Test
    fun `password is valid`(){
        var innerCourseApp = CourseApp()

        val token1 = innerCourseApp.login("matan", "StrongPass")
        innerCourseApp.logout(token1)

        assertDoesNotThrow { innerCourseApp.login("matan", "StrongPass") }
    }

    @Test
    fun `password is invalid`(){
        var innerCourseApp = CourseApp()

        val token1 = innerCourseApp.login("matan", "StrongPass")
        innerCourseApp.logout(token1)

        assertThrows<IllegalArgumentException> { innerCourseApp.login("matan", "OtherStrongPass") }
    }

    @Test
    fun `token is invalid after logout`(){
        var innerCourseApp = CourseApp()

        val token1 = innerCourseApp.login("matan2", "StrongPass")
        val token2 = innerCourseApp.login("matan", "StrongPass")
        innerCourseApp.logout(token2)

        assertThrows<IllegalArgumentException> { innerCourseApp.isUserLoggedIn(token2, "matan2") }
    }

//    @Test
//    fun `one million users are logged in at once`(){
//        val million = 10000000
//        var innerCourseApp = CourseApp()
//
////        var tokenToUserMap = HashMap<String,Pair<String,String>>()
//        for (i in 1..million){
//            val username = "user" + i
//            val password = "StrongPass" + i
//            innerCourseApp.login(username, password)
////            val token = innerCourseApp.login(username, password)
////            tokenToUserMap.put(token, Pair(username, password))
//        }
//
//        val randomToken = "user" + Random.nextInt(1, million)
//        for (i in 1..million){
//            assertDoesNotThrow { innerCourseApp.isUserLoggedIn(randomToken, "user" + i) }
//        }
//
//        for (i in 1..million){
//            assertDoesNotThrow { innerCourseApp.logout("user" + i) }
//        }
//    }
}