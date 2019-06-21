package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.exceptions.*
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

interface CourseBot {


  /**
   * Make the bot join the channel.
   *
   * @throws UserNotAuthorizedException If the channel can't be joined.
   */
  fun join(channelName: String): CompletableFuture<Unit>

  /**
   * Make the bot leave the channel.
   *
   * Leaving the channel resets all statistics for that channel.
   *
   * @throws NoSuchEntityException If the channel can't be parted.
   */
  fun part(channelName: String): CompletableFuture<Unit>

  /**
   * Return a list of all the channels the bot is in, in order of joining.
   */
  fun channels(): CompletableFuture<List<String>>

  /**
   * Start counting messages that match the regular-expression [regex] (see [kotlin.text.Regex]) and [mediaType].
   *
   * If [regex] and [mediaType] are already registered, restart the count from 0.
   *
   * @throws IllegalArgumentException If [regex] and [mediaType] are both null.
   */
  fun beginCount(regex: String? = null, mediaType: MediaType? = null): CompletableFuture<Unit>

  /**
   * Return the number of times that a message that matches [regex] and [mediaType] has been seen by this bot in
   * [channel], or in all channels if [channel] is null.
   *
   * @throws IllegalArgumentException if [regex] and [mediaType] have never been registered (passed to [beginCount]).
   */
  fun count(channel: String?, regex: String? = null, mediaType: MediaType? = null): CompletableFuture<Long>

  /**
   * Set the phrase to trigger calculations.
   *
   * If this is set to a non-null value, then after seeing messages in the format "$trigger $expression" in a channel,
   * the bot will evaluate the expression (integers only, 4 arithmetic operators and parenthesis) and send a message
   * with the value to the same channel.
   *
   * @param trigger The phrase, or null to disable calculator mode.
   * @return The previous phrase.
   */
  fun setCalculationTrigger(trigger: String?): CompletableFuture<String?>

  /**
   * Set the phrase to trigger tipping.
   *
   * If this is set to a non-null value, then after seeing messages in the format "$trigger $number $user" in a
   * channel, the bot will transfer $number bits from the user who sent the message to $user.
   *
   * @param trigger The phrase, or null to disable tipping mode.
   * @return The previous phrase.
   */
  fun setTipTrigger(trigger: String?): CompletableFuture<String?>

  /**
   * Return the creation time of the last message sent by [user] in all channels that the bot is in.
   */
  fun seenTime(user: String): CompletableFuture<LocalDateTime?>


  /**
   * Return the name of the user that has sent the most messages to [channel], or null if we haven't seen any messages.
   *
   * @throws NoSuchEntityException If the bot is not in [channel].
   */
  fun mostActiveUser(channel: String): CompletableFuture<String?>


  /**
   * Return the name of the richest user in [channel]'s tip system, or null if no tipping has occured or no one user is
   * the richest.
   *
   * @throws NoSuchEntityException If the bot is not in [channel].
   */
  fun richestUser(channel: String): CompletableFuture<String?>

  /**
   * Start a survey by sending [question] to [channel], and looking for the literal string in [answers] in future
   * messages to [channel], recording the survey results.
   *
   * @param question The survey question to sent to [channel]
   * @param answers List of phrases to recognize as survey answers
   * @throws NoSuchEntityException If the bot is not in [channel].
   * @return A string identifying this survey.
   */
  fun runSurvey(channel: String, question: String, answers: List<String>): CompletableFuture<String>

  /**
   * Return the results of the survey.
   *
   * @throws NoSuchEntityException If [identifier] does not refer to a known survey.
   * @param identifier A survey identifier that was returned from [runSurvey].
   * @return A list of counters, one for each possible survery answer, in the order the answers appeared in [runSurvey].
   */
  fun surveyResults(identifier: String): CompletableFuture<List<Long>>
}