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
    init {
        val UserToPasswordMapBA = read(1.toByteArray())
        val UserToTokenMapBA = read(2.toByteArray())
        val FreeTokensBA = read(3.toByteArray())
        val ArrayTokenBA = read(4.toByteArray())
        if (UserToPasswordMapBA != null)
            CourseAppInitializer.UserToPasswordMap = deSerialization(UserToPasswordMapBA ,HashMap<String, String>)
        if (UserToPasswordMapBA != null)
            CourseAppInitializer.UserToTokenMap = deSerialization(UserToTokenMapBA , HashMap<String, Int>)
        if (UserToPasswordMapBA != null)
            CourseAppInitializer.FreeTokens = deSerialization(FreeTokensBA ,LinkedList<Int>)
        if (UserToPasswordMapBA != null)
            CourseAppInitializer.ArrayToken = deSerialization(ArrayTokenBA ,Array<String?>)
    }

    fun deSerialization(ByteArray data, Class c) : Serializable {
        val byteIn = ByteArrayInputStream(data)
        val `in` = ObjectInputStream(byteIn)
        val data2 = `in`.readObject() as c
        return byteOut.toByteArray()
    }

    fun Serialization(Serializable data) : ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = ObjectOutputStream(byteOut)
        out.writeObject(data)
        return byteOut.toByteArray()
    }

    fun UpdateAllDataStructsInServer() : Unit{
        write(1.toByteArray(), Serialization(CourseAppInitializer.UserToPasswordMap))
        write(2.toByteArray(), Serialization(CourseAppInitializer.UserToTokenMap))
        write(3.toByteArray(), Serialization(CourseAppInitializer.FreeTokens))
        write(4.toByteArray(), Serialization(CourseAppInitializer.ArrayToken))
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
    fun login(username: String, password: String): String {     // TODO: naive implementation
        //is user was logged in previously?
        //is the user is logged in right now?

        if (!CourseAppInitializer.UserToPasswordMap.containsKey(username)) {
            //new user
            //enter into mapping of user -> password.
            CourseAppInitializer.UserToPasswordMap[username] = password
        }
        else{
            //user already registered.
            if( CourseAppInitializer.UserToPasswordMap[username] != password) {
                throws IllegalArgumentException()
            }
        }
        if(CourseAppInitializer.UserToTokenMap.containsKey(username)) //already in a session.
            throws IllegalArgumentException()

        //give him a token
        val given_token = CourseAppInitializer.FreeTokens.first()
        CourseAppInitializer.FreeTokens.removeFirst()
        CourseAppInitializer.UserToTokenMap[username] = given_token
        CourseAppInitializer.ArrayToken[given_token] = username

        //update token in server

        UpdateAllDataStructsInServer()

        return given_token
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

        if(CourseAppInitializer.ArrayToken[token] == null)
            throws IllegalArgumentException()

        username = CourseAppInitializer.ArrayToken[token]
        CourseAppInitializer.UserToTokenMap.remove(username)

        CourseAppInitializer.ArrayToken[token] = null
        CourseAppInitializer.FreeTokens.add(token)  //adds to the end of the list

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
        if(CourseAppInitializer.ArrayToken[token] == null)
            throws IllegalArgumentException()

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