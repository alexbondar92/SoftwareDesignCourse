package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.read
import il.ac.technion.cs.softwaredesign.storage.write

/**
 * This is the class implementing CourseApp, a course discussion group system.
 *
 * You may assume that [CourseAppInitializer.setup] was called before this class was instantiated.
 *
 * Currently specified:
 * + User authentication.
 */
class CourseApp {

    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null


    /**
     * Log in a user identified by [username] and [password], returning an authentication token that can be used in
     * future calls. If this username did not previously log in to the system, it will be automatically registered with
     * the provided password. Otherwise, the password will be checked against the previously provided password.
     *
     * Note: Allowing enumeration of valid usernames is not a good property for a system to have, from a security
     * standpoint. But this is the way this system will work.
     *
     * This is a *create* command.
     *
     * @throws IllegalArgumentException If the password does not match the username, or the user is already logged in.
     * @return An authentication token to be used in other calls.
     */
    fun login(username: String, password: String): String {

        when (readDataForUsername(username)) {
            notRegistered -> {
                writeDataForUser(username, userLoggedIn)
                writePasswordForUser(username, password)
                return usernameToToken(username)
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw IllegalArgumentException()

                writeDataForUser(username, userLoggedIn)
                return usernameToToken(username)
            }

            userLoggedIn -> throw IllegalArgumentException()
        }

        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }

    /**
     * Log out the user with this authentication [token]. The [token] will be invalidated and can not be used for future
     * calls.
     *
     * This is a *delete* command.
     *
     * @throws IllegalArgumentException If the auth [token] is invalid.
     */
    fun logout(token: String) {
        val username = tokenToUsername(token)
        when (readDataForUsername(username)) {
            notRegistered, registeredNotLoggedIn -> throw IllegalArgumentException()
            userLoggedIn -> writeDataForUser(username, registeredNotLoggedIn)
        }
    }

    /**
     * Indicate the status of [username] in the application.
     *
     * A valid authentication [token] (for *any* user) is required to perform this operation.
     *
     * This is a *read* command.
     *
     * @throws IllegalArgumentException If the auth [token] is invalid.
     * @return True if [username] exists and is logged in, false if it exists and is not logged in, and null if it does
     * not exist.
     */
    fun isUserLoggedIn(token: String, username: String): Boolean? {
        if (!validToken(token)) throw IllegalArgumentException()
        when (readDataForUsername(username)) {
            notRegistered -> return null
            registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }
        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }

    private fun usernameToToken(username: String): String {
        return username
    }

    private fun tokenToUsername(token: String): String {
        return token
    }

    private fun readDataForUsername(username: String): String? {
        val temp = read("U$username".toByteArray())
        if (temp == null) return null else return String(temp)
    }

    private fun writeDataForUser(username: String, data: String) {
        write(("U$username").toByteArray(), data.toByteArray())
    }

    private fun writePasswordForUser(username: String, password: String) {
        write(("P$username$password").toByteArray(), passwordSignedIn.toByteArray())
    }

    private fun passwordValid(username: String, password: String): Boolean {
        return read(("P$username$password").toByteArray()) != null
    }

    private fun validToken(token: String): Boolean {
        when (readDataForUsername(tokenToUsername(token))) {
            notRegistered, registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }
        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }
}

