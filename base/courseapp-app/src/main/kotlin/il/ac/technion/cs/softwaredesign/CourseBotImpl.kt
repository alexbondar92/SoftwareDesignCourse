package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap


class CourseBotImpl : CourseBot {


    constructor(name: String, token: String, courseApp: CourseApp, messageFac: MessageFactory,
                secStorageFac: SecureStorageFactory, dicFactoryForBotMapings: DictionaryFactory, listFactoryIn: LinkedListFactory ,channelsIn : Dictionary, countersIn: Dictionary,
                countersChannelListOfPairsIn: Dictionary, triggerPhraseIn : Dictionary, tipTriggerPhraseIn: Dictionary,
                tippingServiceIn:Dictionary, channelToUsersListsIn: Dictionary, seenLastTimeServiceIn: Dictionary,
                mostActiveServiceIn: Dictionary, surveysIn:Dictionary, surveysHistoryIn:Dictionary, idGeneratorIn: Dictionary,
                surveyIdListIn: Dictionary,
                tipCallbackIn: HashMap<String,ListenerCallback?>, calculationCallbackIn: HashMap<String,ListenerCallback?>,
                lastSeenCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>,
                mostActiveCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>,
                surveysCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>,
                countersCallbacksIn: HashMap<String,HashMap<Triple<String?, String?, MediaType?>, ListenerCallback>>
                ) {
        // TODO ("does this bot in the storage? if yes load his data else create new empty bot")
        // TODO ("sync with the library - to load or init one of them... depending if there was this bot before the reboot")

        cApp = courseApp
        messageFactory = messageFac
        botName = name
        botToken = token

        dicFactory = dicFactoryForBotMapings    //given by courseBots, so no more secure storags will open
        listFactory = listFactoryIn             //given by courseBots, so no more secure storags will open
        heapFactory = MaxHeapFactory(secStorageFac, "botHeapFac")

        // init maps for that bot - local data structures
        lastSeenCallbacks = HashMap()
        mostActiveCallbacks = HashMap()
        surveysCallbacks = HashMap()

        //initiate storage structures:  (given by courseBots - persistent)
        channels  = channelsIn
        counters = countersIn
        countersChannelListOfPairs = countersChannelListOfPairsIn
        triggerPhrase = triggerPhraseIn
        tipTriggerPhrase = tipTriggerPhraseIn
        tippingService = tippingServiceIn
        channelToUsersLists = channelToUsersListsIn
        seenLastTimeService= seenLastTimeServiceIn
        mostActiveService = mostActiveServiceIn
        surveys = surveysIn
        surveysHistory = surveysHistoryIn
        idGenerator = idGeneratorIn
        surveyIdList = surveyIdListIn

        //local From courseBots
        tipCallback = tipCallbackIn
        calculationCallback = calculationCallbackIn
        lastSeenCallbacks = lastSeenCallbacksIn
        mostActiveCallbacks = mostActiveCallbacksIn
        surveysCallbacks = surveysCallbacksIn
        countersCallbacks = countersCallbacksIn
        initializeLocalsIfNeeded()
    }
    //factories:
    private val dicFactory : DictionaryFactory
    private val listFactory : LinkedListFactory
    private val heapFactory : MaxHeapFactory  //not needed yet

    private val charset = Charsets.UTF_8

    private val baseTip: Long = 1000

    private var botName: String
    private var botToken: String

    private var cApp: CourseApp
    private val messageFactory: MessageFactory

    private var tipCallback: HashMap<String,ListenerCallback?>        //on creation need to put the real callBack
    private var calculationCallback: HashMap<String,ListenerCallback?>
    private var lastSeenCallbacks: HashMap<String,HashMap<String, ListenerCallback>>                                                 //local     // channel -> ListenerCallback
    private var mostActiveCallbacks: HashMap<String,HashMap<String, ListenerCallback>>                                                //local     // channel -> ListenerCallback
    private var surveysCallbacks: HashMap<String,HashMap<String, ListenerCallback>>                                                     //local     // surveyID -> ListenerCallback
    private var countersCallbacks: HashMap<String,HashMap<Triple<String?, String?, MediaType?>, ListenerCallback>> = HashMap()          //local     // Triple(channel, Regex, MediaType) -> ListenerCallback

