package il.ac.technion.cs.softwaredesign.tests

import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.function.ThrowingSupplier
import java.time.Duration

// This should be standard.
val isTrue = equalTo(true)
val isFalse = equalTo(false)
val isNull = equalTo(null)
// This is a tiny wrapper over assertTimeoutPreemptively which makes the syntax slightly nicer.
fun <T> runWithTimeout(timeout: Duration, executable: () -> T): T =
        assertTimeoutPreemptively(timeout, ThrowingSupplier(executable))