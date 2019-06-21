package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap


class CourseBotImpl : CourseBot {

    constructor(name: String, token: String) {
        botName = name
        botToken = token

        // TODO ("get all the data from the remote storage for this bot... - if was some...")
        // TODO ("does it need to be persistent at reboots?!?!?.... if yes what to do with all the listeners?? they need to be inserted again?")
        // TODO ("add get() for all the API methods of CourseApp")
        // TODO ("change the charset to UTF8... find the current type in HW2")
        // TODO ("media type need to be text...")
    }

    private val charset = Charsets.UTF_8

    private val baseTip: Long = 1000

    private var botName: String? = null
    private var botToken: String? = null
    private val channels: MutableList<String>
    private var calculationCallback: ListenerCallback?
    private var tipCallback: ListenerCallback?
    private val counters: HashMap<Pair<String?, MediaType?>, HashMap<String, Long>>
    private val counterCallbacks: HashMap<Pair<String?, MediaType?>, ListenerCallback>
    private var triggerPhrase: String?
    private var tipTriggerPhrase: String?
    private val surveys: HashMap<String, Pair<ListenerCallback, List<Long>>>
    private var surveyIdGen = 0.toLong()
    private val seenLastTimeService: HashMap<String, HashMap<String, LocalDateTime?>>
    private val mostActiveService: HashMap<String, HashMap<String, Long>>
    private val tippingService: HashMap<String, HashMap<String, Long>>
    private val surveyCallbacks: HashMap<String, MutableList<ListenerCallback>>

    private val injector = Guice.createInjector(CourseAppModule(), CourseBotModule(), SecureStorageModule())