    //storage:
    private var channels : Dictionary                       //HashMap<String, MutableList<String>>
    private val counters: Dictionary                        //HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private val countersChannelListOfPairs : Dictionary     // botName -> channel -> list of pairs //TODO("change to triple")
    private val triggerPhrase: Dictionary                   // botName -> (Calculation Trigger)String
    private var tipTriggerPhrase: Dictionary                //HashMap<String, String?>
    private val tippingService: Dictionary                  //HashMap<String, HashMap<String, HashMap<String, Long>>>
    private val channelToUsersLists: Dictionary             // channelName -> users List    //not specific to botName
    private val seenLastTimeService: Dictionary             //HashMap<String, HashMap<String, HashMap<String, LocalDateTime?>>>
    private val mostActiveService: Dictionary               // botName -> (channel ->(user, number of messages sent by user))
    private val surveys: Dictionary                         // surveyID -> (heap of idAnswer -> counter)
    private val surveysHistory: Dictionary                  // surveyID -> heap of voter(userName) to answerID
    private val surveyIdList: Dictionary                    // botName -> list of survey ids
    //global survey id counter:
    private var idGenerator:Dictionary      //for key : "num" have the number as a data, if not created just return null when reading for that key.
    //end storage


    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private fun initializeLocalsIfNeeded(){
        if(!tipCallback.contains(botName))
            tipCallback[botName] = null
        if(!calculationCallback.contains(botName))
            calculationCallback[botName] = null
        if(!lastSeenCallbacks.contains(botName))
        lastSeenCallbacks[botName] = hashMapOf()
        if(!mostActiveCallbacks.contains(botName))
        mostActiveCallbacks[botName] = hashMapOf()
        if(!surveysCallbacks.contains(botName))
        surveysCallbacks[botName] = hashMapOf()
        if(!countersCallbacks.contains(botName))
        countersCallbacks[botName] = hashMapOf()
    }
    /**
     * Make the bot join the channel, courseBots is managing the login of bot.
     *
     * @throws UserNotAuthorizedException If the channel can't be joined.
     */
    override fun join(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelJoin(botToken, channelName).get()
        } catch (e: Exception) {
            throw UserNotAuthorizedException()
        }

        addToChannelsList(channelName)

        // tipping service for new channel
        initTippingService(channelName)

        // last seen for new channel
        initSeenLastTimeIn(channelName)
        val lastSeenCallback = getLastSeenCallback(channelName)
        lastSeenCallbacks[botName]!![channelName] = lastSeenCallback
        cApp.addListener(botToken, lastSeenCallback).get()

