package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import java.util.concurrent.CompletableFuture

class CourseAppStatisticsImpl: CourseAppStatistics {

    private val storageIo: DataStoreIo
    private val cApp: CourseAppImpl

    @Inject constructor(storage: DataStoreIo) {
        storageIo = storage
        cApp = CourseAppImpl(storageIo)
    }

    /**
     * Count the total number of users, both logged-in and logged-out, in the system.
     *
     * @return The total number of users.
     */
    override fun totalUsers(): CompletableFuture<Long> {
        return CompletableFuture.completedFuture(cApp.getTotalUsers())
    }

    /**
     * Count the number of logged-in users in the system.
     *
     * @return The number of logged-in users.
     */
    override fun loggedInUsers(): CompletableFuture<Long> {
        return CompletableFuture.completedFuture(cApp.getTotalActiveUsers())
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
    override fun top10ChannelsByUsers(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(cApp.getTop10ChannelsByTotalUsers())
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
    override fun top10ActiveChannelsByUsers(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(cApp.getTop10ChannelsByActiveUsers())
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
    override fun top10UsersByChannels(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(cApp.getTop10User())
    }

    /**
     * Total number of pending messages, i.e. messages that are waiting for a user to read them, not including channel
     * messages.
     *
     * @return The number of pending messages.
     */
    override fun pendingMessages(): CompletableFuture<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Total number of channel messages, i.e., messages that can be fetched using [CourseApp.fetchMessage].
     *
     * @return The number of messages in channels.
     */
    override fun channelMessages(): CompletableFuture<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Return a sorted list of the top 10 channels in the system by message volume. The list will be sorted in
     * descending order.
     *
     * If two channels have the exact same number of messages, they will be sorted in ascending appearance order.
     *
     * If there are less than 10 channels in the system, a shorter list will be returned.
     *
     * @return A sorted list of channels by message count.
     */
    override fun top10ChannelsByMessages(): CompletableFuture<List<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}