    init {
        channels = mutableListOf()
        counters = HashMap()
        counterCallbacks = HashMap()
        triggerPhrase = null
        tipTriggerPhrase = null
        calculationCallback = null
        tipCallback = null
        surveys = HashMap()
        seenLastTimeService = HashMap()
        mostActiveService = HashMap()
        tippingService = HashMap()
        surveyCallbacks = HashMap()

        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    private val cApp = injector.getInstance<CourseApp>()
    private val messageFactory = injector.getInstance<MessageFactory>()

    /**
     * Make the bot join the channel.
     *
     * @throws UserNotAuthorizedException If the channel can't be joined.
     */
    override fun join(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelJoin(botToken!!, channelName)
            addToChannelsList(channelName)
        } catch (e : NameFormatException) {
            throw UserNotAuthorizedException()

        } catch (e : UserNotAuthorizedException) {
            throw UserNotAuthorizedException()
        } catch (e : Exception) {
            assert(false)
        }

        // most active for new channel
        mostActiveService[channelName] = HashMap()
        val mostActiveCallback = getMostActiveUserCallback(channelName)
        cApp.addListener(botToken!!, mostActiveCallback)

        // last seen for new channel
        seenLastTimeService[channelName] = HashMap()
        val lastSeenCallback = getLastSeenCallback(channelName)
        cApp.addListener(botToken!!, lastSeenCallback)

        // survey callback list
        surveyCallbacks[channelName] = mutableListOf()

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Make the bot leave the channel.
     *
     * Leaving the channel resets all statistics for that channel.
     *
     * @throws NoSuchEntityException If the channel can't be parted.
     */
    override fun part(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelPart(botToken!!, channelName)
            removeFromChannelsList(channelName)
        } catch (e : NoSuchEntityException) {
            throw NoSuchEntityException()

        } catch (e : Exception) {
            assert(false)
        }

        resetBotIn(channelName)
        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Return a list of all the channels the bot is in, in order of joining.
     */
    override fun channels(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(channels)
    }

    /**
     * Start counting messages that match the regular-expression [regex] (see [kotlin.text.Regex]) and [mediaType].
     *
     * If [regex] and [mediaType] are already registered, restart the count from 0.
     *
     * @throws IllegalArgumentException If [regex] and [mediaType] are both null.
     */
    override fun beginCount(regex: String?, mediaType: MediaType?): CompletableFuture<Unit> {
        if (regex == null && mediaType == null)
            throw IllegalArgumentException()

        val callback: ListenerCallback = getCounterCallback(regex, mediaType)
        val pair = Pair(regex, mediaType)

        counters[pair] = hashMapOf()
        if (counters[pair] != null) {
            val previousCallback= counterCallbacks[pair]
            cApp.removeListener(botToken!!, previousCallback!!)
        }

        cApp.addListener(botToken!!, callback)
        counterCallbacks[pair] = callback

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Return the number of times that a message that matches [regex] and [mediaType] has been seen by this bot in
     * [channel], or in all channels if [channel] is null.
     *
     * @throws IllegalArgumentException if [regex] and [mediaType] have never been registered (passed to [beginCount]).
     */
    override fun count(channel: String?, regex: String?, mediaType: MediaType?): CompletableFuture<Long> {
        val pair = Pair(regex, mediaType)
        if (counters[pair] == null)
            throw IllegalArgumentException()

        val ret = if (channel == null) {
            counters[pair]!!.asSequence().fold(0.toLong()) { sum, curr -> sum + curr.value}.toLong()
        } else {
            counters[pair]!![channel] ?: 0.toLong()
        }

        return CompletableFuture.completedFuture(ret)
    }

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
    override fun setCalculationTrigger(trigger: String?): CompletableFuture<String?> {
        val previousPhrase = triggerPhrase
        triggerPhrase = trigger

        if (calculationCallback != null)
            cApp.removeListener(botToken!!, calculationCallback!!)

        if (trigger != null) {
            calculationCallback = getCalculationCallback(trigger)
            cApp.addListener(botToken!!, calculationCallback!!)
        }

        return CompletableFuture.completedFuture(previousPhrase)
    }

    /**
     * Set the phrase to trigger tipping.
     *
     * If this is set to a non-null value, then after seeing messages in the format "$trigger $number $user" in a
     * channel, the bot will transfer $number bits from the user who sent the message to $user.
     *
     * @param trigger The phrase, or null to disable tipping mode.
     * @return The previous phrase.
     */
    override fun setTipTrigger(trigger: String?): CompletableFuture<String?> {
        val previousPhrase = tipTriggerPhrase
        tipTriggerPhrase = trigger

        if (tipCallback != null)
            cApp.removeListener(botToken!!, tipCallback!!)

        if (trigger != null) {
            tipCallback = getTipCallback(trigger)
            cApp.addListener(botToken!!, tipCallback!!)
        }

        return CompletableFuture.completedFuture(previousPhrase)
    }

    /**
     * Return the creation time of the last message sent by [user] in all channels that the bot is in.
     */
    override fun seenTime(user: String): CompletableFuture<LocalDateTime?> {
        var lastMessageTime: LocalDateTime? = null
        for (channel in channels) {
            if (cApp.isUserInChannel(botToken!!, channel, user).get() == true) {
                val time = getTimeForLastMessageOf(user, channel)
                if (time!! > lastMessageTime) {
                    lastMessageTime = time
                }
            }
        }

        return CompletableFuture.completedFuture(lastMessageTime)
    }

    /**
     * Return the name of the user that has sent the most messages to [channel], or null if we haven't seen any messages.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun mostActiveUser(channel: String): CompletableFuture<String?> {
        val checker = cApp.isUserInChannel(botToken!!, channel, botName!!).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getMostActiveUser(channel))
    }

    /**
     * Return the name of the richest user in [channel]'s tip system, or null if no tipping has occured or no one user is
     * the richest.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun richestUser(channel: String): CompletableFuture<String?> {
        val checker = cApp.isUserInChannel(botToken!!, channel, botName!!).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getRichestUser(channel))
    }

    /**
     * Start a survey by sending [question] to [channel], and looking for the literal string in [answers] in future
     * messages to [channel], recording the survey results.
     *
     * @param question The survey question to sent to [channel]
     * @param answers List of phrases to recognize as survey answers
     * @throws NoSuchEntityException If the bot is not in [channel].
     * @return A string identifying this survey.
     */
    override fun runSurvey(channel: String, question: String, answers: List<String>): CompletableFuture<String> {
        val checker = cApp.isUserInChannel(botToken!!, channel, botName!!).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        val id = getSurveyId()
        val resultsList = mutableListOf<Long>()

        sendSurveyQuestion(channel, question)
        val callback = getSurveyCallback(channel, answers, resultsList)

        surveys[id] = Pair(callback, resultsList)
        cApp.addListener(botToken!!, callback)

        addMapSurvey(channel, callback)

        return CompletableFuture.completedFuture(id)
    }

    /**
     * Return the results of the survey.
     *
     * @throws NoSuchEntityException If [identifier] does not refer to a known survey.
     * @param identifier A survey identifier that was returned from [runSurvey].
     * @return A list of counters, one for each possible survery answer, in the order the answers appeared in [runSurvey].
     */
    override fun surveyResults(identifier: String): CompletableFuture<List<Long>> {
        if (surveys[identifier] == null)
            throw NoSuchEntityException()

        val (callback, results) = surveys[identifier]!!
        cApp.removeListener(botToken!!, callback)

        return CompletableFuture.completedFuture(results)
    }

    private fun getCounterCallback(regex: String?, mediaType: MediaType?): ListenerCallback {
        val pair = Pair(regex, mediaType)
        return { source, message ->
            if ((mediaType == null && regex != null && Regex(regex).containsMatchIn(message.contents.toString(charset))) ||
                    (mediaType == message.media && regex == null) ||
                    (mediaType == message.media && regex != null && Regex(regex).containsMatchIn(message.contents.toString(charset)))) {
                val channelName = source.split('@')[0]
                val previousCounter= counters[pair]!![channelName] ?: 0.toLong()
                counters[pair]!![channelName] = previousCounter + 1
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getCalculationCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(Charset.defaultCharset())
            val channelName = source.split("@")[0]
            if (channels.contains(channelName) && content.startsWith(trigger)) {
                val expresion  = content.removePrefix("$trigger ")
                val result = evaluateExpresion(expresion).toString()

                val retMessage = messageFactory.create(MediaType.TEXT, result.toByteArray(charset)).get()
                cApp.channelSend(botToken!!, channelName, retMessage)

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getTipCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(charset)
            val channelName = source.split("@")[0]
            val fromUser = source.split("@")[1]
            if (channels.contains(channelName) && content.startsWith(trigger)) {        // matan: assumption that the message for tipping only inside channels
                val number = content.split(" ")[1].toLong()
                val toUser = content.split(" ")[2]
                if (cApp.isUserInChannel(botToken!!, channelName, toUser).get() == true) {  // matan: assumption that the 'To' user need to be in the same channel
                    transferTip(channelName, fromUser, toUser, number)
                }

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getSurveyCallback(channel: String, answers: List<String>, resultsList: MutableList<Long>): ListenerCallback {
        return  { source, message ->
            if (source.split('@')[0] == channel) {
                val i = getAnswerNumber(answers, message)
                if (i != -1)
                    resultsList[i]++
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getLastSeenCallback(channel: String): ListenerCallback {
        return  { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                seenLastTimeService[channel]!![user] = LocalDateTime.now()
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getMostActiveUserCallback(channel: String): ListenerCallback {
        return  { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                val counter = mostActiveService[channel]!![user]
                if (counter == null)
                    mostActiveService[channel]!![user] = 1.toLong()
                else
                    mostActiveService[channel]!![user] = counter + 1.toLong()
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun evaluateExpresion(expresion: String): Double {
        val newExpresion = expresion.replace(" ","")
                .replace(Regex(".(?!$)"), "$0 ")
                .split(" ")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()                     // matan: can be spaces in the exp...

        val outputRPN = ExpressionParser.infixToRPN(newExpresion)
        return ExpressionParser.RPNtoDouble(outputRPN)
    }

    private fun getAnswerNumber(answers: List<String>, message: Message): Int {
        for (answer in answers) {
            if (message.contents.toString(charset) == answer)
                return answers.indexOf(answer)
        }
        assert(false)
        return -1
    }

    private fun getTimeForLastMessageOf(user: String, channel: String): LocalDateTime? {
        return seenLastTimeService[channel]!![user]
    }

    private fun getSurveyId(): String {
        val retId = surveyIdGen.toString()
        surveyIdGen ++
        return retId
    }

    private fun addToChannelsList(channelName: String) {
        channels.add(channelName)
    }

    private fun removeFromChannelsList(channelName: String) {
        channels.remove(channelName)
    }

    private fun transferTip(channel: String, fromUser: String, toUser: String, number: Long) {
        if (tippingService[channel]!![fromUser] == null)
            tippingService[channel]!![fromUser] = getBaseTip()

        if (tippingService[channel]!![toUser] == null)
            tippingService[channel]!![toUser] = getBaseTip()

        tippingService[channel]!![fromUser] = tippingService[channel]!![fromUser]!! - number
        tippingService[channel]!![toUser] = tippingService[channel]!![toUser]!! + number
    }

    private fun sendSurveyQuestion(channel: String, question: String) {
        messageFactory.create(MediaType.TEXT, question.toByteArray(charset)).thenCompose { cApp.channelSend(botToken!!, channel, it) }.get()
    }

    private fun getBaseTip(): Long {
        return baseTip
    }

    private fun getMostActiveUser(channel: String): String? {
        var mostActiveUser: String? = null
        var mostActiveCounter: Long = 0
        mostActiveService[channel]!!.asSequence().forEach {
            if (mostActiveUser == null || mostActiveCounter < it.value) {
                mostActiveUser = it.key
                mostActiveCounter = it.value
            } else if (mostActiveCounter == it.value) {
                return null
            }
        }
        return mostActiveUser
    }

    private fun getRichestUser(channel: String): String? {
        var richestUser: String? = null
        var richestAmount: Long = 0
        tippingService[channel]!!.asSequence().forEach {
            if (richestUser == null || richestAmount < it.value) {
                richestUser = it.key
                richestAmount = it.value
            } else if (richestAmount == it.value) {
                return null
            }
        }
        return richestUser

        // TODO ("what if there is some user that didn't transfer money")
    }

    private fun addMapSurvey(channel: String, callback: ListenerCallback) {
        surveyCallbacks[channel]!!.add(callback)
    }

    private fun resetBotIn(channel: String) {
        // reset surveys
        surveys.asSequence().filter {
            !surveyCallbacks[channel]!!.contains(it.value.first)
        }.toList()          // TODO ("fix it")

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}