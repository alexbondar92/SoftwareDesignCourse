package il.ac.technion.cs.softwaredesign.myTests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import jdk.nashorn.internal.objects.NativeArray.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

import il.ac.technion.cs.softwaredesign.storage.read
import il.ac.technion.cs.softwaredesign.storage.write
import io.mockk.*

class CourseAppStaffTest {
    private val courseAppInitializer = CourseAppInitializer()

//    init {
//        courseAppInitializer.setup()
//    }

    init {
        val storageMock= mutableMapOf<ByteArray,ByteArray>()
        courseAppInitializer.setup()
        mockkStatic("il.ac.techinion.cs.softwaredesing.storage.secureStorageKt")
        val keySlot = slot<ByteArray>()
        val valueSlot = slot<ByteArray>()
        every {
            write(capture(keySlot), capture(valueSlot))
        } answers {
            val value = valueSlot.captured
            val key = keySlot.captured
            // println("now we are writing the value:$value with the key $key")
            storageMock[key] = value
        }
        every {
            read(capture(keySlot))
        } answers {
            val key = keySlot.captured
            val value = storageMock[key]
            Thread.sleep(value?.size?.toLong() ?: 0)
            storageMock[key]
        }
    }

    private val courseApp = CourseApp()

    @Test
    fun `after login, a user is logged in`() {
        courseApp.login("gal", "hunter2")
        courseApp.login("imaman", "31337")

        val token = courseApp.login("matan", "s3kr1t")

        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal") },
                present(isTrue))
    }

    @Test
    fun `an authentication token is invalidated after logout`() {
        val token = courseApp.login("matan", "s3kr1t")

        courseApp.logout(token)

        assertThrows<IllegalArgumentException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "matan") }
        }
    }
}