package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageImpl
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import java.util.concurrent.CompletableFuture


class CourseAppImpl: CourseApp{

    class OurObservableImpl: OurObservable<String, Message, CompletableFuture<Unit>>()

    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null
    private val isAdmin = 1

    private val storageIo: DataStoreIo
    private var usersTree: RemoteAvlTree
    private var channelByMembersTree: RemoteAvlTree
    private var channelByActiveTree: RemoteAvlTree

    private var channelByMessagesTree: RemoteAvlTree
    private var pendingMessagesTree: RemoteAvlTree

    private var broadCastObserver: OurObservableImpl
    private var channelObservers: HashMap<String, OurObservableImpl>
    private var userObservers: HashMap<String, OurObservableImpl>
    private var callbackToToken: HashMap<ListenerCallback, String>

    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val charset: Charset = Charsets.UTF_8

    @Inject constructor(storage: DataStoreIo) {
        storageIo = storage
        usersTree = RemoteAvlTree("AvlUsers",storageIo)
        channelByMembersTree = RemoteAvlTree("AvlChannel1",storageIo)
        channelByActiveTree = RemoteAvlTree("AvlChannel2",storageIo)

        channelByMessagesTree = RemoteAvlTree("AvlChannel3",storageIo)
        pendingMessagesTree = RemoteAvlTree("pendingMessagesAvl",storageIo)
        broadCastObserver = OurObservableImpl()
        channelObservers = HashMap()
        userObservers = HashMap()
        callbackToToken = HashMap()

    }

    enum class UserStatusInChannel(val type:Int){
        Unrelated(0),
        Regular(1),
        Operator(2)
    }

    enum class UpdateLoggedStatus{
        IN,
        OUT
    }

    enum class UpdateType{
        DEC,
        INC
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
        INDEXTOCHANNEL,
        INDEXMESSAGESYS,
        CHANNELSLOGINTIME,
        MESSAGETYPE,
        MESSAGEDATA,
        MESSAGESNUMBERINCHANNEL,
        MESSAGEPENDINGCOUNTER,
        TOTALPENDINGMESSAGES,
        MESSAGESOURCE,
        USERCREATIONTIME,
        MESSAGECREATIONTIME,
        TOTALCHANNELSPENDINGMESSAGES,
        USERLASTLISTENTIME
    }

    enum class TypeMessage  {
        PRIVATE,
        CHANNEL,
        BROADCAST
    }

    init {
        // Empty
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
    override fun login(username: String, password: String): CompletableFuture<String> {
        when (readUserStatus(usernameToToken(username))) {
            notRegistered -> {
                val genToken = getNewUserIndex(username)

                writeUserStatus(genToken, userLoggedIn)
                writePasswordForUser(username, password)

                writeChannelsAmountOfUser(genToken, 0)
                insertUsersTree(genToken, 0)

                incTotalUsers()
                incTotalLoggedInUsers()

                if (getNumberOfLoggedInUsers() == 1.toLong())    // first user in the system
                    setAdministrator(genToken)

                // Add Observer for private messages
                initializeObserverForUser(genToken)

                //update user creation time:
                val time = LocalDateTime.now().format(timeFormatter)
                writeToStorage(mutableListOf(genToken), time, KeyType.USERCREATIONTIME)

                //update last listen time(default is listen at creation of user)
                updateLastUserListenTime(genToken)

                return CompletableFuture.completedFuture(genToken)
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw NoSuchEntityException()

                val token = usernameToToken(username)!!
                writeUserStatus(token, userLoggedIn)
                updateAssocChannels(token, UpdateLoggedStatus.IN)
                incTotalLoggedInUsers()

                // Add Observer for private messages
                initializeObserverForUser(token)

                return CompletableFuture.completedFuture(token)
            }

            userLoggedIn -> throw UserAlreadyLoggedInException()
        }

        assert(false)       //shouldn't get here, data-store might be corrupted.
        throw IllegalArgumentException()
    }

    private fun initializeObserverForUser(genToken: String) {
        this.userObservers[genToken] = OurObservableImpl()
    }

    /**
     * Log out the user with this authentication [token]. The [token] will be invalidated and can not be used for future
     * calls.
     *
     * This is a *delete* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     */
    override fun logout(token: String): CompletableFuture<Unit> {
        when (readUserStatus(token)) {
            notRegistered, registeredNotLoggedIn -> throw InvalidTokenException()

            userLoggedIn -> {
                writeUserStatus(token, registeredNotLoggedIn)
                updateAssocChannels(token, UpdateLoggedStatus.OUT)
                decTotalLoggedInUsers()

                // Remove Observable of this user
                this.userObservers.remove(token)
            }
        }

        return CompletableFuture.completedFuture(Unit)
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
    override fun isUserLoggedIn(token: String, username: String): CompletableFuture<Boolean?> {
        if (!validToken(token))
            throw InvalidTokenException()

        when (readUserStatus(usernameToToken(username))) {
            notRegistered -> return CompletableFuture.completedFuture(null)
            registeredNotLoggedIn -> return CompletableFuture.completedFuture(false)
            userLoggedIn -> return CompletableFuture.completedFuture(true)
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
    override fun makeAdministrator(token: String, username: String): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isAdministrator(token))
            throw UserNotAuthorizedException()

        val userToken = usernameToToken(username)
        when (readUserStatus(userToken)) {
            notRegistered -> throw NoSuchEntityException()

            registeredNotLoggedIn, userLoggedIn -> {
                setAdministrator(userToken!!)
            }
        }
        return CompletableFuture.completedFuture(Unit)
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
    override fun channelJoin(token: String, channel: String): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!validNameChannel(channel))
            throw NameFormatException()

        if (!isChannelExist(channel) && !isAdministrator(token))
            throw UserNotAuthorizedException()

        val size = amountOfUsersInChannel(channel)
        if ( size != null && size > 0){         //channel empty but was not empty in the past, or not empty now

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

            // Add new Observer for this new channel for sending messages inside the channel
            if (this.channelObservers[channel] == null)
                this.channelObservers[channel] = OurObservableImpl()        // in case of new channel or after new courseApp instance, we need new Observable

        } else {
            getNewChannelIndex(channel)
            createChannel(channel, token)       // this fun create new channel and adding the admin(token) as first user and makes him/her operator

            // Add new Observer for this new channel for sending messages inside the channel
            if (this.channelObservers[channel] == null)
                this.channelObservers[channel] = OurObservableImpl()        // in case of new channel or after new courseApp instance, we need new Observable

            insertToMessagesChannelsTree(channel, 0)
            writeToStorage(mutableListOf(channel), 0.toString(), KeyType.MESSAGESNUMBERINCHANNEL)
        }

        //add listeners of this user to the channel observable
        addListenersOfUserWhenJoiningChannel(token, channel)

        return CompletableFuture.completedFuture(Unit)
    }

    private fun addListenersOfUserWhenJoiningChannel(token: String, channel: String) {
        userObservers[token]!!.toList().asSequence().forEach { channelObservers[channel]!!.listen(it) }
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
    override fun channelPart(token: String, channel: String): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel) || !areUserAndChannelConnected(token, channel))
            throw NoSuchEntityException()

