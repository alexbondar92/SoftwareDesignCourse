package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*

class CourseAppImpl : CourseApp{
    private val userLoggedIn = "1"
    private val passwordSignedIn = "1"
    private val registeredNotLoggedIn = "0"
    private val notRegistered = null

    private var usersTree = MyAvlTree(1,DataStoreIo())      // TODO("Remake")

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
        when (readDataForUsername(username)) {
            notRegistered -> {
                writeDataForUser(username, userLoggedIn)        // TODO ("refactor")
                writePasswordForUser(username, password)        // TODO ("refactor")
                usersTree.insert(createKey(username), createData(username))     // TODO ("move this out of the logic")
                if (numOfUsers() == 1)    // first user in the system
                    setAdministrator(username)

                TODO("Inc number of total users by one")
                TODO("Inc number of total active users by one")
                return usernameToToken(username)
            }

            registeredNotLoggedIn -> {
                if (!passwordValid(username, password))
                    throw NoSuchEntityException()

                writeDataForUser(username, userLoggedIn)        // TODO ("refactor")
                updateAssocChannels(username, "loggedIn")
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
        when (readDataForUsername(username)) {
            notRegistered, registeredNotLoggedIn -> throw InvalidTokenException()

            userLoggedIn -> {
                writeDataForUser(username, registeredNotLoggedIn)        // TODO ("refactor")
                updateAssocChannels(username, "loggedOut")
                TODO("Dec number of total active users by one")
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

        when (readDataForUsername(username)) {                      // TODO ("refactor")
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

        when (readDataForUsername(username)) {      // TODO(refactor)
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

        createChannel(channel, token)       // this fun create new channel and adding the admin(token) as first user and makes him/her operator
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

        if (readDataForUsername(username) == notRegistered || !isUserInChannelAux(username, channel))
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

        return isUserInChannelAux(username, channel);
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

    private fun readDataForUsername(username: String): String? {
        return DataStoreIo.read("U$username")
    }

    private fun writeDataForUser(username: String, data: String) {
        DataStoreIo.write(("U$username"), data)
    }

    private fun writePasswordForUser(username: String, password: String) {
        DataStoreIo.write(("P$username$password"), passwordSignedIn)
    }

    private fun passwordValid(username: String, password: String): Boolean {
        return DataStoreIo.read(("P$username$password")) != null
    }

    private fun validToken(token: String): Boolean {
        when (readDataForUsername(tokenToUsername(token))) {
            notRegistered, registeredNotLoggedIn -> return false
            userLoggedIn -> return true
        }
        throw IllegalArgumentException()    //shouldn't get here, data-store might be corrupted.
    }

    private fun numOfUsers(): Int{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Returning number of users in the system, this number can only be increased." +
                    "In addition, this function is called only once... think again about it")
    }

    private fun setAdministrator(username: String){
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Setting user as administrator," +
                "this is a private function so we have assumption about the user")

        TODO("Write the assumption above, as documentation")
    }

    private fun isAdministrator(token: String): Boolean{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Boolean checking for if the user is administrator," +
                "this is a private function so we have assumption about the user")

        TODO("Write the assumption above, as documentation")
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

    private fun updateAssocChannels(username: String, kind: String){
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Updating all the channels that the user is member at," +
                "the update is logged in/logged out(kind param)" +
                "Updating the channles trees for Top10 functions(stats)...")

        TODO("Write the assumption above, as documentation")
    }

    private fun validNameChannel(channel: String): Boolean{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Checking if the channel name is valid")

        TODO("Write the assumption above, as documentation")
    }

    private fun isChannelExist(channel: String): Boolean{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("checking if the channel is exist in the system, the checking is byb his name...")

        TODO("Write the assumption above, as documentation")
    }

    private fun createChannel(channel: String, token: String) { TODO("Refactor: change token to username....")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Creating new channel in the system, inserting it to the trees" +
                "in addition adding the user to the channel , updating the user's channel list & making him/her an operator")

        TODO("Write the assumption above, as documentation")
    }

    private fun isUserInChannelAux(username: String, channel: String): Boolean{     TODO("Refactor: change to better name...")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Checking if the user is member of this channel")

        TODO("Write the assumption above, as documentation")
    }

    private fun removeUserFromChannel(channel: String, token: String){
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Removing the user from the channel, updating the user's channel list" +
                "where to check about the last user??...." +
                "update the channel trees")

        TODO("Write the assumption above, as documentation")
    }

    private fun isOperator(token: String, channel: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Checking if the user is operator at the channel")

        TODO("Write the assumption above, as documentation")
    }

    private fun numberOfActiveUsersInChannel(channel: String): Long{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Returning number of active users in the channel")

        TODO("Write the assumption above, as documentation")
    }

    private fun numberOfUsersInChannel(channel: String): Long{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        TODO("Returning number of users in the channel(members)")

        TODO("Write the assumption above, as documentation")
    }
}