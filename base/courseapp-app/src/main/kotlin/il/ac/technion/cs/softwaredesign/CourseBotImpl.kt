package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap


class CourseBotImpl : CourseBot {


    constructor(name: String, token: String, courseApp : CourseApp, messageFac : MessageFactory) {
        cApp = courseApp
        messageFactory = messageFac
        botName = name
        botToken = token

    }

    private val charset = Charsets.UTF_8

    private val baseTip: Long = 1000

    private var botName: String
    private var botToken: String
    /*
        private var calculationCallback: ListenerCallback?
        private var tipCallback: ListenerCallback?
    */


    companion object{
        //maps from name to list of channels:
        private val channels: HashMap<String,MutableList<String>> = HashMap()                   //storage
        private val counters: HashMap<String,HashMap<String, HashMap<Pair<String?, MediaType?>,Long>>> = HashMap()     //storage
        private val triggerPhrase: HashMap<String, String> = HashMap()  //on storage
        private val calculationCallbacks = HashMap<String,ListenerCallback>()   //local
        /*
        //global survey id counter:
        private var surveyIdGen = 0.toLong()    //on storage.

        //maps from name to phrases:

        private var tipTriggerPhrase: HashMap<String,String> = HashMap()    //on storage



        private val counterCallbacks: HashMap<Pair<String?, MediaType?>, ListenerCallback> =  HashMap() //local

        //need to save the answers too because of comparing inside the lambda.
        private val surveys: HashMap<String, Pair<ListenerCallback, List<Long>>> = HashMap()    //channel -> pair(callback, list of answers)

        //maps for every bot and specific channel to the relevant information:
        private val seenLastTimeService: HashMap<String,HashMap<String, HashMap<String, LocalDateTime?>>> = HashMap()   //botName -> (channel -> (user, time))
        private val mostActiveService: HashMap<String,HashMap<String, HashMap<String, Long>>> = HashMap()       // botName -> (channel ->(user, number of messages sent by user))
        private val tippingService: HashMap<String,HashMap<String, HashMap<String, Long>>> = HashMap()  // botName -> (channel -> (user, current tip))
        private val surveyCallbacks: HashMap<String,HashMap<String, MutableList<ListenerCallback>>> = HashMap()   // botName -> (channel -> list of callbacks)
        */
    }

    private var cApp : CourseApp
    private val messageFactory : MessageFactory

    /**
     * Make the bot join the channel, courseBots is managing the login of bot.
     *
     * @throws UserNotAuthorizedException If the channel can't be joined.
     */
    override fun join(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelJoin(botToken, channelName)
        } catch (e : Exception) {
            throw UserNotAuthorizedException()
        }

        addToChannelsList(channelName)
        return CompletableFuture.completedFuture(Unit)