        removeUserFromChannel(channel, token)   // this fun remove user from channel and update the trees + if the chanel is empty the channel will be deleted

        // Messages System, remove from observers
        if (!isChannelExist(channel)) {
            this.channelObservers.remove(channel)        // in case of new channel or after new courseApp instance, we need new Observable
            deleteFromMessagesChannelsTree(channel)
        }
        //else
            // By the FAQ we can assume that the user have been removed all his listens

        return CompletableFuture.completedFuture(Unit)
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
    override fun channelMakeOperator(token: String, channel: String, username: String): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if(!isAdministrator(token) && !isOperator(token, channel))
            throw UserNotAuthorizedException()

        if (isAdministrator(token) && !isOperator(token, channel) && usernameToToken(username) != token)
            throw UserNotAuthorizedException()

        if(!areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        val userToken = usernameToToken(username)
        if(readUserStatus(userToken) == notRegistered || !areUserAndChannelConnected(userToken!!, channel))
            throw NoSuchEntityException()

        makeUserOperator(channel, userToken)
        return CompletableFuture.completedFuture(Unit)
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
    override fun channelKick(token: String, channel: String, username: String): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isOperator(token, channel))
            throw UserNotAuthorizedException()

        val userToken = usernameToToken(username)
        if (userToken == notRegistered || !areUserAndChannelConnected(userToken, channel))
            throw NoSuchEntityException()

        removeUserFromChannel(channel, userToken)   // this fun remove user from channel and update the trees

        // Messages System, remove from observers
        if (!isChannelExist(channel)) {
            this.channelObservers.remove(channel)        // in case of new channel or after new courseApp instance, we need new Observable
            deleteFromMessagesChannelsTree(channel)
        }
        //else
        // By the FAQ we can assume that the user have been removed all his listens

