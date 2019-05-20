package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import java.lang.NullPointerException

class CourseAppImpl : CourseApp , CourseAppStatistics{

    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null

    private var usersTree = RemoteAvlTree(1,DataStoreIo())      // TODO("Remake")
    private var channelByMembersTree = RemoteAvlTree(2,DataStoreIo())      // TODO("Remake")
    private var channelByActiveTree = RemoteAvlTree(3,DataStoreIo())      // TODO("Remake")

    enum class UpdateLoggedStatus{
        IN,
        OUT
    }

    enum class KeyType{
        USER,
        PASSWORD,
        ADMIN,
        CHANNEL,
        PARTICIPANT,
        CHANNELS,
        TOTALUSERSAMOUNT,
        ACTIVEUSERSAMOUNT,
        CHANNELLOGGED,
        USERCHANNELS
    }

    enum class TreeType{
        USERS, CHANNELTOTAL, CHANNELACTIVE
    }


    /**
     * Count the total number of users, both logged-in and logged-out, in the system.
     *
     * @return The total number of users.
     */
    override fun totalUsers(): Long {
        return numOfUsers()
    }

    /**
     * Count the number of logged-in users in the system.
     *
     * @return The number of logged-in users.
     */
    override fun loggedInUsers(): Long{
        return getNumberOfLoggedInUsers()
    }

    /**
     * Return a sorted list of the top 10 channels in the system by user count. The list will be sorted in descending
     * order, so the channel with the highest membership will be first, followed by the second, and so on.
     *
     * If two channels have the same number of users, they will be sorted in ascending lexicographical order.
     *
     * If there are less than 10 channels in the system, a shorter list will be returned.
     *
     * @return A sorted list of channels by user count.
     */
    override fun top10ChannelsByUsers(): List<String> {
        return mutableListOf("TODO") //TODO("call tree function)
    }

    /**
     * Return a sorted list of the top 10 channels in the system by logged-in user count. The list will be sorted in
     * descending order, so the channel with the highest active membership will be first, followed by the second, and so
     * on.
     *
     * If two channels have the same number of logged-in users, they will be sorted in ascending lexicographical order.
     *
     * If there are less than 10 channels in the system, a shorter list will be returned.
     *
     * @return A sorted list of channels by logged-in user count.
     */
    override fun top10ActiveChannelsByUsers(): List<String> {
        return mutableListOf("TODO")//TODO("call tree function)
    }

