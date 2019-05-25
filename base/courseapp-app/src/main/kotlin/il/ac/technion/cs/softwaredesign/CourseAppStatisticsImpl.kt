package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject

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
    override fun totalUsers(): Long {
        return cApp.getTotalUsers()
    }

    /**
     * Count the number of logged-in users in the system.
     *
     * @return The number of logged-in users.
     */
    override fun loggedInUsers(): Long {
        return cApp.getTotalActiveUsers()
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
        return cApp.getTop10User()
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
        return cApp.getTop10ChannelsByActiveUsers()
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
        return cApp.getTop10User()
    }
}