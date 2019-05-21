package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import java.lang.NullPointerException
import java.util.concurrent.BlockingDeque

class CourseAppImpl : CourseApp{

    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null

    private val storage = DataStoreIo()
    private var usersTree = RemoteAvlTree("AvlUsers",storage)
    private var channelByMembersTree = RemoteAvlTree("AvlChannel1",storage)
    private var channelByActiveTree = RemoteAvlTree("AvlChannel2",storage)

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
        USERCHANNELS,
        INDEXUSERSYS,
        INDEXCHANNELSYS,
        USERTOINDEX,
        INDEXTOUSER,
        CHANNELTOINDEX,
        INDEXTOCHANNEL
    }

    init {
        // resetIndexSystem()
        TODO ("Move to CourseAppInitializer")
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
        when (readUserStatus(usernameToToken(username))) {
            notRegistered -> {
                val genToken = getNewUserIndex(username)

                writeUserStatus(genToken, userLoggedIn)
                writePasswordForUser(username, password)

                writeChannelsAmountOfUser(genToken, 0)
                insertUsersTree(genToken, 0)

                incTotalUsers()
                incTotalLoggedInUsers()

                if (numOfUsers() == 1.toLong())    // first user in the system       TODO(maybe change to total users from stats)
                    setAdministrator(genToken)

                return genToken
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw NoSuchEntityException()

                val token = usernameToToken(username)
                writeUserStatus(token, userLoggedIn)
                updateAssocChannels(token, UpdateLoggedStatus.IN)
                incTotalLoggedInUsers()
                return token
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
        when (readUserStatus(token)) {
            notRegistered, registeredNotLoggedIn -> throw InvalidTokenException()

            userLoggedIn -> {
                writeUserStatus(token, registeredNotLoggedIn)
                updateAssocChannels(token, UpdateLoggedStatus.OUT)
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

        when (readUserStatus(usernameToToken(username))) {
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

        val token = usernameToToken(username)
        when (readUserStatus(token)) {
            notRegistered -> throw NoSuchEntityException()

            registeredNotLoggedIn, userLoggedIn -> {
                setAdministrator(token)
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
    override fun channelJoin(token: String, channel: String) {              //TODO("check logic again")
        if (!validToken(token))
            throw InvalidTokenException()

        if (!validNameChannel(channel))
            throw NameFormatException()

        if (!isChannelExist(channel) && !isAdministrator(token))
            throw UserNotAuthorizedException()

        val newIndex = getNewChannelIndex(channel)

        val size = amountOfUsersInChannel(channel)
        if ( size != null && size > 0){ //channel empty but was not empty in the past, or not empty now

            makeUserConnectedToChannel(channel, token)

            deleteFromTotalChannelsTree(channel)
            deleteFromActiveChannelsTree(channel)
            deleteFromUsersTree(token)

            val updatedUsersAmount = incUsersInChannel(channel)
            val updatedActiveAmount = incLoggedInChannel(channel)
            val updatedChannelsAmountOfUser = incChannelAmountofUser(token)

            insertToTotalChannelsTree(channel, updatedUsersAmount)
            insertToActiveChannelsTree(channel, updatedActiveAmount)
            insertUsersTree(token, updatedChannelsAmountOfUser)

            insertToChannelsListOfUser(channel, token)

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

        if (!isChannelExist(channel) && !areUserAndChannelConnected(token, channel))
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

        if(!areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        val userToken = usernameToToken(username)
        if(readUserStatus(userToken) == notRegistered || !areUserAndChannelConnected(userToken, channel))
            throw NoSuchEntityException()

        makeUserOperator(channel, userToken)
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

        val userToken = usernameToToken(username)
        if (readFromStorage(mutableListOf(userToken),  KeyType.USER) == notRegistered || !areUserAndChannelConnected(userToken, channel))
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        return areUserAndChannelConnected(usernameToToken(username), channel)
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
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

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        return numberOfUsersInChannel(channel)
    }

    // =========================================== API for statistics ==================================================

    fun getTotalUsers(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT)
        return if(str == null) 0 else str.toLong()
    }

    fun getTotalActiveUsers(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT)
        return if(str == null) 0 else str.toLong()
    }

    fun getTop10User(): List<String>{
        return this.usersTree.top10().asSequence().map { tokenToUsername(it.toString()) }.toList()
    }

    fun getTop10ChannelsByTotalUsers(): List<String>{
        return this.channelByMembersTree.top10().asSequence().map { indexToChannel(it.toString()) }.toList()
    }

    fun getTop10ChannelsByActiveUsers(): List<String>{
        return this.channelByActiveTree.top10().asSequence().map { indexToChannel(it.toString()) }.toList()
    }

    //writers:////////////////////////////////////////////////////

    private fun writeToStorage(args : MutableList<String>, data: String, type :KeyType )  {
        when (type) {
            KeyType.USER -> {
                val token = args[0]
                storage.write("UV$token", data)
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                storage.write(("UP$username$password"), data)
            }
            KeyType.ADMIN ->{
                val userIndex = args[0]
                storage.write(("UA$userIndex"),data)
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                storage.write(("CV$index"), data)
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                val token = args[1]
                storage.write(("CU$index%$token"), data) //delimiter "%" between channel and token
            }
            KeyType.CHANNELS -> {
                val token = args[0]
                storage.write(("UCL$token"), data)
            }
            KeyType.TOTALUSERSAMOUNT -> {
                storage.write(("totalUsers"), data)
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                storage.write(("activeUsers"), data)
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                storage.write(("CL$index"), data)
            }
            KeyType.USERCHANNELS -> {
                val token = args[0]
                storage.write(("UL$token"), data)
            }
            KeyType.INDEXCHANNELSYS -> {
                storage.write(("IndexChannelSys"), data)
            }
            KeyType.INDEXUSERSYS -> {
                storage.write(("IndexUserSys"), data)
            }
            KeyType.CHANNELTOINDEX -> {
                val channel = args[0]
                storage.write(("CI$channel"), data)
            }
            KeyType.INDEXTOCHANNEL -> {
                val index = args[0]
                storage.write(("IC$index"), data)
            }
            KeyType.USERTOINDEX -> {
                val username = args[0]
                storage.write(("UI$username"), data)
            }
            KeyType.INDEXTOUSER -> {
                val index = args[0]
                storage.write(("IU$index"), data)
            }
        }
    }

    private fun writeUserStatus(token: String, data: String) {
        writeToStorage(mutableListOf(token), data, KeyType.USER)
    }

    private fun writePasswordForUser(username: String, password: String) {
        writeToStorage(mutableListOf(username, password), data = passwordSignedIn, type = KeyType.USER)
    }

    private fun writeChannelsAmountOfUser(token: String, number : Long) {
        writeToStorage(mutableListOf(token), number.toString(), KeyType.USERCHANNELS)
    }

    private fun incChannelAmountofUser(token: String) : Long {
        val number = readAmountOfChannelsForUser(token)
        writeChannelsAmountOfUser(token, number+1)
        return number+1
    }

    private fun decChannelAmountofUser(token: String) : Long {
        val number = readAmountOfChannelsForUser(token)
        assert(number > 0)
        writeChannelsAmountOfUser(token, number-1)
        return number-1
    }

    private fun setAdministrator(token: String){
        // Assumption: userIndex is valid, and is in the system.
        writeToStorage(mutableListOf(token), data = "1" , type = KeyType.ADMIN)
    }

    private fun insertToChannelsListOfUser(channel : String, token : String) {
        val str = readFromStorage(mutableListOf(token), KeyType.CHANNELS)
        val index = channelToIndex(channel)
        writeToStorage(mutableListOf(token), data = "$str%$index", type = KeyType.CHANNELS)
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

    private fun makeUserOperator(channel : String, token: String){
        writeToStorage(mutableListOf(channel, token), data = "2", type = KeyType.PARTICIPANT)
    }

    private fun makeUserConnectedToChannel(channel : String, token: String){
        writeToStorage(mutableListOf(channel, token), data = "1", type = KeyType.PARTICIPANT)
    }

    private fun disConnectUserFromChannel(channel : String, token: String){
        writeToStorage(mutableListOf(channel, token), data = "0", type = KeyType.PARTICIPANT)
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

    private fun removeFromChannelListOfUser(token: String, channel: String) : Int{
        val list = getChannelsOf(token)
        list.remove(channelToIndex(channel))
        writeToStorage(mutableListOf(token), data = list.joinToString("%"), type = KeyType.CHANNELS)
        return list.size
    }

    //end writers////////////////////////////////////////////////////

    //reader:////////////////////////////////////////////////////

    private fun readFromStorage(args : MutableList<String>, type :KeyType ) : String? {
        var str : String? = null
        when (type) {
            KeyType.USER -> {
                val token = args[0]
                str = storage.read("UV$token")
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                str = storage.read(("UP$username$password"))

            }
            KeyType.ADMIN ->{
                val token = args[0]
                str = storage.read(("UA$token"))
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                str = storage.read(("CV$index"))
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                val token = args[1]
                str = storage.read(("CU$index%$token")) //delimiter "%" between channel and token
            }
            KeyType.CHANNELS -> {
                val token = args[0]
                str = storage.read(("UCL$token"))
            }
            KeyType.TOTALUSERSAMOUNT -> {
                str = storage.read(("totalUsers"))
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                str = storage.read(("activeUsers"))
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                str = storage.read(("CL$index"))
            }
            KeyType.USERCHANNELS -> {
                val token = args[0]
                str = storage.read(("UL$token"))
            }
            KeyType.INDEXCHANNELSYS -> {
                str = storage.read(("IndexChannelSys"))
            }
            KeyType.INDEXUSERSYS -> {
                str = storage.read(("IndexUserSys"))
            }
            KeyType.CHANNELTOINDEX -> {
                val channel = args[0]
                str = storage.read(("CI$channel"))
            }
            KeyType.INDEXTOCHANNEL -> {
                val index = args[0]
                str = storage.read(("IC$index"))
            }
            KeyType.USERTOINDEX -> {
                val username = args[0]
                str = storage.read(("UI$username"))
            }
            KeyType.INDEXTOUSER -> {
                val index = args[0]
                str = storage.read(("IU$index"))
            }
        }
        return str
    }

    private fun readUserStatus(token : String) :String?{
        return readFromStorage(mutableListOf(token),  KeyType.USER)
    }

    private fun amountOfUsersInChannel(channel: String) : Long? {
        return readFromStorage(mutableListOf(channel), KeyType.CHANNEL)?.toLong()
    }

    private fun numberOfUsersInChannel(channel: String): Long{
        // Assumption: channel is valid
        return readFromStorage(mutableListOf(channel), KeyType.CHANNEL)!!.toLong()
    }

    private fun readAmountOfChannelsForUser(token: String) : Long{
        return readFromStorage(mutableListOf(token), KeyType.USERCHANNELS)!!.toLong()
    }

    private fun getChannelsOf(token: String): MutableList<String>{
        // Assumption:: token is valid
        val str = readFromStorage(mutableListOf(token), KeyType.CHANNELS)!!
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
    private fun insertUsersTree(token: String, number : Long) {
        usersTree.insert(token, number.toString())
    }

    private fun insertToTotalChannelsTree(channel : String, number : Long) {
        channelByMembersTree.insert(channel, number.toString())
    }

    private fun insertToActiveChannelsTree(channel : String, number : Long) {
        channelByActiveTree.insert(channel, number.toString())
    }


    //deletes:
    private fun  deleteFromTotalChannelsTree(channel: String){
        val number = numberOfUsersInChannel(channel)
        channelByMembersTree.delete(channel, number.toString())
    }

    private fun  deleteFromActiveChannelsTree(channel: String){
        val number = numberOfActiveUsersInChannel(channel)
        channelByActiveTree.delete(channel, number.toString())
    }

    private fun  deleteFromUsersTree(token: String) {
        val number = readAmountOfChannelsForUser(token)
        usersTree.delete(token, number.toString())
    }

    //end tree wrappers*********************************

    // ========================================= Private Functions ============================================

    private fun resetIndexSystem() {
        writeToStorage(mutableListOf(), 0.toString(), KeyType.INDEXUSERSYS)
        writeToStorage(mutableListOf(), 0.toString(), KeyType.INDEXCHANNELSYS)
    }

    private fun getNewUserIndex(username: String): String {
        val str = readFromStorage(mutableListOf(),  KeyType.INDEXUSERSYS)!!

        val newIndex = str!!.toInt() + 1
        writeToStorage(mutableListOf(newIndex.toString()), newIndex.toString(), KeyType.INDEXUSERSYS)
        attachUsernameToIndex(username, newIndex.toString())
        return newIndex.toString()
    }

    private fun getNewChannelIndex(channel: String): String{
        val str = readFromStorage(mutableListOf(),  KeyType.INDEXCHANNELSYS)!!

        val newIndex = str!!.toInt() + 1
        writeToStorage(mutableListOf(newIndex.toString()), newIndex.toString(), KeyType.INDEXCHANNELSYS)
        attachChannelToIndex(channel, newIndex.toString())
        return newIndex.toString()
    }

    private fun attachUsernameToIndex(username: String, index: String) {
        writeToStorage(mutableListOf(username), index, KeyType.USERTOINDEX)
        writeToStorage(mutableListOf(index), username, KeyType.INDEXTOUSER)
    }

    private fun attachChannelToIndex(channel: String, index: String) {
        writeToStorage(mutableListOf(channel), index, KeyType.CHANNELTOINDEX)
        writeToStorage(mutableListOf(index), channel, KeyType.INDEXTOCHANNEL)
    }

    private fun createChannel(channel: String, token: String) {             //TODO("check logic again")
        // Assumption: token is valid and is associated & the token is associated to admin

        updateAmountOfUsersInChannel(channel, 1)
        updateAmountOfLoggedInChannel(channel, 1)

        assert(isAdministrator(token))

        // make the admin as operator of this channel:
        makeUserOperator(channel, token)

        deleteFromUsersTree(token)
        val updatedChannelsAmountOfUser = incChannelAmountofUser(token)
        insertUsersTree(token, updatedChannelsAmountOfUser)

        insertToTotalChannelsTree(channel, 1)
        insertToActiveChannelsTree(channel, 1)


        insertToChannelsListOfUser(channel, token)
    }

    private fun removeUserFromChannel(channel: String, token: String){      //TODO("check logic again")
        // Assumption: token and channel is valid

        // update user channel list
        removeFromChannelListOfUser(token, channel)

        // update trees:
        deleteFromTotalChannelsTree(channel)
        deleteFromUsersTree(token)

        val updatedChannelsAmountOfUser = decChannelAmountofUser(token)
        val updatedUsersAmount = decUsersInChannel(channel)

        insertToTotalChannelsTree(channel, updatedUsersAmount)
        insertUsersTree(token, updatedChannelsAmountOfUser)

        if (readFromStorage(mutableListOf(token),  KeyType.USER) ==  userLoggedIn) {

            deleteFromActiveChannelsTree(channel)
            val updatedActiveAmount = decLoggedInChannel(channel)
            insertToActiveChannelsTree(channel, updatedActiveAmount)
        }

        disConnectUserFromChannel(channel, token)
    }

    private fun usernameToToken(username: String): String {
        return readFromStorage(mutableListOf(username), KeyType.USERTOINDEX)!!
    }

    private fun tokenToUsername(token: String): String {
        return readFromStorage(mutableListOf(token), KeyType.INDEXTOUSER)!!
    }

    private fun channelToIndex(channel: String): String {
        return readFromStorage(mutableListOf(channel), KeyType.CHANNELTOINDEX)!!
    }

    private fun indexToChannel(index: String): String {
        return readFromStorage(mutableListOf(index), KeyType.INDEXTOCHANNEL)!!
    }

    private fun passwordValid(username: String, password: String): Boolean {
        return readFromStorage(mutableListOf(username, password), KeyType.PASSWORD) != null
    }

    private fun validToken(token: String): Boolean {
        when (readFromStorage(mutableListOf(token), KeyType.USER)) {
            notRegistered, registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }
        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }

    private fun isAdministrator(token: String): Boolean{
        // Assumption: token is valid, and is in the system.
        val str = readFromStorage(mutableListOf(token), KeyType.ADMIN) ?: return false
        return str.toInt() == 1
    }

    private fun updateAssocChannels(token: String, kind: UpdateLoggedStatus){
        // Assumption:: token is valid
        val channels = getChannelsOf(token)
        for (channel in channels){ // TODO ("refactor this to updateChannel fun")
            var numOfLoggedInUsers: Long = 0
            val str: String  = readFromStorage(mutableListOf(channel), KeyType.CHANNELLOGGED)!!
            numOfLoggedInUsers = str.toLong()

            if (numOfLoggedInUsers == 0.toLong())                         // Sanity check
                assert(kind == UpdateLoggedStatus.IN)

            channelByActiveTree.delete(channel, numOfLoggedInUsers.toString())
            when(kind){
                UpdateLoggedStatus.IN -> numOfLoggedInUsers++
                UpdateLoggedStatus.OUT -> numOfLoggedInUsers--
            }
            channelByActiveTree.insert(channel, numOfLoggedInUsers.toString())

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

    private fun areUserAndChannelConnected(token: String, channel: String): Boolean{
        // Assumption: topken and channel is valid

        val str = readFromStorage(mutableListOf(channel, token), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 1 || str.toInt() == 2
    }

    private fun isOperator(token: String, channel: String): Boolean {
        // Assumption: token and channel is valid
        val str = readFromStorage(mutableListOf(channel, token), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == 2     // TODO("2 is sign for operator")
    }
}