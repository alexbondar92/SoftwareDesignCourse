package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.read
import il.ac.technion.cs.softwaredesign.storage.write
import java.io.*
import java.util.*
import java.io.Serializable

/**
 * This is the class implementing CourseApp, a course discussion group system.
 *
 * You may assume that [CourseAppInitializer.setup] was called before this class was instantiated.
 *
 * Currently specified:
 * + User authentication.
 */
class CourseApp {
    init {
        val UserToPasswordMapBA = read(1.toString().toByteArray())
        if (UserToPasswordMapBA != null) {
            val UserToTokenMapBA = read(2.toString().toByteArray())
            val FreeTokensBA = read(3.toString().toByteArray())
            val ArrayTokenBA = read(4.toString().toByteArray())

            CourseAppInitializer.UserToPasswordMap = deSerialization(UserToPasswordMapBA,1) as HashMap<String, String>
            CourseAppInitializer.UserToTokenMap = deSerialization(UserToTokenMapBA, 2) as HashMap<String, Int>
            CourseAppInitializer.FreeTokens = deSerialization(FreeTokensBA, 3) as LinkedList<Int>
            CourseAppInitializer.ArrayToken = deSerialization(ArrayTokenBA, 4) as Array<String?>
        }
    }

    private fun deSerialization(data : ByteArray?, type : Int ) : Serializable {
        val byteIn = ByteArrayInputStream(data)
        val `in` = ObjectInputStream(byteIn)

        when (type) {
            1 ->  return `in`.readObject() as HashMap<String, String>
            2 ->  return `in`.readObject() as HashMap<String, Int>
            3 ->  return `in`.readObject() as LinkedList<Int>
            4 ->  return `in`.readObject() as Array<String?>
            else -> return LinkedList<Int>()
        }


    }

    private fun Serialization(data : Serializable) : ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = ObjectOutputStream(byteOut)
        out.writeObject(data)
        return byteOut.toByteArray()
    }

    private fun UpdateAllDataStructsInServer() : Unit{
        write(1.toString().toByteArray(), Serialization(CourseAppInitializer.UserToPasswordMap))
        write(2.toString().toByteArray(), Serialization(CourseAppInitializer.UserToTokenMap))
        write(3.toString().toByteArray(), Serialization(CourseAppInitializer.FreeTokens))
        write(4.toString().toByteArray(), Serialization(CourseAppInitializer.ArrayToken))
    }

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

        if (!CourseAppInitializer.UserToPasswordMap.containsKey(username)) {
            //new user
            //enter into mapping of user -> password.
            CourseAppInitializer.UserToPasswordMap[username] = password
        }
        else{
            //user already registered.
            if( CourseAppInitializer.UserToPasswordMap[username] != password) {
                throw IllegalArgumentException()
            }
        }
        if(CourseAppInitializer.UserToTokenMap.containsKey(username)) //already in a session.
            throw IllegalArgumentException()

        //give him a token
        val givenToken = CourseAppInitializer.FreeTokens.first()
        CourseAppInitializer.FreeTokens.removeFirst()
        CourseAppInitializer.UserToTokenMap[username] = givenToken
        CourseAppInitializer.ArrayToken[givenToken] = username

        //update token in server

        UpdateAllDataStructsInServer()

        return givenToken.toString()
    }

    /**
     * Log out the user with this authentication [token]. The [token] will be invalidated and can not be used for future
     * calls.
     *
     * This is a *delete* command.
     *
     * @throws IllegalArgumentException If the auth [token] is invalid.
     */
    fun logout(token: String): Unit {

        if(CourseAppInitializer.ArrayToken[token.toInt()] == null)
            throw IllegalArgumentException()

        val username = CourseAppInitializer.ArrayToken[token.toInt()]
        CourseAppInitializer.UserToTokenMap.remove(username)

        CourseAppInitializer.ArrayToken[token.toInt()] = null
        CourseAppInitializer.FreeTokens.add(token.toInt())  //adds to the end of the list

        //update token in server

        UpdateAllDataStructsInServer()
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
        if(CourseAppInitializer.ArrayToken[token.toInt()] == null)
            throw IllegalArgumentException()

        if(CourseAppInitializer.UserToTokenMap[username] == null) {
            if (!CourseAppInitializer.UserToPasswordMap.containsKey(username)) {
                return false
            }
            return true
        }
        else
            return true
    }
}