        // most active for new channel
        initMostActiveService(channelName)
        val mostActiveCallback = getMostActiveUserCallback(channelName)
        mostActiveCallbacks[botName]!![channelName] = mostActiveCallback
        cApp.addListener(botToken, mostActiveCallback).get()

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
            cApp.channelPart(botToken, channelName).get()
        } catch (e: Exception) {
            throw NoSuchEntityException()
        }
        resetCounterForAllChannelsOfBot(channelToReset = channelName)
        removeFromChannelsList(channelName)
        return CompletableFuture.completedFuture(Unit)

        // TODO ("need to remove all the callbacks from this channel before part - assumption for courseApp")
        /*
        resetBotIn(channelName)
        return CompletableFuture.completedFuture(Unit)
        */
    }

    /**
     * Return a list of all the channels the bot is in, in order of joining.
     */
    override fun channels(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(channelsList())
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

        if (!isCountersExists())
            initCountersIfNeeded()

        val pair = Pair(regex, mediaType)
        if (channel == null)
            initiateCounterForPair(p = pair)
        else {
            initiateCounterForPair(p = pair, channelToReset = channel)
        }
        val callback: ListenerCallback = getCounterCallback(pair, channel)

        countersCallbacks[botName]!![Triple(channel, regex, mediaType)] = callback
        cApp.addListener(botToken, callback).get()

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
       // if (isCounterExist() == null)
            //throw IllegalArgumentException()
        if (channel != null) {
            if (getCounterOf(channel, pair) == null)
                throw IllegalArgumentException()
            val res = getCounterOf(channel, pair)!!
            return CompletableFuture.completedFuture(res)
        }
        //channel is null, need to return the sum of all of calls in all channels
        val channelOfBot = channels().join()
        var sum = 0.toLong()
        var flagSeen = false
        for (currChannel in channelOfBot) {
            if (!isCountersOfExists(currChannel)) continue
            val num = getCounterOf(currChannel, pair)
            if (num != null) {
                sum += num
                flagSeen = true
            }
        }
        if (!flagSeen) throw IllegalArgumentException()
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
        val previousPhrase = getTriggerPhrase()
        if (trigger == null) {            //previously was activated, and need to be deactivated.
            if (previousPhrase != null)
                cApp.removeListener(botToken, calculationCallback[botName]!!).get()
            setTriggerPhrase(null)
            return CompletableFuture.completedFuture(previousPhrase)
        }

        setTriggerPhrase(trigger)
        val callback = getCalculationCallback(trigger)
        cApp.addListener(botToken, callback).get()
        calculationCallback[botName] = callback

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
        val previousPhrase = getTipTriggerPhrase()
        setTipTriggerPhrase(trigger)

        if (tipCallback[botName] != null)
            cApp.removeListener(botToken, tipCallback[botName]!!).get()

        if (trigger != null) {
            tipCallback[botName] = getTipCallback(trigger)
            cApp.addListener(botToken, tipCallback[botName]!!).get()
        }

        return CompletableFuture.completedFuture(previousPhrase)
    }

    /**
     * Return the creation time of the last message sent by [user] in all channels that the bot is in.
     */
    override fun seenTime(user: String): CompletableFuture<LocalDateTime?> {
        var lastMessageTime: LocalDateTime? = null
        for (channel in channelsList()) {
            if (cApp.isUserInChannel(botToken, channel, user).get() == true) {
                val time = getSeenLastTimeFor(channel, user) ?: continue
                if (lastMessageTime == null || time > lastMessageTime) {
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
        val checker : Boolean?
        try {
            checker = cApp.isUserInChannel(botToken, channel, botName).get()
        } catch (e : Exception) {
            throw NoSuchEntityException()
        }
        if (checker == null || checker == false) {
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
        val checker: Boolean?
        try {
            checker = cApp.isUserInChannel(botToken, channel, botName).get()
        } catch (e: Exception) {
            throw NoSuchEntityException()
        }
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
        val checker: Boolean?
        try {
            checker = cApp.isUserInChannel(botToken, channel, botName).get()
        } catch (e: Exception) {
            throw NoSuchEntityException()
        }

        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        val surveyId = getSurveyId()
        addToSurveyList(surveyId)

        sendSurveyQuestion(channel, question)
        val callback = getSurveyCallback(channel, answers, surveyId)

        surveysCallbacks[botName]!![surveyId] = callback
        cApp.addListener(botToken, callback).get()

        return CompletableFuture.completedFuture(surveyId)
    }

    private fun addToSurveyList(surveyId: String) {
        var listId = surveyIdList.read(botName)
        if(listId == null){
            listId = listFactory.get().getId()
            surveyIdList.write(botName, listId)
        }
        val list = listFactory.restoreLinkedList(listId)
        if(!list.contains(surveyId)){
            list.add(surveyId)
        }
    }

    /**
     * Return the results of the survey.
     *
     * @throws NoSuchEntityException If [identifier] does not refer to a known survey.
     * @param identifier A survey identifier that was returned from [runSurvey].
     * @return A list of counters, one for each possible survey answer, in the order the answers appeared in [runSurvey].
     */
    override fun surveyResults(identifier: String): CompletableFuture<List<Long>> {
        if (!isSurveysExists(identifier))
            throw NoSuchEntityException()
        val resList = getSurveys(identifier)
        return CompletableFuture.completedFuture(resList)
    }

    private fun getCounterCallback(p: Pair<String?, MediaType?>, targetChannel: String?): ListenerCallback {
        return { source, message ->
            if ((p.second == null && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset))) ||
                    (p.second == message.media && p.first == null) ||
                    (p.second == message.media && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset)))) {
                val channelName = source.split('@')[0]
                if (isCountersExists() && isCountersOfExists(channelName) && (targetChannel == null || targetChannel == channelName)) {
                    incCounterOf(channelName, p)
                }
            }
            CompletableFuture.completedFuture(Unit)
        }
    }

    private fun getCalculationCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(charset)
            val channelName = source.split("@")[0]
            if (channelsList().contains(channelName) && content.startsWith(trigger)) {
                val expression = content.removePrefix("$trigger ")
                val result = evaluateExpresion(expression).toString()

                val retMessage = messageFactory.create(MediaType.TEXT, result.toByteArray(charset)).get()
                cApp.channelSend(botToken, channelName, retMessage).get()

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
            if (channelsList().contains(channelName) && content.startsWith(trigger)) {        // matan: assumption that the message for tipping only inside channels
                val number = content.split(" ")[1].toLong()
                val toUser = content.split(" ")[2]
                if (cApp.isUserInChannel(botToken, channelName, toUser).get() == true) {  // matan: assumption that the 'To' user need to be in the same channel
                    transferTip(channelName, fromUser, toUser, number)
                }

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getSurveyCallback(channel: String, answers: List<String>, surveyId: String): ListenerCallback {
        initSurveys(surveyId, answers.size)
        initSurveysHistory(surveyId)

        return { source, message ->
            val inChannel = source.split('@')[0]
            val voter = source.split('@')[1]

            if (voter != botName && inChannel == channel) {
                val i = getAnswerNumber(answers, message)
                if (i != -1) {
                    setVoteIdForVoter(surveyId, voter, i)
                }

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getLastSeenCallback(channel: String): ListenerCallback {
        return { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                setSeenLastTimeFor(channel, user)
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getMostActiveUserCallback(channel: String): ListenerCallback {
        return { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                val counter = getMostActiveServiceOf(channel, user)
                if (counter == null)
                    setMostActiveServiceOf(channel, user, 1L)
                else
                    setMostActiveServiceOf(channel, user, counter + 1)
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun evaluateExpresion(expression: String): Double {

//        val newExpresion = expression.replace(" ","")
//                .replace(Regex(".(?!$)"), "$0 ")
//                .split(" ")
//                .dropLastWhile { it.isEmpty() }
//                .toTypedArray()                     // matan: can be spaces in the exp...
        val newExpresion = expression.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val outputRPN = ExpressionParser.infixToRPN(newExpresion)
        return ExpressionParser.RPNtoDouble(outputRPN)
    }

    private fun getAnswerNumber(answers: List<String>, message: Message): Int {
        val content = message.contents.toString(charset)
        return answers.indexOf(content)
    }

    private fun transferTip(channel: String, fromUser: String, toUser: String, number: Long) {
        if (getTippingServiceFor(channel, fromUser) == null)
            setTippingService(channel, fromUser, getBaseTip())

        if (getTippingServiceFor(channel, toUser) == null)
            setTippingService(channel, toUser, getBaseTip())

        if (getTippingServiceFor(channel, fromUser)!! >= number) {
            setTippingService(channel, fromUser, getTippingServiceFor(channel, fromUser)!! - number)
            setTippingService(channel, toUser, getTippingServiceFor(channel, toUser)!! + number)
        }
        addUserToTippingOfChannel(channel, fromUser)
        addUserToTippingOfChannel(channel, toUser)
    }

    private fun sendSurveyQuestion(channel: String, question: String) {
        messageFactory.create(MediaType.TEXT, question.toByteArray(charset)).thenCompose { cApp.channelSend(botToken, channel, it) }.get()
    }

    private fun getBaseTip(): Long {
        return baseTip
    }

    private fun initiateCounterForPair(p: Pair<String?, MediaType?>, channelToReset: String? = null) {
        if (channelToReset == null) {
            val channelOfBot = channels().join()
            for (channel in channelOfBot) {
                initiateSinglePairInChannel(channel, p)
            }
        } else {
            initiateSinglePairInChannel(channelToReset, p)
        }
    }

    private fun getMostActiveUser(channel: String): String? {
        var mostActiveUser: String? = null
        var mostActiveCounter: Long = 0
        getMostActiveService(channel).asSequence().forEach {
            if (mostActiveUser == null || mostActiveCounter < it.value) {
                mostActiveUser = it.key
                mostActiveCounter = it.value
            } else if (mostActiveCounter == it.value) {
                return null
            }
        }
        return mostActiveUser
    }

    //should reset all counters of pairs for specific channel
    private fun resetCounterForAllChannelsOfBot( channelToReset: String ) {
        initCountersOf(channelToReset)
    }



    private fun initiateSinglePairInChannel(channelToReset: String, p: Pair<String?, MediaType?>) {
        if (!isCountersExists()) {
            initCountersIfNeeded()
        }
        if (!isCountersOfExists(channelToReset))
            initCountersOf(channelToReset)
        resetCounter(channelToReset, p)
    }

    private fun getRichestUser(channel: String): String? {
        var richestUser: String? = null
        var richestAmount: Long = 0
        if (getTippingService(channel) == null) return null
        getTippingService(channel)!!.asSequence().forEach {
            if (richestUser == null || richestAmount < it.value) {
                richestUser = it.key
                richestAmount = it.value
            } else if (richestAmount == it.value) {
                return null
            }
        }
        return richestUser
    }

    private fun resetBotIn(channel: String) {
        /*
        // reset surveys
        surveys.asSequence().filter {
            !surveyCallbacks[channel]!!.contains(it.value.first)
        }.toList()          // TODO ("fix it")

        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        */
    }

    // ========================== getters, setters & inits for storage structures ======================================
    private fun channelsList(): MutableList<String> {
        val listId = channels.read(botName)
        if (listId == null) {
            channels.write(botName, listFactory.get().getId())
            return mutableListOf()
        }
        val listStorage = listFactory.restoreLinkedList(listId)
        val listLocal : MutableList<String> = mutableListOf()
        for(channel in listStorage) listLocal.add(channel)
        return listLocal
    }

    private fun addToChannelsList(channelName: String) {
        var listId = channels.read(botName)
        if (listId == null) {
            listId = listFactory.get().getId()
            channels.write(botName, listId)
        }
        val listStorage = listFactory.restoreLinkedList(listId)

        if(listStorage.contains(channelName)) return
        listStorage.add(channelName)
    }

    private fun removeFromChannelsList(channelName: String) {
        val listId = channels.read(botName)?: throw Exception()
        val listStorage = listFactory.restoreLinkedList(listId)
        if(!listStorage.contains(channelName)) throw Exception()
        listStorage.remove(channelName)
    }
    // botName -> (Calculation Trigger)String
    private fun getTriggerPhrase(): String? {
        val res = triggerPhrase.read(botName)
        if(res == null || res == "null")
            return null
        return res
    }
    // botName -> (Calculation Trigger)String
    private fun setTriggerPhrase(trigger: String?) {
        var triggerV = trigger
        if(trigger == null)
            triggerV = "null"
        triggerPhrase.write(botName, triggerV!!)
    }

    private fun getTipTriggerPhrase(): String? {
        val res = tipTriggerPhrase.read(botName)
        if(res == null || res == "null")
            return null
        return res
    }

    private fun setTipTriggerPhrase(trigger: String?) {
        var triggerV = trigger
        if(trigger == null)
            triggerV = "null"
        tipTriggerPhrase.write(botName, triggerV!!)
    }
    // surveyID -> (heap of idAnswer -> counter)
    private fun getSurveys(id: String): List<Long> {
        val heapOfAnswerCountersId = surveys.read(id)?: return mutableListOf()
        val heapOfAnswerCounters = heapFactory.restoreMaxHeap(heapOfAnswerCountersId)
        val resList : MutableList<Long> = mutableListOf()
        val amountOfAnswers = heapOfAnswerCounters.count()
        for( answerId in 0 until amountOfAnswers){
            resList.add(heapOfAnswerCounters.getScore(answerId.toString()).toLong())
        }
        return resList
    }

    private fun initSurveys(id: String, size: Int) {
        var heapOfAnswerCountersId = surveys.read(id)
        if(heapOfAnswerCountersId == null){
            heapOfAnswerCountersId = heapFactory.get().getId()
            surveys.write(id, heapOfAnswerCountersId)
        }
        val heapOfAnswerCounters = heapFactory.restoreMaxHeap(heapOfAnswerCountersId)
        val amountOfAnswers = heapOfAnswerCounters.count()
        for( answerId in 0 until amountOfAnswers){      //remove previous elements
            heapOfAnswerCounters.remove(answerId.toString())
        }

        for( i in 0 until size){    //size excluded - [0, size)
            heapOfAnswerCounters.add(i.toString())  //heap put 0 automatically as data for current key
        }
    }
    private fun isSurveysExists(identifier: String) : Boolean{
        return surveys.read(identifier) != null
    }

    private fun incSurveyVote(surveyId: String, answerId: Int) {
        val heapOfAnswerCountersId = surveys.read(surveyId)!!
        val heapOfAnswerCounters = heapFactory.restoreMaxHeap(heapOfAnswerCountersId)
        val oldCounter = heapOfAnswerCounters.getScore(answerId.toString())
        heapOfAnswerCounters.changeScore(answerId.toString() ,  1)
    }

    private fun decSurveyVote(surveyId: String, answerId: Int) {
        val heapOfAnswerCountersId = surveys.read(surveyId)!!
        val heapOfAnswerCounters = heapFactory.restoreMaxHeap(heapOfAnswerCountersId)
        val oldCounter = heapOfAnswerCounters.getScore(answerId.toString())
        var newCounter = oldCounter - 1
        if(newCounter < 0) newCounter = 0
        heapOfAnswerCounters.changeScore(answerId.toString() , newCounter - oldCounter)
    }
    // surveyID -> heap of voter(userName) to answerID
    private fun setVoteIdForVoter(surveyId: String, voter: String, newVoteId: Int){
        val heapOfAnswerCountersId = surveysHistory.read(surveyId)!!
        val heapOfAnswerCounters = heapFactory.restoreMaxHeap(heapOfAnswerCountersId)
        if(heapOfAnswerCounters.alreadyExist(voter)){
            decSurveyVote(surveyId, heapOfAnswerCounters.getScore(voter))
            val oldVoteId = heapOfAnswerCounters.getScore(voter)
            heapOfAnswerCounters.changeScore(voter, newVoteId - oldVoteId)
        }
        else{
            heapOfAnswerCounters.add(voter)
            heapOfAnswerCounters.changeScore(voter, newVoteId - 0)  //0 by default is the old identifier
        }
        incSurveyVote(surveyId, newVoteId)
    }


    private fun initSurveysHistory(surveyId: String) {
        val heapOfAnswerCountersId = surveysHistory.read(surveyId)
        assert(heapOfAnswerCountersId == null)
        surveysHistory.write(surveyId, heapFactory.get().getId())

    }

    private fun getSurveyId(): String {
        val id = idGenerator.read("num")
        if(id == null){
            idGenerator.write("num", "1")
            return "0"
        }
        idGenerator.write("num", (id.toInt()+1).toString())
        return id
    }
    //HashMap<String, HashMap<String, HashMap<String, LocalDateTime?>>>
    private fun initSeenLastTimeIn(channel: String) {
        var channelToUserToTimeId = seenLastTimeService.read(botName)
        if(channelToUserToTimeId == null){
            channelToUserToTimeId = dicFactory.get().getId()
            seenLastTimeService.write(botName, channelToUserToTimeId)
        }
        val channelToUserToTime = dicFactory.restoreDictionary(channelToUserToTimeId)
        var userToTimeId = channelToUserToTime.read(channel)
        if(userToTimeId == null){
            userToTimeId = dicFactory.get().getId()
            channelToUserToTime.write(channel, userToTimeId)
        }
    }

    private fun setSeenLastTimeFor(channel: String, user: String) {
        val channelToUserToTimeId = seenLastTimeService.read(botName)!!
        val channelToUserToTime = dicFactory.restoreDictionary(channelToUserToTimeId)
        val userToTimeId = channelToUserToTime.read(channel)!!
        val userToTimeMap = dicFactory.restoreDictionary(userToTimeId)
        val currentTime = LocalDateTime.now().format(timeFormatter)
        userToTimeMap.write(user, currentTime)
    }

    private fun getSeenLastTimeFor(channel: String, user: String): LocalDateTime? {
        val channelToUserToTimeId = seenLastTimeService.read(botName)!!
        val channelToUserToTime = dicFactory.restoreDictionary(channelToUserToTimeId)
        val userToTimeId = channelToUserToTime.read(channel)!!
        val userToTimeMap = dicFactory.restoreDictionary(userToTimeId)
        val currentTime =  userToTimeMap.read(user)?: return null
        return  LocalDateTime.parse(currentTime, timeFormatter)
    }
    //type counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private fun initCountersIfNeeded() {
        val id = counters.read(botName)
        if (id == null) {
            counters.write(botName, dicFactory.get().getId())
        }
        val id2 = countersChannelListOfPairs.read(botName)
        if (id2 == null) {
            countersChannelListOfPairs.write(botName, dicFactory.get().getId())
        }
    }
    //type counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private fun initCountersOf(channel: String) {
        var id = counters.read(botName)
        if(id == null){
            id = dicFactory.get().getId()
            counters.write(botName, id)
        }
        val mapChannel = dicFactory.restoreDictionary(id)
        val channelMapId  = mapChannel.read(channel)
        if(channelMapId == null){
            mapChannel.write(channel, dicFactory.get().getId())
            return
        }
        val mapPairToLong = dicFactory.restoreDictionary(channelMapId)

        val channelToListPairsId = countersChannelListOfPairs.read(botName)!!    // botName -> channel -> list of pairs
        val channelToListPairMap = dicFactory.restoreDictionary(channelToListPairsId)
        var listId = channelToListPairMap.read(channel)
        if(listId == null){
            listId = listFactory.get().getId()
            channelToListPairMap.write(channel, listId)
        }
        val listPairs = listFactory.restoreLinkedList(listId)
        for(pair in listPairs){
            mapPairToLong.write(pair, "0")
        }

    }

    private fun isCountersExists(): Boolean {
        return counters.read(botName) != null
    }
    //type counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private fun getCounterOf(channel: String, p: Pair<String?, MediaType?>): Long? {
        val id = counters.read(botName)!!
        val mapChannel = dicFactory.restoreDictionary(id)
        val channelMapId  = mapChannel.read(channel)!!
        val mapPairToLong = dicFactory.restoreDictionary(channelMapId)

        return mapPairToLong.read(pairToString(p))!!.toLong()
    }

    private fun pairToString(p: Pair<String?, MediaType?>) : String{
        return p.first + "%" + p.second?.ordinal.toString()
    }

    private fun incCounterOf(channel: String, p: Pair<String?, MediaType?>) {
        val id = counters.read(botName)!!
        val mapChannel = dicFactory.restoreDictionary(id)
        val channelMapId  = mapChannel.read(channel)!!
        val mapPairToLong = dicFactory.restoreDictionary(channelMapId)
        val oldS = mapPairToLong.read(pairToString(p))!!
        val oldCounter = oldS.toLong()
        mapPairToLong.write(pairToString(p), (oldCounter+1).toString())
    }
    //type counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private fun isCountersOfExists(channel: String): Boolean {
        val id = counters.read(botName)!!
        val mapChannel = dicFactory.restoreDictionary(id)
        return mapChannel.read(channel) != null
    }
    //type counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>>
    private fun resetCounter(channel: String, p: Pair<String?, MediaType?>) {
        val id = counters.read(botName)!!
        val mapChannel = dicFactory.restoreDictionary(id)
        val channelMapId  = mapChannel.read(channel)!!
        val mapPairToLong = dicFactory.restoreDictionary(channelMapId)
        mapPairToLong.write(pairToString(p), "0")
        addNewPairToListOfNameToChannelToList(channel, p)
    }

    private fun addNewPairToListOfNameToChannelToList(channel: String, p: Pair<String?, MediaType?>) {
        val channelToListPairsId = countersChannelListOfPairs.read(botName)!!    // botName -> channel -> list of pairs
        val channelToListPairMap = dicFactory.restoreDictionary(channelToListPairsId)
        var listId = channelToListPairMap.read(channel)
        if(listId == null){
            listId = listFactory.get().getId()
            channelToListPairMap.write(channel, listId)
        }
        val listPairs = listFactory.restoreLinkedList(listId)
        if(!listPairs.contains(pairToString(p)))
            listPairs.add(pairToString(p))
    }

    // botName -> (channel ->(user, number of messages sent by user))
    private fun initMostActiveService(channel: String) {
        var channelToUserToNumberId = mostActiveService.read(botName)
        if(channelToUserToNumberId == null){
            channelToUserToNumberId = dicFactory.get().getId()
            mostActiveService.write(botName, channelToUserToNumberId)
        }
        val channelToUserToNumber = dicFactory.restoreDictionary(channelToUserToNumberId)
        var userToNumberId = channelToUserToNumber.read(channel)
        if(userToNumberId == null){
            userToNumberId = dicFactory.get().getId()
            channelToUserToNumber.write(channel, userToNumberId)
        }
    }
    // botName -> (channel ->(user, number of messages sent by user))
    private fun getMostActiveServiceOf(channel: String, user: String): Long? {
        val channelToUserToNumberId = mostActiveService.read(botName)!!
        val channelToUserToNumber = dicFactory.restoreDictionary(channelToUserToNumberId)
        val userToNumberId = channelToUserToNumber.read(channel)!!
        val userToNumberMap = dicFactory.restoreDictionary(userToNumberId)
        return userToNumberMap.read(user)?.toLong()
    }
    // botName -> (channel ->(user, number of messages sent by user))
    private fun setMostActiveServiceOf(channel: String, user: String, value: Long) {
        val channelToUserToNumberId = mostActiveService.read(botName)!!
        val channelToUserToNumber = dicFactory.restoreDictionary(channelToUserToNumberId)
        val userToNumberId = channelToUserToNumber.read(channel)!!
        val userToNumberMap = dicFactory.restoreDictionary(userToNumberId)
        userToNumberMap.write(user, value.toString())
    }
    // botName -> (channel ->(user, number of messages sent by user))
    private fun getMostActiveService(channel: String): HashMap<String, Long> {
        val channelToUserToNumberId = mostActiveService.read(botName)!!
        val channelToUserToNumber = dicFactory.restoreDictionary(channelToUserToNumberId)
        val userToNumberId = channelToUserToNumber.read(channel)!!
        val userToNumberMap = dicFactory.restoreDictionary(userToNumberId)

        val listOfUsersId = channelToUsersLists.read(channel)?:return hashMapOf()
        val listOfUsersMap = listFactory.restoreLinkedList(listOfUsersId)
        val resHashMap : HashMap<String, Long> = hashMapOf()
        for(user in listOfUsersMap) {
            val value = userToNumberMap.read(user) ?: continue
            resHashMap[user] = value.toLong()
        }
        return resHashMap
    }
    //HashMap<String, HashMap<String, HashMap<String, Long>>>
    private fun initTippingService(channel: String) {
        var idChannelToUserToValue = tippingService.read(botName)
        if(idChannelToUserToValue == null){
            idChannelToUserToValue = dicFactory.get().getId()
            tippingService.write(botName, idChannelToUserToValue)
        }
        val channelToUserToValueMap = dicFactory.restoreDictionary(idChannelToUserToValue)
        val userToValueId = channelToUserToValueMap.read(channel)
        if(userToValueId == null){
            channelToUserToValueMap.write(channel, dicFactory.get().getId())
        }
    }

    private fun getTippingServiceFor(channel: String, user: String): Long? {
        val idChannelToUserToValue = tippingService.read(botName)!!
        val channelToUserToValueMap = dicFactory.restoreDictionary(idChannelToUserToValue)
        val userToValueId = channelToUserToValueMap.read(channel)!!
        val userToValueMap = dicFactory.restoreDictionary(userToValueId)
        return userToValueMap.read(user)?.toLong()

    }

    private fun setTippingService(channel: String, user: String, value: Long) {
        val idChannelToUserToValue = tippingService.read(botName)!!
        val channelToUserToValueMap = dicFactory.restoreDictionary(idChannelToUserToValue)
        val userToValueId = channelToUserToValueMap.read(channel)!!
        val userToValueMap = dicFactory.restoreDictionary(userToValueId)
        return userToValueMap.write(user, value.toString())
    }

    private fun getTippingService(channel: String): HashMap<String, Long>? {
        val idChannelToUserToValue = tippingService.read(botName)!!
        val channelToUserToValueMap = dicFactory.restoreDictionary(idChannelToUserToValue)
        val userToValueId = channelToUserToValueMap.read(channel)!!
        val userToValueMap = dicFactory.restoreDictionary(userToValueId)

        val usersListId = channelToUsersLists.read(channel)!!
        val usersList = listFactory.restoreLinkedList(usersListId)

        val usersToValues : HashMap<String, Long> = hashMapOf()
        for(user in usersList){
            val value = userToValueMap.read(user)
            if(value != null){
                usersToValues[user] = value.toLong()
            }
        }
        return usersToValues
    }

    private fun addUserToTippingOfChannel(channelName : String, userName: String ){
        var usersListId = channelToUsersLists.read(channelName)
        if(usersListId == null){
            usersListId = listFactory.get().getId()
            channelToUsersLists.write(channelName, usersListId)
        }
        val usersList = listFactory.restoreLinkedList(usersListId)
        if(!usersList.contains(userName)){
            usersList.add(userName)
        }
    }

    // TODO ("counter with triplet issue...")
    // TODO ("issue with zeroing counters and all the stats for channels after parting the channel")
    // TODO ("choose library")
    // TODO ("move data structures library")
    // TODO ("pass to tests")
    // TODO ("excretions eval - need to correct it... examples: (20) +2 ; (2 + 4) ; (2 + -2) ; 2*20 + 5 : -2 + -2 : (2+3)*5 : (2 +     6 ) *     7")
}