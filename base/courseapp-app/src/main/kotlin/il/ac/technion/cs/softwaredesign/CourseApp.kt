package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.Message
import java.util.concurrent.CompletableFuture

typealias ListenerCallback = (source: String, message: Message) -> CompletableFuture<Unit>

/**
 * This is the interface implementing CourseApp, a course discussion group system.
 *
 * You may assume that [CourseAppInitializer.setup] was called before this class was instantiated.
 *
 * Currently specified:
 * + User authentication.
 * + Channels.
 * + Messages: In channels, broadcasts, and private messages.
 */
interface CourseApp {
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
    fun login(username: String, password: String): CompletableFuture<String>

    /**
     * Log out the user with this authentication [token]. The [token] will be invalidated and can not be used for future
     * calls.
     *
     * This is a *delete* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     */
    fun logout(token: String): CompletableFuture<Unit>

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
    fun isUserLoggedIn(token: String, username: String): CompletableFuture<Boolean?>


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
    fun makeAdministrator(token: String, username: String): CompletableFuture<Unit>

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
    fun channelJoin(token: String, channel: String): CompletableFuture<Unit>

    /**
     * The user identified by [token] will exit [channel].
     *
     * If the last user leaves a channel, the channel will be destroyed and its name will be available for re-use. The
     * first user to join the channel becomes an operator.
     *
     * This is a *delete* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [token] identifies a user who is not a member of [channel], or [channel] does
     * does exist.
     */
    fun channelPart(token: String, channel: String): CompletableFuture<Unit>

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
    fun channelMakeOperator(token: String, channel: String, username: String): CompletableFuture<Unit>

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
    fun channelKick(token: String, channel: String, username: String): CompletableFuture<Unit>

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
    fun isUserInChannel(token: String, channel: String, username: String): CompletableFuture<Boolean?>

    /**
     * Gets the number of logged-in users in a given [channel].
     *
     * Administrators can query any channel, while regular users can only query channels that they are members of.
     *
     * This is a *read* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] identifies a user who is not an administrator and is not a member
     * of [channel].
     * @returns Number of logged-in users in [channel].
     */
    fun numberOfActiveUsersInChannel(token: String, channel: String): CompletableFuture<Long>

    /**
     * Gets the number of users in a given [channel].
     *
     * Administrators can query any channel, while regular users can only query channels that they are members of.
     *
     * This is a *read* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [channel] does not exist.
     * @throws UserNotAuthorizedException If [token] identifies a user who is not an administrator and is not a member
     * of [channel].
     * @return Number of users, both logged-in and logged-out, in [channel].
     */
    fun numberOfTotalUsersInChannel(token: String, channel: String): CompletableFuture<Long>

    /**
     * Adds a listener to this Course App instance. See Observer design pattern for more information.
     *
     * See the assignment PDF for semantics.
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException if the auth [token] is invalid.
     */
    fun addListener(token: String, callback: ListenerCallback): CompletableFuture<Unit>

    /**
     * Remove a listener from this Course App instance. See Observer design pattern for more information.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws NoSuchEntityException If [callback] is not registered with this instance.
     */
    fun removeListener(token: String, callback: ListenerCallback): CompletableFuture<Unit>

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
    fun channelSend(token: String, channel: String, message: Message): CompletableFuture<Unit>

    /**
     * Sends a message to all users from an admin identified by [token]. Listeners will be notified, source is
     * "BROADCAST".
     *
     * This is an *update* command.
     *
     * @throws InvalidTokenException If the auth [token] is invalid.
     * @throws UserNotAuthorizedException If [token] does not identify an administrator.
     */
    fun broadcast(token: String, message: Message): CompletableFuture<Unit>

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
    fun privateSend(token: String, user: String, message: Message): CompletableFuture<Unit>

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
    fun fetchMessage(token: String, id: Long): CompletableFuture<Pair<String, Message>>
}
