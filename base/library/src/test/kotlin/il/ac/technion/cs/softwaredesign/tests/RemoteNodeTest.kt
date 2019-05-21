package il.ac.technion.cs.softwaredesign.tests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import java.time.Duration
import org.junit.jupiter.api.Test

class RemoteNodeTest {

    @Test
    fun `after login, a user is logged in`() {
        val token = courseApp.login("matan", "s3kr1t")

        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal") },
                present(isTrue))
    }
}