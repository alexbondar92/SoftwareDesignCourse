package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

interface CourseBots {
  /**
   * Get an instance of CourseBot for a bot named [name].
   *
   * If the bot did not previously exist, it will be created. The default name for a bot is "Anna$n", where n is the
   * number of bots that currently exist, e.g. Anna0.
   *
   * @param name The requested name for the bot, or a default name if null.
   */
  fun bot(name: String? = null): CompletableFuture<CourseBot>

  /**
   * List the names of bots that currently exist in the system. If [channel] is specified, only bots that are in the
   * named channel will be returned.
   *
   * @param channel Limit the query to bots that are in this channel, or all channels if null.
   * @return List of bot names in order of bot creation.
   */
  fun bots(channel: String? = null): CompletableFuture<List<String>>
}