        return CompletableFuture.completedFuture(Unit)
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
    override fun isUserInChannel(token: String, channel: String, username: String): CompletableFuture<Boolean?> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        val userToken = usernameToToken(username) ?: return CompletableFuture.completedFuture(null)
        return CompletableFuture.completedFuture(areUserAndChannelConnected(userToken, channel))
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
    override fun numberOfActiveUsersInChannel(token: String, channel: String): CompletableFuture<Long> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        return CompletableFuture.completedFuture(numberOfActiveUsersInChannel(channel))
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
    override fun numberOfTotalUsersInChannel(token: String, channel: String): CompletableFuture<Long> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!isChannelExist(channel))
            throw NoSuchEntityException()

        if (!isAdministrator(token) && !areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        return CompletableFuture.completedFuture(numberOfUsersInChannel(channel))
    }

    /**
     * Adds a listener to this Course App instance. See Observer design pattern for more information.
     *
     * See the assignment PDF for semantics.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException if the auth [token] is invalid.
     */
    override fun addListener(token: String, callback: ListenerCallback): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()

        addListenerToBroadcastObserver(callback)
        addListenerToChannelsObserver(token, callback)
        addListenerToPrivateObserver(token, callback)

        activatePendingMessagesFor(token, callback)

        callbackToToken[callback] = token

        //update last listen time(default is listen at creation of user)
        updateLastUserListenTime(token)

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Remove a listener from this Course App instance. See Observer design pattern for more information.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [callback] is not registered with this instance.
     */
    override fun removeListener(token: String, callback: ListenerCallback): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()
        if(!isListenerExist(callback))
            throw NoSuchEntityException()

        removeListenerFromBroadcastObserver(callback)
        removeListenerFromChannelsObserver(token, callback)
        removeListenerFromPrivateObserver(token, callback)

        callbackToToken.remove(callback)

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Send a message to a channel from the user identified by [token]. Listeners will be notified, source will be
     * "[channel]@<user>" (including the leading `"`). So, if `gal` sent a message to `#236700`, the source will be
     * `#236700@gal`.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] identifies a user who is not a member of [channel].
     */
    override fun channelSend(token: String, channel: String, message: Message): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()
        if(!isChannelExist(channel))
            throw NoSuchEntityException()
        if(!areUserAndChannelConnected(token, channel))
            throw UserNotAuthorizedException()

        val username = tokenToUsername(token)
        val source = "$channel@$username"

        assocMessage(message, TypeMessage.CHANNEL, data = channel)
        saveMessageInStorage(source, message)
        updateNumberOfMessagesInChannel(channel, UpdateType.INC)
        sendMessageInChannel(token, channel, source, message)

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Sends a message to all users from an admin identified by [token]. Listeners will be notified, source is
     * "BROADCAST".
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws UserNotAuthorizedException If [token] does not identify an administrator.
     */
    override fun broadcast(token: String, message: Message): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()
        if(!isAdministrator(token))
            throw UserNotAuthorizedException()

        val source = "BROADCAST"

        assocMessage(message, TypeMessage.BROADCAST)
        saveMessageInStorage(source, message)
        sendMessageInBroadcast(source, message)

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Sends a private message from the user identified by [token] to [user]. Listeners will be notified, source will be
     * "@<user>", where <user> is the user identified by [token]. So, if `gal` sent `matan` a message, that source will
     * be `@gal`.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [user] does not exist.
     */
    override fun privateSend(token: String, user: String, message: Message): CompletableFuture<Unit> {
        if (!validToken(token))
            throw InvalidTokenException()
        val userToken = usernameToToken(user)
        val res = readUserStatus(userToken)
        when(res) {
            notRegistered -> throw NoSuchEntityException()
            registeredNotLoggedIn, userLoggedIn -> {
                val username = tokenToUsername(token)
                val source = "@$username"

                assocMessage(message, TypeMessage.PRIVATE, data = userToken)
                saveMessageInStorage(source, message)
                sendMessageInPrivate(userToken!!, source, message)
            }
        }

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Returns the message identified by [id], if it exists.
     *
     * This method is only useful for messages sent to channels.
     *
     * This is a *read* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [id] does not exist or is not a channel message.
     * @throws UserNotAuthorizedException If [id] identifies a message in a channel that the user identified by [token]
     * is not a member of.
     * @return The message identified by [id] along with its source.
     */
    override fun fetchMessage(token: String, id: Long): CompletableFuture<Pair<String, Message>> {
        if (!validToken(token))
            throw InvalidTokenException()

        if (!validMessage(id) || !validChannelMessage(id))
            throw NoSuchEntityException()

        if (!messageIsSameChannelAsUser(token, id))
            throw UserNotAuthorizedException()

        return fetchMessageAux(id)
    }

    // =========================================== API for statistics ==================================================

    fun getTotalUsers(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.TOTALUSERSAMOUNT)
        return str?.toLong() ?: 0
    }

    fun getTotalActiveUsers(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT)
        return str?.toLong() ?: 0
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

    fun getPendingMessagesNumberForUsers(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.TOTALPENDINGMESSAGES)
        return str?.toLong() ?: 0
    }

    fun getPendingMessagesNumberForChannels(): Long {
        val str = readFromStorage(mutableListOf(), KeyType.TOTALCHANNELSPENDINGMESSAGES)
        return str?.toLong() ?: 0
    }

    fun getTop10ChannelsByMessagesNumber(): List<String> {
        return this.channelByMessagesTree.top10().asSequence().map { indexToChannel(it.toString()) }.toList()
    }

    //writers:////////////////////////////////////////////////////

    private fun writeToStorage(args : MutableList<String>, data: String, type :KeyType )  {
        when (type) {
            KeyType.USER -> {
                val token = args[0]
                storageIo.write("UV$token", data).get()
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                storageIo.write(("UP$username$password"), data).get()
            }
            KeyType.ADMIN ->{
                val userIndex = args[0]
                storageIo.write(("UA$userIndex"),data).get()
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                storageIo.write(("CV$index"), data).get()
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                val token = args[1]
                storageIo.write(("CU$index%$token"), data).get() //delimiter "%" between channel and token
            }
            KeyType.CHANNELS -> {
                val token = args[0]
                storageIo.write(("UCL$token"), data).get()
            }
            KeyType.TOTALUSERSAMOUNT -> {
                storageIo.write(("totalUsers"), data).get()
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                storageIo.write(("activeUsers"), data).get()
            }
            KeyType.CHANNELLOGGED -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                storageIo.write(("CL$index"), data).get()
            }
            KeyType.USERCHANNELS -> {
                val token = args[0]
                storageIo.write(("UL$token"), data).get()
            }
            KeyType.INDEXCHANNELSYS -> {
                storageIo.write(("IndexChannelSys"), data).get()
            }
            KeyType.INDEXUSERSYS -> {
                storageIo.write(("IndexUserSys"), data).get()
            }
            KeyType.CHANNELTOINDEX -> {
                val channel = args[0]
                storageIo.write(("CI$channel"), data).get()
            }
            KeyType.INDEXTOCHANNEL -> {
                val index = args[0]
                storageIo.write(("IC$index"), data).get()
            }
            KeyType.USERTOINDEX -> {
                val username = args[0]
                storageIo.write(("UI$username"), data).get()
            }
            KeyType.INDEXTOUSER -> {
                val index = args[0]
                storageIo.write(("IU$index"), data).get()
            }
            KeyType.INDEXMESSAGESYS -> {
                storageIo.write(("IndexMessageSys"), data).get()
            }
            KeyType.CHANNELSLOGINTIME -> {
                val token = args[0]
                storageIo.write(("CTL$token"), data).get()
            }
            KeyType.MESSAGETYPE -> {
                val id = args[0]
                storageIo.write(("MSGT$id"), data).get()
            }
            KeyType.MESSAGEDATA -> {
                val id = args[0]
                storageIo.write(("MSGD$id"), data).get()
            }
            KeyType.MESSAGESNUMBERINCHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                storageIo.write(("MNC$index"), data).get()
            }
            KeyType.MESSAGEPENDINGCOUNTER -> {
                val id = args[0]
                storageIo.write(("MPC$id"), data).get()
            }
            KeyType.TOTALPENDINGMESSAGES -> {
                storageIo.write(("PendingMessagesInSystem"), data).get()
            }
            KeyType.MESSAGESOURCE -> {
                val id = args[0]
                storageIo.write(("MSGS$id"), data).get()
            }
            KeyType.USERCREATIONTIME -> {
                val token = args[0]
                storageIo.write(("UCT$token"), data).get()
            }
            KeyType.MESSAGECREATIONTIME -> {
                val id = args[0]
                storageIo.write(("MSGCT$id"), data).get()
            }
            KeyType.TOTALCHANNELSPENDINGMESSAGES -> {
                storageIo.write(("TCPM"), data).get()
            }
            KeyType.USERLASTLISTENTIME -> {
                val token = args[0]
                storageIo.write(("USERLASTTISTEN$token"), data).get()
            }
        }
    }

    private fun writeUserStatus(token: String, data: String) {
        writeToStorage(mutableListOf(token), data, KeyType.USER)
    }

    private fun writePasswordForUser(username: String, password: String) {
        writeToStorage(mutableListOf(username, password), data = passwordSignedIn, type = KeyType.PASSWORD)
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
        writeToStorage(mutableListOf(token), data = isAdmin.toString() , type = KeyType.ADMIN)
    }

    private fun insertToChannelsListOfUser(channel : String, token : String) {
        val str = readFromStorage(mutableListOf(token), KeyType.CHANNELS)
        val index = channelToIndex(channel)
        val newStr = if (str == null || str == "") "$index" else "$str%$index"

        writeToStorage(mutableListOf(token), data = newStr, type = KeyType.CHANNELS)

        // Add channel join time for using is the Messages system
        val str2 = readFromStorage(mutableListOf(token), KeyType.CHANNELSLOGINTIME)
        val currTime = LocalDateTime.now()
        val newStr2 = if (str2 == null || str2 == "") "$currTime" else "$str2%$currTime"

        writeToStorage(mutableListOf(token), data = newStr2, type = KeyType.CHANNELSLOGINTIME)
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
        writeToStorage(mutableListOf(channel, token), data = UserStatusInChannel.Operator.type.toString(), type = KeyType.PARTICIPANT)
    }

    private fun makeUserConnectedToChannel(channel : String, token: String){
        writeToStorage(mutableListOf(channel, token), data = UserStatusInChannel.Regular.type.toString(), type = KeyType.PARTICIPANT)
    }

    private fun disConnectUserFromChannel(channel : String, token: String){
        writeToStorage(mutableListOf(channel, token), data = UserStatusInChannel.Unrelated.type.toString(), type = KeyType.PARTICIPANT)
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

    private fun decTotalLoggedInUsers(){
        var totalLoggedUsers: Int
        val str = readFromStorage(mutableListOf(), KeyType.ACTIVEUSERSAMOUNT) !!
        totalLoggedUsers = str.toInt()

        totalLoggedUsers--
        writeToStorage(mutableListOf(), data = totalLoggedUsers.toString(), type = KeyType.ACTIVEUSERSAMOUNT)
    }

    private fun removeFromChannelListOfUser(token: String, channel: String) : Int{
        val list = getChannelsOf(token)
        val i = list.indexOf(channelToIndex(channel))

        list.remove(channelToIndex(channel))
        writeToStorage(mutableListOf(token), data = list.joinToString("%"), type = KeyType.CHANNELS)

        // remove channel join time for using is the Messages system
        val str2 = readFromStorage(mutableListOf(token), KeyType.CHANNELSLOGINTIME)
        if (str2 != null) {
            val list2 = str2.split("%").toMutableList()
            list2.removeAt(i)
            var retStr = ""
            list2.forEach { retStr = if(retStr == "") it else "$retStr%$it" }

            writeToStorage(mutableListOf(token), data = retStr, type = KeyType.CHANNELSLOGINTIME)
        } else {
            assert(false)           // sanity check
        }

        return list.size
    }

    //end writers////////////////////////////////////////////////////

    //reader:////////////////////////////////////////////////////

    private fun  readFromStorage(args : MutableList<String>, type :KeyType ) : String? {
        var str : String? = null
        when (type) {
            KeyType.USER -> {
                val token = args[0]
                str = storageIo.read("UV$token").get()
            }
            KeyType.PASSWORD -> {
                val username = args[0]
                val password = args[1]
                str = storageIo.read(("UP$username$password")).get()
            }
            KeyType.ADMIN ->{
                val token = args[0]
                str = storageIo.read(("UA$token")).get()
            }
            KeyType.CHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                str = storageIo.read(("CV$index")).get()
            }
            KeyType.PARTICIPANT -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                val token = args[1]
                str = storageIo.read(("CU$index%$token")).get() //delimiter "%" between channel and token
            }
            KeyType.CHANNELS -> {
                val token = args[0]
                str = storageIo.read(("UCL$token")).get()
            }
            KeyType.TOTALUSERSAMOUNT -> {
                str = storageIo.read(("totalUsers")).get()
            }
            KeyType.ACTIVEUSERSAMOUNT -> {
                str = storageIo.read(("activeUsers")).get()
            }
            KeyType.CHANNELLOGGED -> {
                val index = args[0]
                str = storageIo.read(("CL$index")).get()
            }
            KeyType.USERCHANNELS -> {
                val token = args[0]
                str = storageIo.read(("UL$token")).get()
            }
            KeyType.INDEXCHANNELSYS -> {
                str = storageIo.read(("IndexChannelSys")).get()
            }
            KeyType.INDEXUSERSYS -> {
                str = storageIo.read(("IndexUserSys")).get()
            }
            KeyType.CHANNELTOINDEX -> {
                val channel = args[0]
                str = storageIo.read(("CI$channel")).get()
            }
            KeyType.INDEXTOCHANNEL -> {
                val index = args[0]
                str = storageIo.read(("IC$index")).get()
            }
            KeyType.USERTOINDEX -> {
                val username = args[0]
                str = storageIo.read(("UI$username")).get()
            }
            KeyType.INDEXTOUSER -> {
                val index = args[0]
                str = storageIo.read(("IU$index")).get()
            }
            KeyType.INDEXMESSAGESYS -> {
                str = storageIo.read(("IndexMessageSys")).get()
            }
            KeyType.CHANNELSLOGINTIME -> {
                val token = args[0]
                str = storageIo.read(("CTL$token")).get()
            }
            KeyType.MESSAGETYPE -> {
                val id = args[0]
                str = storageIo.read(("MSGT$id")).get()
            }
            KeyType.MESSAGEDATA -> {
                val id = args[0]
                str = storageIo.read(("MSGD$id")).get()
            }
            KeyType.MESSAGESNUMBERINCHANNEL -> {
                val channel = args[0]
                val index = channelToIndex(channel)
                str = storageIo.read(("MNC$index")).get()
            }
            KeyType.MESSAGEPENDINGCOUNTER -> {
                val id = args[0]
                str = storageIo.read(("MPC$id")).get()
            }
            KeyType.TOTALPENDINGMESSAGES -> {
                str = storageIo.read(("PendingMessagesInSystem")).get()
            }
            KeyType.MESSAGESOURCE -> {
                val id = args[0]
                str = storageIo.read(("MSGS$id")).get()
            }
            KeyType.USERCREATIONTIME -> {
                val token = args[0]
                str = storageIo.read(("UCT$token")).get()
            }
            KeyType.MESSAGECREATIONTIME -> {
                val id = args[0]
                str = storageIo.read(("MSGCT$id")).get()
            }
            KeyType.TOTALCHANNELSPENDINGMESSAGES -> {
                str = storageIo.read(("TCPM")).get()
            }
            KeyType.USERLASTLISTENTIME -> {
                val token = args[0]
                str = storageIo.read(("USERLASTTISTEN$token")).get()
            }
        }
        return str
    }

    private fun readUserStatus(token : String?) :String?{
        if (token == null)
            return notRegistered
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
        val str = readFromStorage(mutableListOf(token), KeyType.CHANNELS) ?: return mutableListOf()
        val delimiter = "%"
        return str.split(delimiter).toMutableList()
    }

    private fun numberOfActiveUsersInChannel(channel: String): Long {
        // Assumption: channel is valid
        val channelIndex = channelToIndex(channel)
        return readFromStorage(mutableListOf(channelIndex!!), KeyType.CHANNELLOGGED)!!.toLong()
    }

    //end readers////////////////////////////////////////////////////

    //tree wrappers:*********************************

    //inserts:
    private fun insertUsersTree(token: String, number : Long) {
        usersTree.insert(token, number.toString())
    }

    private fun insertToTotalChannelsTree(channel : String, number : Long) {
        val channelIndex = channelToIndex(channel)!!
        channelByMembersTree.insert(channelIndex, number.toString())
    }

    private fun insertToActiveChannelsTree(channel : String, number : Long) {
        val channelIndex = channelToIndex(channel)!!
        channelByActiveTree.insert(channelIndex, number.toString())
    }

    private fun insertToMessagesChannelsTree(channel : String, number : Long) {
        val channelIndex = channelToIndex(channel)!!
        channelByMessagesTree.insert(channelIndex, number.toString())
    }


    //deletes:
    private fun  deleteFromTotalChannelsTree(channel: String){
        val number = numberOfUsersInChannel(channel)
        val channelIndex = channelToIndex(channel)!!
        channelByMembersTree.delete(channelIndex, number.toString())
    }

    private fun  deleteFromActiveChannelsTree(channel: String){
        val number = numberOfActiveUsersInChannel(channel)
        val channelIndex = channelToIndex(channel)!!
        channelByActiveTree.delete(channelIndex, number.toString())
    }

    private fun  deleteFromUsersTree(token: String) {
        val number = readAmountOfChannelsForUser(token)
        usersTree.delete(token, number.toString())
    }

    private fun  deleteFromMessagesChannelsTree(channel: String){
        val number = getNumberOfMessageIn(channel)
        val channelIndex = channelToIndex(channel)!!
        channelByMessagesTree.delete(channelIndex, number.toString())
    }

    //end tree wrappers*********************************

    // ========================================= Private Functions ============================================

    private fun getNewUserIndex(username: String): String {
        val str = readFromStorage(mutableListOf(),  KeyType.INDEXUSERSYS)

        val newIndex = if (str == null) 1 else str.toInt() + 1

        writeToStorage(mutableListOf(newIndex.toString()), newIndex.toString(), KeyType.INDEXUSERSYS)
        attachUsernameToIndex(username, newIndex.toString())
        return newIndex.toString()
    }

    private fun getNewChannelIndex(channel: String): String{
        val str = readFromStorage(mutableListOf(),  KeyType.INDEXCHANNELSYS)

        val newIndex = if (str == null) 1 else str.toInt() + 1

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

    private fun createChannel(channel: String, token: String) {
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

    private fun removeUserFromChannel(channel: String, token: String){
        // Assumption: token and channel is valid

        // update user channel list
        removeFromChannelListOfUser(token, channel)

        // update trees:
        deleteFromTotalChannelsTree(channel)
        deleteFromUsersTree(token)

        val updatedChannelsAmountOfUser = decChannelAmountofUser(token)
        val updatedUsersAmount = decUsersInChannel(channel)

        if (updatedUsersAmount != 0.toLong()) {
            insertToTotalChannelsTree(channel, updatedUsersAmount)
            insertUsersTree(token, updatedChannelsAmountOfUser)

            if (readFromStorage(mutableListOf(token), KeyType.USER) == userLoggedIn) {

                deleteFromActiveChannelsTree(channel)
                val updatedActiveAmount = decLoggedInChannel(channel)
                insertToActiveChannelsTree(channel, updatedActiveAmount)
            }
        }

        disConnectUserFromChannel(channel, token)
    }

    private fun usernameToToken(username: String): String? {
        return readFromStorage(mutableListOf(username), KeyType.USERTOINDEX)
    }

    private fun tokenToUsername(token: String): String {
        return readFromStorage(mutableListOf(token), KeyType.INDEXTOUSER)!!
    }

    private fun channelToIndex(channel: String): String? {
        return readFromStorage(mutableListOf(channel), KeyType.CHANNELTOINDEX)
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
        return str.toInt() == isAdmin
    }

    private fun updateAssocChannels(token: String, kind: UpdateLoggedStatus){
        // Assumption:: token is valid
        val channelIndexesList = getChannelsOf(token)
        for (channelIndex in channelIndexesList){
            val str: String  = readFromStorage(mutableListOf(channelIndex), KeyType.CHANNELLOGGED)!!
            var numOfLoggedInUsers = str.toLong()

            if (numOfLoggedInUsers == 0.toLong())                         // Sanity check
                assert(kind == UpdateLoggedStatus.IN)

            val channel = indexToChannel(channelIndex)
            channelByActiveTree.delete(channelIndex, numOfLoggedInUsers.toString())
            when(kind){
                UpdateLoggedStatus.IN -> {
                    numOfLoggedInUsers++
                    incLoggedInChannel(channel)
                }
                UpdateLoggedStatus.OUT -> {
                    numOfLoggedInUsers--
                    decLoggedInChannel(channel)
                }
            }
            channelByActiveTree.insert(channelIndex, numOfLoggedInUsers.toString())

            writeToStorage(mutableListOf(channelIndex), data = numOfLoggedInUsers.toString(), type = KeyType.CHANNELLOGGED)
        }
    }

    private fun validNameChannel(channel: String): Boolean{
        val regex = Regex("((#)([0-9]|[a-z]|[A-Z]|(#)|(_))*)")      // Regex pattern of valid channel
        return regex.matchEntire(channel)?.value != null
    }

    private fun isChannelExist(channel: String): Boolean{
        val str = readFromStorage(mutableListOf(channel), KeyType.CHANNEL) ?: return false
        return str.toInt() > 0
    }

    private fun areUserAndChannelConnected(token: String, channel: String): Boolean{
        // Assumption: topken and channel is valid

        val str = readFromStorage(mutableListOf(channel, token), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == UserStatusInChannel.Regular.type || str.toInt() == UserStatusInChannel.Operator.type
    }

    private fun isOperator(token: String, channel: String): Boolean {
        // Assumption: token and channel is valid
        val str = readFromStorage(mutableListOf(channel, token), KeyType.PARTICIPANT) ?: return false
        return str.toInt() == UserStatusInChannel.Operator.type
    }

    private fun activatePendingMessagesFor(token: String, callback: ListenerCallback) {
        val channelList = getChannelsOf(token)
        val userCreationTime = LocalDateTime.parse(readFromStorage(mutableListOf(token), KeyType.USERCREATIONTIME), timeFormatter)
        val userLastListenTime = LocalDateTime.parse(readFromStorage(mutableListOf(token), KeyType.USERLASTLISTENTIME), timeFormatter)

        val pendingMessagesList = this.pendingMessagesTree.toKeyList()
        for (currId in pendingMessagesList) {
            val messageCreationTime = LocalDateTime.parse(readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGECREATIONTIME), timeFormatter)
            if (userCreationTime <= messageCreationTime && userLastListenTime <= messageCreationTime) {
                val str = readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGETYPE)!!
                val list = str.split("%")
                val type = list[0]
                when (type) {
                    "PRIVATE" -> {
                        val receiveToken = list[1]

                        if (receiveToken == token) {
                            activateLambdaWithSourceAndMessageFromStorage(currId, callback)
                            updateMessageCounterAndTotalPending(currId)
                        }
                    }
                    "CHANNEL" -> {
                        val channelOfMessage = list[1]

                        if (channelList.contains(channelOfMessage)) {
                            activateLambdaWithSourceAndMessageFromStorage(currId, callback)
                            if (decPendingUsersForMessage(currId) == 0.toLong()) {
                                decTotalNumberOfPendingChannelsMessages()
                                removeMessageFromPending(currId)
                            }
                        }
                    }
                    "BROADCAST" -> {
                        activateLambdaWithSourceAndMessageFromStorage(currId, callback)
                        updateMessageCounterAndTotalPending(currId)
                    }
                }
            }
        }
    }

    private fun decPendingUsersForMessage(currId: Long): Long {
        val prevPendingNumber = readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGEPENDINGCOUNTER)!!.toLong()
        val currPendingNumber = prevPendingNumber - 1.toLong()
        writeToStorage(mutableListOf(currId.toString()), currPendingNumber.toString(), KeyType.MESSAGEPENDINGCOUNTER)
        return currPendingNumber
    }

    private fun updateMessageCounterAndTotalPending(currId: Long) {
        val prevPendingNumber = readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGEPENDINGCOUNTER)!!.toLong()
        val currPendingNumber = prevPendingNumber - 1.toLong()
        writeToStorage(mutableListOf(currId.toString()), currPendingNumber.toString(), KeyType.MESSAGEPENDINGCOUNTER)
        decTotalPendingMessagesIfNeeded(currPendingNumber, currId)
    }

    private fun decTotalPendingMessagesIfNeeded(currPendingNumber: Long, currId: Long) {
        if (currPendingNumber == 0.toLong()) {
            removeMessageFromPending(currId)

            val prevTotalPendingMessages = readFromStorage(mutableListOf(), KeyType.TOTALPENDINGMESSAGES)!!.toLong()
            val currTotalPendingMessages = prevTotalPendingMessages - 1.toLong()
            writeToStorage(mutableListOf(currId.toString()), currTotalPendingMessages.toString(), KeyType.TOTALPENDINGMESSAGES)
        }
    }

    private fun activateLambdaWithSourceAndMessageFromStorage(currId: Long, callback: ListenerCallback) {
        val source = readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGESOURCE)!!

        val str2 = readFromStorage(mutableListOf(currId.toString()), KeyType.MESSAGEDATA)
        val listOfFields = str2!!.split("%")

        val receivedTime = if (listOfFields[4] == "null") LocalDateTime.now()
                                         else LocalDateTime.parse(listOfFields[4], timeFormatter)

        val message = MessageImpl(id = listOfFields[0].toLong(),
                media = MediaType.valueOf(listOfFields[1]),
                contents = listOfFields[2].toByteArray(charset),
                created = LocalDateTime.parse(listOfFields[3], timeFormatter),
                received = receivedTime)

        saveMessageInStorage(source, message)
        callback(source, message)
    }

    private fun addListenerToBroadcastObserver(callback: ListenerCallback) {
        this.broadCastObserver.listen(callback)
    }

    private fun addListenerToChannelsObserver(token: String, callback: ListenerCallback) {
        val channelList = getChannelsOf(token)
        for (channelIndex in channelList)
            this.channelObservers[indexToChannel(channelIndex)]!!.listen(callback)
    }

    private fun addListenerToPrivateObserver(token: String, callback: ListenerCallback) {
        this.userObservers[token]!!.listen(callback)
    }

    private fun isListenerExist(callback: ListenerCallback): Boolean {
        /*
            broadCastObserver contains all the listeners of the system, so it is a nice place to check if
            a specific user is a listener.
        */
        return this.broadCastObserver.contains(callback)
    }

    private fun removeListenerFromBroadcastObserver( callback: ListenerCallback) {
        this.broadCastObserver.unlisten(callback)
    }

    private fun removeListenerFromChannelsObserver(token: String, callback: ListenerCallback) {
        val channelList = getChannelsOf(token)
        for (channelIndex in channelList) {
            val channelName = indexToChannel(channelIndex)
            this.channelObservers[channelName]!!.unlisten(callback)
        }
    }

    private fun removeListenerFromPrivateObserver(token: String, callback: ListenerCallback) {
        this.userObservers[token]!!.unlisten(callback)
    }

    private fun assocMessage(message: Message, type: TypeMessage, data: String? = null) {
        when (type) {
            TypeMessage.PRIVATE -> {
                val token = data!!         // token of the user that the message is for
                writeToStorage(mutableListOf(message.id.toString()), "PRIVATE%$token", KeyType.MESSAGETYPE)
            }
            TypeMessage.CHANNEL -> {
                val channelIndex = channelToIndex(data!!)         // channel index of the the message is associated for
                writeToStorage(mutableListOf(message.id.toString()), "CHANNEL%$channelIndex", KeyType.MESSAGETYPE)
            }
            TypeMessage.BROADCAST -> {
                writeToStorage(mutableListOf(message.id.toString()), "BROADCAST", KeyType.MESSAGETYPE)
            }
        }
    }

    private fun saveMessageInStorage(source: String, message: Message) {
        val id = message.id
        val str = message.toString()
        writeToStorage(mutableListOf(id.toString()), str, KeyType.MESSAGEDATA)
        writeToStorage(mutableListOf(id.toString()), message.created.toString(), KeyType.MESSAGECREATIONTIME)
        writeToStorage(mutableListOf(id.toString()), source, KeyType.MESSAGESOURCE)
    }

    private fun updateNumberOfMessagesInChannel(channel: String, type: UpdateType): Long {
        deleteFromMessagesChannelsTree(channel)

        val numberOfMessages = when (type) {
            UpdateType.DEC -> getNumberOfMessageIn(channel) - 1
            UpdateType.INC -> getNumberOfMessageIn(channel) + 1
        }
        writeToStorage(mutableListOf(channel), numberOfMessages.toString(), KeyType.MESSAGESNUMBERINCHANNEL)

        insertToMessagesChannelsTree(channel, numberOfMessages)
        return numberOfMessages
    }

    private fun sendMessageInChannel(token: String, channel: String, source: String, message: Message) {
        val obs= this.channelObservers[channel]!!
        obs.onChange(source, message)

        val receivedUsers:MutableList<String> = getListenUsers(obs)
        val numberOfPendingUsers = numberOfTotalUsersInChannel(token, channel).get() - receivedUsers.size.toLong()

        if (numberOfPendingUsers != 0.toLong()) {
            incTotalNumberOfPendingChannelsMessages()
            addMessageToPending(message.id)
            writeToStorage(mutableListOf(message.id.toString()), numberOfPendingUsers.toString(), KeyType.MESSAGEPENDINGCOUNTER)
        }
    }

    private fun incTotalNumberOfPendingChannelsMessages() {
        val previousNumberOfPendingMessages = getPendingMessagesNumberForChannels()
        val currPendingNumberOfPendingMessages = previousNumberOfPendingMessages + 1
        writeToStorage(mutableListOf(), currPendingNumberOfPendingMessages.toString(), KeyType.TOTALCHANNELSPENDINGMESSAGES)
    }

    private fun decTotalNumberOfPendingChannelsMessages() {
        val previousNumberOfPendingMessages = getPendingMessagesNumberForChannels()
        val currPendingNumberOfPendingMessages = previousNumberOfPendingMessages - 1
        writeToStorage(mutableListOf(), currPendingNumberOfPendingMessages.toString(), KeyType.TOTALCHANNELSPENDINGMESSAGES)
    }

    private fun addMessageToPending(id: Long) {
        pendingMessagesTree.insert(id.toString(), id.toString())
    }

    private fun removeMessageFromPending(id: Long) {
        pendingMessagesTree.delete(id.toString(), id.toString())
    }

    private fun sendMessageInBroadcast(source: String, message: Message) {
        this.broadCastObserver.onChange(source, message)

        val receivedUsers:MutableList<String> = getListenUsers(this.broadCastObserver)
        val numberOfPendingUsers = getTotalUsers() - receivedUsers.size.toLong()

        if (numberOfPendingUsers != 0.toLong()) {
            addMessageToPending(message.id)
            writeToStorage(mutableListOf(message.id.toString()), numberOfPendingUsers.toString(), KeyType.MESSAGEPENDINGCOUNTER)
        }

        incNumberOfPendingMessages()
    }

    private fun getListenUsers(observer: CourseAppImpl.OurObservableImpl): MutableList<String> {
        val observersList = observer.toList()
        val users = mutableListOf<String>()
        for (currObservable in observersList) {
            val user = callbackToToken[currObservable]
            if (user != null) {
                users.add(user)
            }
        }
        return users
    }

    private fun getNumberOfPendingMessages(): Long {
        return readFromStorage(mutableListOf(), KeyType.TOTALPENDINGMESSAGES)?.toLong() ?: 0
    }

    private fun incNumberOfPendingMessages() {
        val numberOfPendingMessages = getNumberOfPendingMessages() + 1.toLong()
        writeToStorage(mutableListOf(), numberOfPendingMessages.toString(), KeyType.TOTALPENDINGMESSAGES)
    }

    private fun sendMessageInPrivate(token: String, source: String, message: Message) {
        if (this.userObservers[token] != null && !this.userObservers[token]!!.isEmpty())
            this.userObservers[token]!!.onChange(source, message)
        else {
            addMessageToPending(message.id)
            writeToStorage(mutableListOf(message.id.toString()), 1.toString(), KeyType.MESSAGEPENDINGCOUNTER)
            incNumberOfPendingMessages()
        }
    }

    private fun validMessage(id: Long): Boolean{
        val lastIndex = readFromStorage(mutableListOf(),KeyType.INDEXMESSAGESYS)!!.toLong()
        return id <= lastIndex && readFromStorage(mutableListOf(id.toString()), KeyType.MESSAGECREATIONTIME) != null
    }

    private fun validChannelMessage(id: Long): Boolean {
        val str = readFromStorage(mutableListOf(id.toString()), KeyType.MESSAGETYPE)
        val list = str!!.split("%")
        val type = list[0]
        if (type != "CHANNEL")
            return false

        val channelMessageIndex = list[1]
        return isChannelExist(indexToChannel(channelMessageIndex))
        // second check is for messages that was associated to deleted channel
    }

    private fun messageIsSameChannelAsUser(token: String, id: Long): Boolean {
        val channelIndex = readFromStorage(mutableListOf(id.toString()), KeyType.MESSAGETYPE)!!.split("%")[1]
        return getChannelsOf(token).asSequence().map { indexToChannel(it) }.toList().contains(indexToChannel(channelIndex))
    }

    private fun fetchMessageAux( id: Long): CompletableFuture<Pair<String, Message>> {
        var ret: Pair<String, Message>? = null
        val lamd : ListenerCallback = { s: String, message: Message -> ret = Pair(s, message)
            CompletableFuture.completedFuture(Unit)
        }
        activateLambdaWithSourceAndMessageFromStorage(id, lamd)

        return CompletableFuture.completedFuture(ret)
    }

    private fun getNumberOfMessageIn(channel: String): Long {
        return readFromStorage(mutableListOf(channel), KeyType.MESSAGESNUMBERINCHANNEL)?.toLong() ?: 0
    }

    private fun updateLastUserListenTime(token: String) {
        val time = LocalDateTime.now()
        writeToStorage(mutableListOf(token), time.format(timeFormatter), KeyType.USERLASTLISTENTIME)
    }
}