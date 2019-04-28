package il.ac.technion.cs.softwaredesign.myTests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration


class CourseAppStaffTest {
    private val courseAppInitializer = CourseAppInitializer()

    init {
        courseAppInitializer.setup()
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