    /**
     * Return a sorted list of the top 10 users in the system by channel membership count. The list will be sorted in
     * descending order, so the user who is a member of the most channels will be first, followed by the second, and so
     * on.
     *
     * If two users are members of the same number of channels, they will be sorted in ascending lexicographical order.
     *
     * If there are less than 10 users in the system, a shorter list will be returned.
     *
     * @return A sorted list of users by channel count.
     *
     */
    override fun top10UsersByChannels(): List<String> {
        return mutableListOf("TODO")//TODO("call tree function)
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
        when (readUserStatus(username)) {
            notRegistered -> {
                writeUserStatus(username, userLoggedIn)
                writePasswordForUser(username, password)

                writeChannelsAmountOfUser(username, 0)
                insertUsersTree(username, 0)

                incTotalUsers()
                incTotalLoggedInUsers()

                if (numOfUsers() == 1.toLong())    // first user in the system       TODO(maybe change to total users from stats)
                    setAdministrator(username)

                return usernameToToken(username)
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw NoSuchEntityException()

                writeUserStatus(username, userLoggedIn)
                updateAssocChannels(username, UpdateLoggedStatus.IN)
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
        when (readUserStatus(username)) {
            notRegistered, registeredNotLoggedIn -> throw InvalidTokenException()

            userLoggedIn -> {
                writeUserStatus(username, registeredNotLoggedIn)
                updateAssocChannels(username, UpdateLoggedStatus.OUT)
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

        when (readUserStatus(username)) {
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

        when (readUserStatus(username)) {
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
    override fun channelJoin(token: String, channel: String) {  //TODO("check logic again")
        if (!validToken(token))
            throw InvalidTokenException()

        if (!validNameChannel(channel))
            throw NameFormatException()

        if (!isChannelExist(channel) && !isAdministrator(token))
            throw UserNotAuthorizedException()

        val size = amountOfUsersInChannel(channel)
        if ( size != null && size > 0){ //channel empty but was not empty in the past, or not empty now

            val username = tokenToUsername(token)

            makeUserConnectedToChannel(channel, username)

            deleteFromTotalChannelsTree(channel)
            deleteFromActiveChannelsTree(channel)
            deleteFromUsersTree(username)

            val updatedUsersAmount = incUsersInChannel(channel)
            val updatedActiveAmount = incLoggedInChannel(channel)
            val updatedChannelsAmountOfUser = incChannelAmountofUser(username)

            insertToTotalChannelsTree(channel, updatedUsersAmount)
            insertToActiveChannelsTree(channel, updatedActiveAmount)
            insertUsersTree(username, updatedChannelsAmountOfUser)

            insertToChannelsListOfUser(channel, username)

        } else {
            createChannel(channel, token)       // this fun create new channel and adding the admin(token) as first user and makes him/her operator
        }
    }




    private fun createChannel(channel: String, token: String) {//TODO("check logic again")
        // Assumption: token is valid and is associated & the token is associated to admin

        updateAmountOfUsersInChannel(channel, 1)
        updateAmountOfLoggedInChannel(channel, 1)

        assert(isAdministrator(token))

        val username = tokenToUsername(token)

        // make the admin as operator of this channel:
        makeUserOperator(channel, username)

        deleteFromUsersTree(username)
        val updatedChannelsAmountOfUser = incChannelAmountofUser(username)
        insertUsersTree(username, updatedChannelsAmountOfUser)

        insertToTotalChannelsTree(channel, 1)
        insertToActiveChannelsTree(channel, 1)


        insertToChannelsListOfUser(channel, username)
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

        if (!isChannelExist(channel) && !areUserAndChannelConnected(tokenToUsername(token), channel))
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

        if(!isAdministrator(token) && !isOperator(token, channel))
            throw UserNotAuthorizedException()

        if(isAdministrator(token) && !isOperator(token, channel))
            throw UserNotAuthorizedException()

        if(!areUserAndChannelConnected(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        if(readUserStatus(username) == notRegistered || !areUserAndChannelConnected(username, channel))
            throw NoSuchEntityException()

        makeUserOperator(channel, username)
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

        if (readFromStorage(mutableListOf(username),  KeyType.USER) == notRegistered || !areUserAndChannelConnected(username, channel))
            throw NoSuchEntityException()

        removeUserFromChannel(channel, token)   // this fun remove user from channel and update the trees
    }



    private fun removeUserFromChannel(channel: String, token: String){ //TODO("check logic again")
        // Assumption: token and channel is valid
        val username = tokenToUsername(token)

        // update user channel list
        removeFromChannelListOfUser(username, channel)

        // update trees:
        deleteFromTotalChannelsTree(channel)
        deleteFromUsersTree(username)

        val updatedChannelsAmountOfUser = decChannelAmountofUser(username)
        val updatedUsersAmount = decUsersInChannel(channel)

        insertToTotalChannelsTree(channel, updatedUsersAmount)
        insertUsersTree(username, updatedChannelsAmountOfUser)

        if (readFromStorage(mutableListOf(username),  KeyType.USER) ==  userLoggedIn) {

            deleteFromActiveChannelsTree(channel)
            val updatedActiveAmount = decLoggedInChannel(channel)
            insertToActiveChannelsTree(channel, updatedActiveAmount)
        }

        disConnectUserFromChannel(channel, username)
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        return areUserAndChannelConnected(username, channel)
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(tokenToUsername(token), channel))
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(tokenToUsername(token), channel))
            throw UserNotAuthorizedException()

        return numberOfUsersInChannel(channel)
    }

    private fun usernameToToken(username: String): String {
        return username
    }

    private fun tokenToUsername(token: String): String {
        return token
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


    private fun isAdministrator(token: String): Boolean{
        // Assumption: token is valid, and is in the system.
        val username = tokenToUsername(token)
        val str = readFromStorage(mutableListOf(username), KeyType.ADMIN) ?: return false
        return str.toInt() == 1
    }


    private fun createKey(key_in : String, type: TreeType, number : Long): String{
        var key : String
        when (type) {
            TreeType.USERS -> key ="AvlUsers%$key_in%$number"
            TreeType.CHANNELTOTAL -> key = "AvlChannel1%$key_in%$number"
            TreeType.CHANNELACTIVE -> key ="AvlChannel2%$key_in%$number"
        }
        return key
    }

    private fun updateAssocChannels(username: String, kind: UpdateLoggedStatus){
        // Assumption:: username is valid
        val channels = getChannelsOf(username)
        for (channel in channels){ // TODO ("refactor this to updateChannel fun")
            var numOfLoggedInUsers = 0
            val str: String  = readFromStorage(mutableListOf(channel), KeyType.CHANNELLOGGED)!!
            numOfLoggedInUsers = str.toInt()

            if (numOfLoggedInUsers == 0)                         // Sanity check
                assert(kind == UpdateLoggedStatus.IN)

            channelByActiveTree.delete(("AvlChannel2$channel%$numOfLoggedInUsers"))
            when(kind){
                UpdateLoggedStatus.IN -> numOfLoggedInUsers++
                UpdateLoggedStatus.OUT -> numOfLoggedInUsers--
            }
            channelByActiveTree.insert(("AvlChannel2$channel%$numOfLoggedInUsers"))

            writeToStorage(mutableListOf(channel), data = numOfLoggedInUsers.toString(), type = KeyType.CHANNELLOGGED)
        }
    }



    private fun validNameChannel(channel: String): Boolean{
        val regex = Regex("((#)([a-z]|[A-Z]|(#)|(_))*)")      // Regex pattern of valid channel
        return regex.matchEntire(channel)?.value != null
    }

    private fun isChannelExist(channel: String): Boolean{
        val str = readFromStorage(mutableListOf(channel), KeyType.CHANNEL) ?: return false
        return str.toInt() > 0
    }


    private fun areUserAndChannelConnected(username: String, channel: String): Boolean{
        // Assumption: username and channel is valid

        val str = readFromStorage(mutableListOf(channel, username), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 1 || str.toInt() == 2
    }


    private fun isOperator(token: String, channel: String): Boolean {       // TODO("change token to username")
        // Assumption: token and channel is valid
        val username = tokenToUsername(token)
        val str = readFromStorage(mutableListOf(channel, username), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 2     // TODO("2 is sign for operator")
    }



    //writers:////////////////////////////////////////////////////

    private fun writeToStorage(args : MutableList<String>, data: String, type :KeyType )  {

        when (type) {
            KeyType.USER -> {
                val username = args[0]
                DataStoreIo.write("UV$username", data)
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                DataStoreIo.write(("UP$username$password"), data)

            }
            KeyType.ADMIN ->{
                val username = args[0]
                DataStoreIo.write(("UA$username"),data)
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                DataStoreIo.write(("CV$channel"), data)
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val username = args[1]
                DataStoreIo.write(("CU$channel%$username"), data) //delimiter "%" between channel and username
            }
            KeyType.CHANNELS -> {
                val username = args[0]
                DataStoreIo.write(("UCL$username"), data)
            }
            KeyType.TOTALUSERSAMOUNT -> {
                DataStoreIo.write(("totalUsers"), data)
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                DataStoreIo.write(("activeUsers"), data)
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                DataStoreIo.write(("CL$channel"), data)
            }
            KeyType.USERCHANNELS -> {
                val username = args[0]
                DataStoreIo.write(("UL$username"), data)
            }
        }

    }

    private fun writeUserStatus(username: String, data: String) {
        writeToStorage(mutableListOf(username), data, KeyType.USER)
    }

    private fun writePasswordForUser(username: String, password: String) {
        writeToStorage(mutableListOf(username, password), data = passwordSignedIn, type = KeyType.USER)
    }

    private fun writeChannelsAmountOfUser(username: String, number : Long) {
        writeToStorage(mutableListOf(username), number.toString(), KeyType.USERCHANNELS)
    }

    private fun incChannelAmountofUser(username: String) : Long {
        val number = readAmountOfChannelsForUser(username)
        writeChannelsAmountOfUser(username, number+1)
        return number+1
    }

    private fun decChannelAmountofUser(username: String) : Long {
        val number = readAmountOfChannelsForUser(username)
        assert(number > 0)
        writeChannelsAmountOfUser(username, number-1)
        return number-1
    }

    private fun setAdministrator(username: String){
        // Assumption: username is valid, and is in the system.
        writeToStorage(mutableListOf(username), data = "1" , type = KeyType.ADMIN)
    }

    private fun insertToChannelsListOfUser(channel : String, username : String) {
        val str = readFromStorage(mutableListOf(username), KeyType.CHANNELS)
        writeToStorage(mutableListOf(username), data = "$str%$channel", type = KeyType.CHANNELS)
    }

    private fun updateAmountOfLoggedInChannel(channel : String, number: Long){
        writeToStorage(mutableListOf(channel), data = number.toString(), type = KeyType.CHANNELLOGGED)
    }

    private fun incLoggedInChannel(channel: String) : Long{
        val size = numberOfActiveUsersInChannel(channel)
        updateAmountOfLoggedInChannel(channel, size+1)
        return size + 1
    }

    private fun decLoggedInChannel(channel: String) : Long {
        val size = numberOfActiveUsersInChannel(channel)
        assert(size > 0)
        updateAmountOfLoggedInChannel(channel, size-1)
        return size - 1
    }

    private fun updateAmountOfUsersInChannel(channel : String, number: Long) {
        writeToStorage(mutableListOf(channel), data = number.toString(), type = KeyType.CHANNEL)
    }

    private fun incUsersInChannel(channel: String): Long {
        val size = amountOfUsersInChannel(channel)!!
        updateAmountOfUsersInChannel(channel, size+1)
        return size + 1
    }
    private fun decUsersInChannel(channel: String) : Long {
        val size = amountOfUsersInChannel(channel)!!
        assert(size > 0)
        updateAmountOfUsersInChannel(channel, size-1)
        return size - 1
    }

    private fun makeUserOperator(channel : String, username: String){
        writeToStorage(mutableListOf(channel, username), data = "2", type = KeyType.PARTICIPANT)
    }

    private fun makeUserConnectedToChannel(channel : String, username: String){
        writeToStorage(mutableListOf(channel, username), data = "1", type = KeyType.PARTICIPANT)
    }

    private fun disConnectUserFromChannel(channel : String, username: String){
        writeToStorage(mutableListOf(channel, username), data = "0", type = KeyType.PARTICIPANT)
    }

    private fun incTotalUsers(){
        var totalUsers : Long = 0
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT)
        if (str != null)
            totalUsers = str.toLong()

        totalUsers++
        writeToStorage(mutableListOf(), data = totalUsers.toString(), type = KeyType.TOTALUSERSAMOUNT)
    }

    private fun getNumberOfLoggedInUsers(): Long{
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT)!!
        return str.toLong()
    }

    private fun incTotalLoggedInUsers(){
        var totalLoggedUsers = 0
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT)
        if (str != null)
            totalLoggedUsers = str.toInt()

        totalLoggedUsers++
        writeToStorage(mutableListOf(), data = totalLoggedUsers.toString(), type = KeyType.ACTIVEUSERSAMOUNT)
    }

    private fun DecTotalLoggedInUsers(){
        var totalLoggedUsers = 0
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT) !!
        totalLoggedUsers = str.toInt()

        totalLoggedUsers--
        writeToStorage(mutableListOf(), data = totalLoggedUsers.toString(), type = KeyType.ACTIVEUSERSAMOUNT)
    }

    private fun removeFromChannelListOfUser(username: String, channel: String) : Int{
        val list = getChannelsOf(username)
        list.remove(channel)
        writeToStorage(mutableListOf(username), data = list.joinToString("%"), type = KeyType.CHANNELS)
        return list.size
    }

    //end writers////////////////////////////////////////////////////

    //reader:////////////////////////////////////////////////////

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
            KeyType.USERCHANNELS -> {
                val username = args[0]
                str = DataStoreIo.read(("UL$username"))
            }
        }
        return str
    }

    private fun readUserStatus(username : String) :String?{
        return readFromStorage(mutableListOf(username),  KeyType.USER)
    }

    private fun amountOfUsersInChannel(channel: String) : Long? {
        return readFromStorage(mutableListOf(channel), KeyType.CHANNEL)?.toLong()
    }

    private fun numberOfUsersInChannel(channel: String): Long{
        // Assumption: channel is valid
        return readFromStorage(mutableListOf(channel), KeyType.CHANNEL)!!.toLong()
    }


    private fun readAmountOfChannelsForUser(username: String) : Long{
        return readFromStorage(mutableListOf(username), KeyType.USERCHANNELS)!!.toLong()
    }

    private fun getChannelsOf(username: String): MutableList<String>{
        // Assumption:: username is valid
        val str = readFromStorage(mutableListOf(username), KeyType.CHANNELS)!!
        val delimiter = "%"
        return str.split(delimiter).toMutableList()
    }

    private fun numberOfActiveUsersInChannel(channel: String): Long {
        // Assumption: channel is valid
        return readFromStorage(mutableListOf(channel), KeyType.CHANNELLOGGED)!!.toLong()
    }

    private fun numOfUsers(): Long{
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT) ?: throw NullPointerException()
        return str.toLong()
    }

    //end readers////////////////////////////////////////////////////

    //tree wrappers:*********************************

    //inserts:
    private fun insertUsersTree(username : String, number : Long) {
        usersTree.insert(createKey(username,  TreeType.USERS, number))
    }

    private fun insertToTotalChannelsTree(channel : String, number : Long) {
        channelByMembersTree.insert(createKey(channel,  TreeType.CHANNELTOTAL, number))
    }

    private fun insertToActiveChannelsTree(channel : String, number : Long) {
        channelByActiveTree.insert(createKey(channel,  TreeType.CHANNELACTIVE, number))
    }


    //deletes:
    private fun  deleteFromTotalChannelsTree(channel: String){
        val number = numberOfUsersInChannel(channel)
        channelByMembersTree.delete(createKey(channel,  TreeType.CHANNELTOTAL, number))
    }

    private fun  deleteFromActiveChannelsTree(channel: String){
        val number = numberOfActiveUsersInChannel(channel)
        channelByActiveTree.delete(createKey(channel,  TreeType.CHANNELACTIVE, number))
    }

    private fun  deleteFromUsersTree(username: String) {
        val number = readAmountOfChannelsForUser(username)
        usersTree.delete(createKey(username,TreeType.USERS, number ))
    }

    //end tree wrappers*********************************


}