        /*
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
        */

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
            cApp.channelPart(botToken, channelName)
        } catch (e : Exception) {
            throw NoSuchEntityException()
        }
        resetCounterForAllChannelsOfBot(channelToReset = channelName)   //TODO("check")
        removeFromChannelsList(channelName)
        return CompletableFuture.completedFuture(Unit)

        /*
        resetBotIn(channelName)
        return CompletableFuture.completedFuture(Unit)
        */
    }

    /**
     * Return a list of all the channels the bot is in, in order of joining.
     */
    override fun channels(): CompletableFuture<List<String>> {
        return getChannels()
    }

    private fun getChannels(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(channels[botName]?: mutableListOf())
    }

    /**
     * Start counting messages that match [channel], the regular-expression [regex] (see [kotlin.text.Regex]) and
     * [mediaType].
     *
     * If [channel], [regex] and [mediaType] are already registered, restart the count from 0.
     *
     * If [channel], [regex], or [mediaType] are null, they are treated as wildcards.
     *
     * @throws IllegalArgumentException If [regex] and [mediaType] are both null.
     */
    override fun beginCount(channel: String?, regex: String?, mediaType: MediaType?): CompletableFuture<Unit> {
        if (regex == null && mediaType == null)
            throw IllegalArgumentException()

        if(counters[botName] == null)
            counters[botName] = hashMapOf()

        val pair = Pair(regex, mediaType)
        if(channel == null)
            initiateCounterForPair(p = pair)
        else
        {
            initiateCounterForPair(p = pair, channelToReset = channel)
        }
        val callback: ListenerCallback = getCounterCallback(pair,channel)

        cApp.addListener(botToken, callback)

        return CompletableFuture.completedFuture(Unit)

    }

    private fun initiateCounterForPair(p: Pair<String?,MediaType?>? = null, channelToReset : String? = null){
        assert(p != null)
        if(channelToReset == null){
            val channelOfBot = channels().join()
            for (channel in channelOfBot) {
                initiateSinglePairInChannel(channel, p)
            }
        }else{
            initiateSinglePairInChannel(channelToReset, p)
        }
    }

    private fun initiateSinglePairInChannel(channelToReset: String, p: Pair<String?, MediaType?>?) {
        if (counters[botName] == null) {
            counters[botName] = hashMapOf()
        }
        if (counters[botName]!![channelToReset] == null)
            counters[botName]!![channelToReset] = hashMapOf()
        counters[botName]!![channelToReset]!![p!!] = 0.toLong()
    }

    /*
        resets the counter for all the channels of bot with pair,
        if pair is null: resets all channels and all pairs.
     */
    private fun resetCounterForAllChannelsOfBot(p: Pair<String?,MediaType?>? = null, channelToReset : String? = null) {
        if(counters[botName] == null) return

        val channelOfBot = channels().join()
        if(p == null) {
            if(channelToReset == null) {
                for (channel in channelOfBot) {
                    counters[botName]!![channel] = hashMapOf()
                }
            } else {
                counters[botName]!![channelToReset] = hashMapOf()
            }
        }else{
            if(channelToReset == null) {
                for (channel in channelOfBot) {
                    counters[botName]!![channel]?.remove(p)

                }
            }else{
                counters[botName]!![channelToReset]?.remove(p)
            }
        }
    }

    /**
     * Return the number of times that a message that matches [regex] and [mediaType] has been seen by this bot in
     * [channel], or in all channels if [channel] is null.
     *
     * @throws IllegalArgumentException if [regex] and [mediaType] have never been registered (passed to [beginCount]).
     */
    override fun count(channel: String?, regex: String?, mediaType: MediaType?): CompletableFuture<Long> {
        val pair = Pair(regex, mediaType)
        if(counters[botName] == null )
            throw IllegalArgumentException()
        if(channel != null){
            if(counters[botName]!![channel]!![pair] == null)
                throw IllegalArgumentException()
            val res = counters[botName]!![channel]!![pair]!!
            return CompletableFuture.completedFuture(res)
        }
        //channel is null, need to return the sum of all of calls in all channels
        val channelOfBot = channels().join()
        var sum = 0.toLong()
        var flagSeen = false
        for (channel in  channelOfBot ) {
            if(counters[botName]!![channel] == null) continue
            val num = counters[botName]!![channel]!![pair]
            if(num != null) {
                sum += num
                flagSeen = true
            }
        }
        if(!flagSeen) throw IllegalArgumentException()
        return CompletableFuture.completedFuture(sum)
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
        val previousPhrase = triggerPhrase[botName]
        if(trigger == null ){            //previously was activated, and need to be deactivated.
            if(previousPhrase!= null)
                cApp.removeListener(botToken, calculationCallbacks[botName]!!)
            triggerPhrase.remove(botName)       //turn off by removing phrase for current bot.
            return CompletableFuture.completedFuture(previousPhrase)
        }

        triggerPhrase[botName] = trigger
        val calculationCallback = getCalculationCallback(trigger)
        cApp.addListener(botToken, calculationCallback)
        calculationCallbacks[botName] = calculationCallback

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
        //TODO("implement me")
        return CompletableFuture.completedFuture("hi")
        /*
        val previousPhrase = tipTriggerPhrase
        tipTriggerPhrase = trigger

        if (tipCallback != null)
            cApp.removeListener(botToken!!, tipCallback!!)

        if (trigger != null) {
            tipCallback = getTipCallback(trigger)
            cApp.addListener(botToken!!, tipCallback!!)
        }

        return CompletableFuture.completedFuture(previousPhrase)
        */
    }

    /**
     * Return the creation time of the last message sent by [user] in all channels that the bot is in.
     */
    override fun seenTime(user: String): CompletableFuture<LocalDateTime?> {
        //TODO("implement me")
        return CompletableFuture.completedFuture(LocalDateTime.now())
        /*
        var lastMessageTime: LocalDateTime? = null
        for (channel in channels[user]?: mutableListOf()) {
            if (cApp.isUserInChannel(botToken, channel, user).get() == true) {
                val time = getTimeForLastMessageOf(user, channel)
                if (time!! > lastMessageTime) {
                    lastMessageTime = time
                }
            }
        }

        return CompletableFuture.completedFuture(lastMessageTime)
        */
    }

    /**
     * Return the name of the user that has sent the most messages to [channel], or null if we haven't seen any messages.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun mostActiveUser(channel: String): CompletableFuture<String?> {
        //TODO("implement me")
        return CompletableFuture.completedFuture("hi")
        /*
        val checker = cApp.isUserInChannel(botToken!!, channel, botName!!).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getMostActiveUser(channel))
        */
    }

    /**
     * Return the name of the richest user in [channel]'s tip system, or null if no tipping has occured or no one user is
     * the richest.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun richestUser(channel: String): CompletableFuture<String?> {
        //TODO("implement me")
        return CompletableFuture.completedFuture("hi")
        /*
        var checker : Boolean?
        try {
            checker = cApp.isUserInChannel(botToken, channel, botName).get()
        }catch (e : Exception) {
            throw NoSuchEntityException()
        }
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getRichestUser(channel))
        */
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
        //TODO("implement me")
        return CompletableFuture.completedFuture("hi")
        /*
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
        */
    }

    /**
     * Return the results of the survey.
     *
     * @throws NoSuchEntityException If [identifier] does not refer to a known survey.
     * @param identifier A survey identifier that was returned from [runSurvey].
     * @return A list of counters, one for each possible survery answer, in the order the answers appeared in [runSurvey].
     */
    override fun surveyResults(identifier: String): CompletableFuture<List<Long>> {
        //TODO("implement me")
        return CompletableFuture.completedFuture(listOf())
        /*
        if (surveys[identifier] == null)
            throw NoSuchEntityException()

        val (callback, results) = surveys[identifier]!!
        cApp.removeListener(botToken!!, callback)

        return CompletableFuture.completedFuture(results)
        */
    }

    private fun getCounterCallback(p : Pair<String?, MediaType?>, targetChannel: String?): ListenerCallback {
        return { source, message ->
            if ((p.second == null && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset))) ||
                    (p.second == message.media && p.first == null) ||
                    (p.second == message.media && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset)))) {
                val channelName = source.split('@')[0]
                if(counters[botName] != null && counters[botName]!![channelName] != null &&
                        (targetChannel == null || targetChannel == channelName)) {
                    val previousCounter= counters[botName]!![channelName]!![p] ?: 0.toLong()
                    counters[botName]!![channelName]!![p] = previousCounter + 1
                }
            }
            CompletableFuture.completedFuture(Unit)
        }
    }

    private fun getCalculationCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(charset)
            val channelName = source.split("@")[0]
            if (channels[botName]!!.contains(channelName) && content.startsWith(trigger)) {
                val expression  = content.removePrefix("$trigger ")
                val result = evaluateExpresion(expression).toString()

                val retMessage = messageFactory.create(MediaType.TEXT, result.toByteArray(charset)).get()
                cApp.channelSend(botToken, channelName, retMessage)

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getTipCallback(trigger: String): ListenerCallback {
        //TODO("implement me")
        return {_, _ -> CompletableFuture.completedFuture(Unit) }
        /*
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
        */
    }

    private fun getSurveyCallback(channel: String, answers: List<String>, resultsList: MutableList<Long>): ListenerCallback {
        //TODO("implement me")
        return {_, _ -> CompletableFuture.completedFuture(Unit) }
        /*
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
        */
    }

    private fun getLastSeenCallback(channel: String): ListenerCallback {
        //TODO("implement me")
        return {_, _ -> CompletableFuture.completedFuture(Unit) }
        /*
        return  { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                seenLastTimeService[channel]!![user] = LocalDateTime.now()
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
        */
    }

    private fun getMostActiveUserCallback(channel: String): ListenerCallback {
        //TODO("implement me")
        return {_, _ -> CompletableFuture.completedFuture(Unit) }
        /*
        return  { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                val counter = mostActiveService[botName]!![channel]!![user]
                if (counter == null)
                    mostActiveService[botName][channel]!![user] = 1.toLong()
                else
                    mostActiveService[botName][channel]!![user] = counter + 1.toLong()
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
        */
    }

    private fun evaluateExpresion(expression: String): Double {

        val newExpresion = expression.replace(" ","")
                .replace(Regex(".(?!$)"), "$0 ")
                .split(" ")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()                     // matan: can be spaces in the exp...

        val outputRPN = ExpressionParser.infixToRPN(newExpresion)
        return ExpressionParser.RPNtoDouble(outputRPN)
    }
/*
    private fun getAnswerNumber(answers: List<String>, message: Message): Int {
        for (answer in answers) {
            if (message.contents.toString(charset) == answer)
                return answers.indexOf(answer)
        }
        assert(false)
        return -1

    }
*/
    private fun getTimeForLastMessageOf(user: String, channel: String): LocalDateTime? {
        return null
        //TODO("implement me")
        /*
        return seenLastTimeService[channel]!![user]
        */
    }

    private fun getSurveyId(): String {
        //TODO("implement me")
        return "hi"
        /*
        val retId = surveyIdGen.toString()
        surveyIdGen ++
        return retId
        */
    }

    private fun addToChannelsList(channelName: String) {
        if(channels[botName] == null)       //if list not exist yet, make a new list
            channels[botName] = mutableListOf()
        if(channels[botName]!!.contains(channelName))
            return
        channels[botName]!!.add(channelName)

    }

    private fun removeFromChannelsList(channelName: String) {
        if(channels[botName] == null || !channels[botName]!!.contains(channelName))
            throw Exception()
        channels[botName]!!.remove(channelName)
    }

    private fun transferTip(channel: String, fromUser: String, toUser: String, number: Long) {
        //TODO("implement me")
        /*
        if (tippingService[channel]!![fromUser] == null)
            tippingService[channel]!![fromUser] = getBaseTip()

        if (tippingService[channel]!![toUser] == null)
            tippingService[channel]!![toUser] = getBaseTip()

        tippingService[channel]!![fromUser] = tippingService[channel]!![fromUser]!! - number
        tippingService[channel]!![toUser] = tippingService[channel]!![toUser]!! + number
        */
    }

    private fun sendSurveyQuestion(channel: String, question: String) {
        //TODO("implement me")
        /*
        messageFactory.create(MediaType.TEXT, question.toByteArray(charset)).thenCompose { cApp.channelSend(botToken!!, channel, it) }.get()
        */
    }

    private fun getBaseTip(): Long {
        //TODO("implement me")
        return 10
        /*
        return baseTip
        */
    }

    private fun getMostActiveUser(channel: String): String? {
        //TODO("implement me")
        return null
        /*
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
        */
    }

    private fun getRichestUser(channel: String): String? {
        //TODO("implement me")
        return null
        /*
        var richestUser: String? = null
        var richestAmount: Long = 0
        if(null == tippingService[channel]) return null
        tippingService[channel]!!.asSequence().forEach {
            if (richestUser == null || richestAmount < it.value) {
                richestUser = it.key
                richestAmount = it.value
            } else if (richestAmount == it.value) {
                return null
            }
        }
        return richestUser
        */

        // TODO ("what if there is some user that didn't transfer money")
    }

    private fun addMapSurvey(channel: String, callback: ListenerCallback) {
        //TODO("implement me")
        /*
        surveyCallbacks[channel]!!.add(callback)
        */
    }

    private fun resetBotIn(channel: String) {
        //TODO("implement me")
        /*
        // reset surveys
        surveys.asSequence().filter {
            !surveyCallbacks[channel]!!.contains(it.value.first)
        }.toList()          // TODO ("fix it")

        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        */
    }
}