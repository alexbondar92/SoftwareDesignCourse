package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import java.lang.NullPointerException

class CourseAppImpl : CourseApp{
    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null

    private var usersTree = MyAvlTree(1,DataStoreIo())      // TODO("Remake")
    private var channelByMembersTree = MyAvlTree(2,DataStoreIo())      // TODO("Remake")
    private var channelByActiveTree = MyAvlTree(3,DataStoreIo())      // TODO("Remake")

    enum class updateLoggedStatus{
        IN,
        OUT
    }

    enum class KeyType{
        USER, PASSWORD, ADMIN, CHANNEL, PARTICIPANT, CHANNELS, TOTALUSERSAMOUNT, ACTIVEUSERSAMOUNT, CHANNELLOGGED
    }


    /**
     * Log in a user identified by [username] and [password], returning an authentication token that can be used in
     * future calls. If this username did not previously log in to the system, it will be automatically registered with
     * the provided password. Otherwise, the password will be checked against the previously provided password.
     *
     * Note: Allowing enumeration of valid usernames is not a good property for a system to have, from a security
     * standpoint. But this is the way this system will work.
     *
     * If this is the first user to be registered, it will be made an administrator.
     *
     * This is a *create* command.
     *
     * @throws NoSuchEntityException If the password does not match the username.
     * @throws UserAlreadyLoggedInException If the user is already logged-in.
     * @return An authentication token to be used in other calls.
     */
    override fun login(username: String, password: String): String {
        when (readFromStorage(mutableListOf(username),  KeyType.USER)) {
            notRegistered -> {
                writeDataForUser(username, userLoggedIn)        // TODO ("refactor")
                writePasswordForUser(username, password)        // TODO ("refactor")
                usersTree.insert(createKey(username), createData(username))     // TODO ("move this out of the logic")

                incTotalUsers()
                incTotalLoggedInUsers()

                if (numOfUsers() == 1)    // first user in the system       TODO(maybe change to total users from stats)
                    setAdministrator(username)

                return usernameToToken(username)
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw NoSuchEntityException()

                writeDataForUser(username, userLoggedIn)        // TODO ("refactor")
                updateAssocChannels(username, updateLoggedStatus.IN)
                incTotalLoggedInUsers()
                return usernameToToken(username)
            }

            userLoggedIn -> throw UserAlreadyLoggedInException()
        }

        assert(false)       //shouldn't get here, data-store might be corrupted.
        throw IllegalArgumentException()
    }

    /**
     * Log out the user with this authentication [token]. The [token] will be invalidated and can not be used for future
     * calls.
     *
     * This is a *delete* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     */
    override fun logout(token: String) {
        val username = tokenToUsername(token)
        when (readFromStorage(mutableListOf(username),  KeyType.USER)) {
            notRegistered, registeredNotLoggedIn -> throw InvalidTokenException()

            userLoggedIn -> {
                writeDataForUser(username, registeredNotLoggedIn)        // TODO ("refactor")
                updateAssocChannels(username, updateLoggedStatus.OUT)
                DecTotalLoggedInUsers()
            }
        }
    }

    /**
     * Indicate the status of [username] in the application.
     *
     * A valid authentication [token] (for *any* user) is required to perform this operation.
     *
     * This is a *read* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @return True if [username] exists and is logged in, false if it exists and is not logged in, and null if it does
     * not exist.
     */
    override fun isUserLoggedIn(token: String, username: String): Boolean? {
        if (!validToken(token))
            throw InvalidTokenException()

        when (readFromStorage(mutableListOf(username),  KeyType.USER)) {                      // TODO ("refactor")
            notRegistered -> return null
            registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }

        assert(false)       //shouldn't get here, data-store might be corrupted.
        throw IllegalArgumentException()
    }

    /**
     * Make another user, identified by [username], an administrator. Only users who are administrators may perform this
     * operation.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws UserNotAuthorizedException If the auth [token] does not belong to a user who is an administrator.
     * @throws NoSuchEntityException If [username] does not exist.
     */
    override fun makeAdministrator(token: String, username: String) {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isAdministrator(token))
            throw UserNotAuthorizedException()

