package il.ac.technion.cs.softwaredesign

/**
 * This is an interface specifying a statistics API for CourseApp. It allows performing aggregate queries over
 * CourseApp.
 */
interface CourseAppStatistics {
    /**
     * Count the total number of users, both logged-in and logged-out, in the system.
     *
     * @return The total number of users.
     */
    fun totalUsers(): Long

    /**
     * Count the number of logged-in users in the system.
     *
     * @return The number of logged-in users.
     */
    fun loggedInUsers(): Long

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
    fun top10ChannelsByUsers(): List<String>

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
    fun top10ActiveChannelsByUsers(): List<String>

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
    fun top10UsersByChannels(): List<String>
}