        when (readFromStorage(mutableListOf(username),  KeyType.USER)) {      // TODO(refactor)
            notRegistered -> throw NoSuchEntityException()

            registeredNotLoggedIn, userLoggedIn -> {
                setAdministrator(username)
            }
        }
    }

    /**
     * The user identified by [token] will join [channel]. If the channel does not exist, it is created only if [token]
     * identifies a user who is an administrator.
     *
     * Valid names for channels start with `#`, then have any number of English alphanumeric characters, underscores
     * (`_`) and hashes (`#`).
     *
     * This is a *create* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NameFormatException If [channel] is not a valid name for a channel.
     * @throws UserNotAuthorizedException If [channel] does not exist and [token] belongs to a user who is not an
     * administrator.
     */
    override fun channelJoin(token: String, channel: String) {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!validNameChannel(channel))
            throw NameFormatException()

        if (isChannelExist(channel) && !isAdministrator(token))
            throw UserNotAuthorizedException()

        if (readFromStorage(mutableListOf(channel), KeyType.CHANNEL)!!.toInt() > 0){
            TODO("the channel already exist" +
                    "add the user as member to this channel" +
                    "update the channel trees and the user tree")
        } else {
            createChannel(channel, token)       // this fun create new channel and adding the admin(token) as first user and makes him/her operator
        }
    }

    /**
     * The user identified by [token] will exit [channel].
     *
     * If the last user leaves a channel, the channel will be destroyed and its name will be available for re-use.
     *
     * This is a *delete* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [token] identifies a user who is not a member of [channel], or [channel] does
     * does exist.
     */
    override fun channelPart(token: String, channel: String) {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel) && !isUserInChannelAux(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        removeUserFromChannel(channel, token)   // this fun remove user from channel and update the trees + if the chanel is empty the channel will be deleted
    }

    /**
     * Make [username] an operator of this channel. Only existing operators of [channel] and administrators are allowed
     * to make other users operators.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If the user identified by [token] is at least one of the following:
     * 1. Not an operator of [channel] or an administrator,
     * 2. An administrator who is not an operator of [channel] and [username] does not match [token],
     * 3. Not a member of [channel].
     * @throws NoSuchEntityException If [username] does not exist, or if [username] is not a member of [channel].
     */
    override fun channelMakeOperator(token: String, channel: String, username: String) {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("1. check that the token is valid" +
                    "2. check that the channel exits" +
                    "3. check that the tokes is following all 3 things" +
                    "4. check that the user exist and is member of that channel" +
                    "5. make user as operator under this channel at user's channel list")
    }

    /**
     * Remove the user [username] from [channel]. Only operators of [channel] may perform this operation.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] is not an operator of this channel.
     * @throws NoSuchEntityException If [username] does not exist, or if [username] is not a member of [channel].
     */
    override fun channelKick(token: String, channel: String, username: String) {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isOperator(token, channel))
            throw UserNotAuthorizedException()

        if (readFromStorage(mutableListOf(username),  KeyType.USER) == notRegistered || !isUserInChannelAux(username, channel))
            throw NoSuchEntityException()

        removeUserFromChannel(channel, token)   // this fun remove user from channel and update the trees
    }

    /**
     * Indicate [username]'s membership in [channel]. A user is still a member of a channel when logged off.
     *
     * This is a *read* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] is not an administrator or member of this channel.
     * @return True if [username] exists and is a member of [channel], false if it exists and is not a member, and null
     * if it does not exist.
     */
    override fun isUserInChannel(token: String, channel: String, username: String): Boolean? {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !isUserInChannelAux(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        return isUserInChannelAux(username, channel)
    }

    /**
     * Gets the number of logged-in users in a given [channel].
     *
     * Administrators can query any channel, while regular users can only query channels that they are members of.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] identifies a user who is not an administrator or is not a member
     * of [channel].
     * @returns Number of logged-in users in [channel].
     */
    override fun numberOfActiveUsersInChannel(token: String, channel: String): Long {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !isUserInChannelAux(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        return numberOfActiveUsersInChannel(channel)
    }

    /**
     * Gets the number of users in a given [channel].
     *
     * Administrators can query any channel, while regular users can only query channels that they are members of.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] identifies a user who is not an administrator or is not a member
     * of [channel].
     * @return Number of users, both logged-in and logged-out, in [channel].
     */
    override fun numberOfTotalUsersInChannel(token: String, channel: String): Long {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !isUserInChannelAux(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        return numberOfUsersInChannel(channel)
    }

    private fun usernameToToken(username: String): String {
        return username
    }

    private fun tokenToUsername(token: String): String {
        return token
    }

    private fun readFromStorage(args : MutableList<String>, type :KeyType ) : String? {
        var str : String?
        when (type) {
            KeyType.USER -> {
                val username = args[0]
                str = DataStoreIo.read("UV$username")
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                str = DataStoreIo.read(("UP$username$password"))

            }
            KeyType.ADMIN ->{
                val username = args[0]
                str = DataStoreIo.read(("UA$username"))
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                str = DataStoreIo.read(("CV$channel"))
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val username = args[1]
                str = DataStoreIo.read(("CU$channel%$username")) //delimiter "%" between channel and username
            }
            KeyType.CHANNELS -> {
                val username = args[0]
                str = DataStoreIo.read(("UCL$username"))
            }
            KeyType.TOTALUSERSAMOUNT -> {
                str = DataStoreIo.read(("totalUsers"))
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                str = DataStoreIo.read(("activeUsers"))
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                str = DataStoreIo.read(("CL$channel"))
            }
        }
        return str
    }

    private fun writeToStorage(args : MutableList<String>, type :KeyType ) : String? {
        var str : String?
        when (type) {
            KeyType.USER -> {
                val username = args[0]
                str = DataStoreIo.read("UV$username")
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                str = DataStoreIo.read(("UP$username$password"))

            }
            KeyType.ADMIN ->{
                val username = args[0]
                str = DataStoreIo.read(("UA$username"))
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                str = DataStoreIo.read(("CV$channel"))
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val username = args[1]
                str = DataStoreIo.read(("CU$channel%$username")) //delimiter "%" between channel and username
            }
            KeyType.CHANNELS -> {
                val username = args[0]
                str = DataStoreIo.read(("UCL$username"))
            }
            KeyType.TOTALUSERSAMOUNT -> {
                str = DataStoreIo.read(("totalUsers"))
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                str = DataStoreIo.read(("activeUsers"))
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                str = DataStoreIo.read(("CL$channel"))
            }
        }
        return str
    }

    private fun writeDataForUser(username: String, data: String) {
        DataStoreIo.write(("U$username"), data)
    }

    private fun writePasswordForUser(username: String, password: String) {
        DataStoreIo.write(("P$username$password"), passwordSignedIn)
    }

    private fun passwordValid(username: String, password: String): Boolean {
        return readFromStorage(mutableListOf(username, password), KeyType.PASSWORD) != null
    }

    private fun validToken(token: String): Boolean {
        when (readFromStorage(mutableListOf(tokenToUsername(token)), KeyType.USER)) {
            notRegistered, registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }
        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }

    private fun incTotalUsers(){
        var totalUsers = 0
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT)
        if (str != null)
            totalUsers = str.toInt()

        totalUsers++
        DataStoreIo.write(("totalUsers"), totalUsers.toString())
    }

    private fun incTotalLoggedInUsers(){
        var totalLoggedUsers = 0
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT)
        if (str != null)
            totalLoggedUsers = str.toInt()

        totalLoggedUsers++
        DataStoreIo.write(("totalLoggedInUsers"), totalLoggedUsers.toString())
    }

    private fun DecTotalLoggedInUsers(){
        var totalLoggedUsers = 0
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT) !!
        totalLoggedUsers = str.toInt()

        totalLoggedUsers--
        DataStoreIo.write(("totalLoggedInUsers"), totalLoggedUsers.toString())
    }

    private fun numOfUsers(): Int{
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT) ?: throw NullPointerException()
        return str.toInt()
    }

    private fun setAdministrator(username: String){
        // Assumption: username is valid, and is in the system.
        DataStoreIo.write(("UA$username"),1.toString())
    }

    private fun isAdministrator(token: String): Boolean{
        // Assumption: token is valid, and is in the system.
        val username = tokenToUsername(token)
        val str = readFromStorage(mutableListOf(username), KeyType.ADMIN) ?: return false
        return str.toInt() == 1
    }

    private fun createKey(username: String): String{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Create key for the username, this fun is for the Storage implementation" +
                "Rethink about this....")

        TODO("Write the assumption above, as documentation")
    }

    private fun createData(username: String): String{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Create data for the username, this fun is for the Storage implementation" +
                "Rethink about this....")

        TODO("Write the assumption above, as documentation")
    }

    private fun updateAssocChannels(username: String, kind: updateLoggedStatus){
        // Assumption:: username is valid
        val channels = getChannelsOf(username)
        for (channel in channels){ // TODO ("refactor this to updateChannel fun")
            var numOfLoggedInUsers = 0
            val str: String  = readFromStorage(mutableListOf(channel), KeyType.CHANNELLOGGED)!!
                numOfLoggedInUsers = str.toInt()

            if (numOfLoggedInUsers == 0)                         // Sanity check
                assert(kind == updateLoggedStatus.IN)

            channelByActiveTree.delete(("AvlChannel2$channel%$numOfLoggedInUsers"))
            when(kind){
                updateLoggedStatus.IN -> numOfLoggedInUsers++
                updateLoggedStatus.OUT -> numOfLoggedInUsers--
            }
            channelByActiveTree.insert(("AvlChannel2$channel%$numOfLoggedInUsers"))

            DataStoreIo.write(("CL$channel"), numOfLoggedInUsers.toString())
        }
    }

    private fun getChannelsOf(username: String): MutableList<String>{
        // Assumption:: username is valid
        val str = readFromStorage(mutableListOf(username), KeyType.CHANNELS)!!
        val delimiter = "%"
        return str.split(delimiter).toMutableList()

    }

    private fun validNameChannel(channel: String): Boolean{
        val regex = Regex("((#)([a-z]|[A-Z]|(#)|(_))*)")      // Regex pattern of valid channel
        return regex.matchEntire(channel)?.value != null
    }

    private fun isChannelExist(channel: String): Boolean{
        val str = readFromStorage(mutableListOf(channel), KeyType.CHANNEL) ?: return false
        return str.toInt() > 0
    }

    private fun createChannel(channel: String, token: String) { //TODO("Refactor: change token to username....")
        // Assumption: token is valid and is associated & the token is associated to admin
        DataStoreIo.write(("CV$channel"),1.toString())

        assert(isAdministrator(token))
        val username = tokenToUsername(token)
        DataStoreIo.write(("CU$channel$username"), 2.toString())        // make the admin as operator of this channel

        // update number of logged in members in the channel
        DataStoreIo.write(("CL$channel"), 1.toString())

        val numOfUsers = 1
        channelByMembersTree.insert(("AvlChannel1$channel%$numOfUsers"))
        channelByActiveTree.insert(("AvlChannel2$channel%$numOfUsers"))

        val str = readFromStorage(mutableListOf(username), KeyType.CHANNELS)
        DataStoreIo.write(("UCL$username"), "$str%$channel")
    }

    private fun isUserInChannelAux(username: String, channel: String): Boolean{     // TODO("Refactor: change to better name...")
        // Assumption: username and channel is valid

        val str = readFromStorage(mutableListOf(channel, username), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 1 || str.toInt() == 2
    }

    private fun removeUserFromChannel(channel: String, token: String){
        // Assumption: token and channel is valid
        val username = tokenToUsername(token)

        // update user channel list
        val list = getChannelsOf(username)
        var numOfAssocChannels = list.size
        list.remove(channel)
        DataStoreIo.write(("UCL$username"), list.joinToString("%"))

        // update users tree
        usersTree.delete(("AvlUsers$username%$numOfAssocChannels"))
        numOfAssocChannels--
        usersTree.insert(("AvlUsers$username%$numOfAssocChannels"))

        //update two trees:
        var numOfUsersInChannel = readFromStorage(mutableListOf(channel), KeyType.CHANNEL)!!.toInt()
        var numOfLoggedUsersInChannel = readFromStorage(mutableListOf(channel), KeyType.CHANNELLOGGED)!!.toInt()
        channelByMembersTree.delete(("AvlChannel1$channel%$numOfUsersInChannel"))
        numOfUsersInChannel--
        channelByMembersTree.insert(("AvlChannel1$channel%$numOfUsersInChannel"))
        DataStoreIo.write(("CV$channel"), numOfUsersInChannel.toString())

        if (readFromStorage(mutableListOf(username),  KeyType.USER) ==  userLoggedIn) {
            channelByActiveTree.delete(("AvlChannel2$channel%$numOfLoggedUsersInChannel"))
            numOfLoggedUsersInChannel--
            channelByActiveTree.insert("AvlChannel2$channel%$numOfLoggedUsersInChannel")

            DataStoreIo.write(("CL$channel"), numOfLoggedUsersInChannel.toString())
        }

        //update
        DataStoreIo.write(("CU$channel%$username"), 0.toString()) //TODO("delimer %, refactoring")

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Removing the user from the channel, updating the user's channel list" +
                "where to check about the last user??...." +
                "update the channel trees")

        TODO("Write the assumption above, as documentation")
    }

    private fun isOperator(token: String, channel: String): Boolean {       // TODO("change token to username")
        // Assumption: token and channel is valid
        val username = tokenToUsername(token)
        val str = readFromStorage(mutableListOf(channel, username), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 2     // TODO("2 is sign for operator")
    }

    private fun numberOfActiveUsersInChannel(channel: String): Long {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Returning number of active users in the channel")

        TODO("Write the assumption above, as documentation")
    }

    private fun numberOfUsersInChannel(channel: String): Long{
        // Assumption: channel is valid
        return readFromStorage(mutableListOf(channel), KeyType.CHANNEL)!!.toLong()
    }
}