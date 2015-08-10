from __future__ import with_statement

import math
import traceback

from com.lightstreamer.ls_client import LSClient, ConnectionInfo, ExtendedConnectionListener, HandyTableListener, ExtendedTableInfo
from com.rabbitmq.client import ConnectionFactory, QueueingConsumer
from java.io import BufferedReader, InputStreamReader, ByteArrayOutputStream
from java.lang import StringBuffer, String
from java.net import URL, URLEncoder, CookieHandler, CookieManager, CookiePolicy, SocketTimeoutException
from javax.net.ssl import SSLProtocolException
from java.util import Random
from java.util.zip import InflaterOutputStream
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest, HTTPPluginControl, TimeoutException
from org.apache.commons.codec.binary import Base64
from org.json.simple import JSONValue
from time import time
from collections import deque
from threading import Lock
from HTTPClient import NVPair


log = grinder.logger.info
HTTPPluginControl.getConnectionDefaults().setFollowRedirects(2)
HTTPPluginControl.getConnectionDefaults().setTimeout(10000)
CookieHandler.setDefault(CookieManager(None, CookiePolicy.ACCEPT_ALL))  # require for LS load balancing
baseUrl = grinder.properties.getProperty("web.baseUrl")
isLogEnabled = grinder.properties.getBoolean("log.isEnabled", False)
useRabbitForGameMessages = grinder.properties.getBoolean("yazino.useRabbitForGameMessages", False)


class PlayerSource:
    def __init__(self):
        sessionDuration = grinder.properties.getInt("player.sessionDurationInSeconds", 0)
        self.password = grinder.properties.getProperty("player.password")
        players_url = grinder.properties.getProperty("player.source-url")
        passwordHash = URLEncoder.encode(grinder.properties.getProperty("player.passwordHash"), "UTF-8")
        workers = grinder.properties.getInt("grinder.processes", 0)
        threads = grinder.properties.getInt("grinder.threads", 0)
        workerIndex = grinder.processNumber - grinder.firstProcessNumber
        threadsPerAgent = workers * threads
        agentIndex = grinder.agentNumber
        start = agentIndex * threadsPerAgent + workerIndex * threads
        if isLogEnabled:
            log("Worker will handle %s players starting from %d" % (threads, start))
        playerRange = (start, threads)
        self.workerPlayers = []
        params = "passwordHash=%s&minimumBalance=%s&startRow=%s&limit=%s" % (passwordHash, str(sessionDuration / 5), str(playerRange[0]), str(threads))
        urlStr = players_url + "?" + params
        try:
            data = StringBuffer()
            url = URL(urlStr)
            conn = url.openConnection()
            rd = BufferedReader(InputStreamReader(conn.getInputStream()))
            line = rd.readLine()
            while line is not None:
                data.append(line)
                line = rd.readLine()
            rd.close()
            if isLogEnabled:
                log(data.toString())
            message = JSONValue.parse(str(String(data.toString(), "UTF-8")))
            for entry in message:
                self.workerPlayers.append((entry.get(0), entry.get(1)))
        except Exception:
            raise Exception("Couldn't fetch players from %s: %s" % (urlStr, traceback.format_exc()))
        if isLogEnabled:
            log(str(self.workerPlayers))

    def getLoginDetails(self, threadNumber):
        if threadNumber > len(self.workerPlayers):
            raise Exception("Player for thread #%d not available. (worker has %d players)" % (threadNumber, len(self.workerPlayers)))
        return (self.workerPlayers[threadNumber][0], self.workerPlayers[threadNumber][1], self.password)


class GameStatusChanges:
    gameId = 1
    gameStartIncrement = -1
    players = []
    amIPlayer = False
    currentBalance = 0.0
    increment = 0
    allowedActions = []
    nextTimeout = None
    changes = []
    warnings = None

    def __init__(self, messageBody):
        messageObject = JSONValue.parse(str(String(messageBody, "UTF-8")))
        if isLogEnabled:
            log("parsed json to " + JSONValue.toJSONString(messageObject))
        lines = messageObject.get("changes").split("\n")
        gameIdAndStartIncrementAndPlayers = lines[0].split("\t")
        self.gameId = int(gameIdAndStartIncrementAndPlayers[0])
        self.gameStartIncrement = int(gameIdAndStartIncrementAndPlayers[1])
        self.players = gameIdAndStartIncrementAndPlayers[2:]
        if lines[1]:
            self.currentBalance = float(lines[1])
        self.increment = int(lines[2])
        self.allowedActions = lines[3].split("\t")  # TODO make it a set
        nextTimeoutLine = lines[4].split("\t")
        if len(nextTimeoutLine) == 3:
            self.nextTimeout = (str(nextTimeoutLine[0]), long(nextTimeoutLine[1]), long(nextTimeoutLine[2]))
        self.changes = []
        for i in range(5, len(lines)):
            self.changes.append(GameStatusChange(lines[i].split("\t")))
        self.warnings = messageObject.get("warnings")


class GameStatusChange:
    increment = 0
    changetype = ""
    details = []

    def __init__(self, line):
        self.increment = int(line[0])
        self.changeType = line[1]
        self.details = line[2:]


class ScheduledFunctionContext:
    playerId = None
    tableId = None
    rabbitConnection = None
    consumer = None
    botStrategy = None
    lastReceivedIncrement = -1
    lobbyLightstreamerConnection = None
    gameLightstreamerConnection = None
    lobbyConsumer = None
    tournamentDetails = None
    fetchedPlayerIds = set()
    gameLightstreamerMessageQueue = deque()
    gameLightstreamerMessageQueueLock = Lock()


def getScheduleInvoker(initialTime, schedule, context):
    totalNumberOfInvocations = map(lambda x: 0, schedule)

    def result():
        sleepTime = 10000
        for idx, functionPeriod in enumerate(schedule):
            scheduledFunction = functionPeriod[0]
            periodInSeconds = functionPeriod[1]
            requestsToMake = int((time() - initialTime) / periodInSeconds - totalNumberOfInvocations[idx])
            for i in range(0, requestsToMake):
                scheduledFunction(context)
                totalNumberOfInvocations[idx] += 1
            sleepTime = min(sleepTime, long(1000 * (initialTime + periodInSeconds * (totalNumberOfInvocations[idx] + 1) - time())))
        return max(sleepTime, 0)
    return result

testCounter = 0


def fail(reason):
    if grinder.statistics.forCurrentTest.success:
        log("FAILURE: %s" % (str(reason)))
    else:
        log("FAILURE[ADD]: %s" % (str(reason)))

    grinder.statistics.forCurrentTest.setSuccess(0)


def createTestFunction(description, testFunction):
    global testCounter
    testCounter += 1
    test = Test(testCounter, description)
    return test.wrap(testFunction())


def httpGet(url, params=None):
    try:
        request = HTTPRequest()
        if params is not None:
            return request.GET(url, params)
        else:
            return request.GET(url)

    except SSLProtocolException, e:
        log("SSL error for %s: %s" % (url, e))
        return None

    except TimeoutException:
        log("Timeout for %s" % (url))
        return None


def httpPost(url, params=None):
    try:
        request = HTTPRequest()
        if params is not None:
            return request.POST(url, params)
        else:
            return request.POST(url)

    except SSLProtocolException, e:
        log("SSL error for %s: %s" % (url, e))
        return None
    except (TimeoutException, SocketTimeoutException):
        log("Timeout for %s" % (url))
        return None


def createHttpLogin():
    def result(context):
        login = playerSource.getLoginDetails(grinder.threadNumber)
        context.playerId = str(login[0]).rstrip('0').rstrip('.')
        response = httpPost(baseUrl + "public/login/WEB/SLOTS/YAZINO", (NVPair("email", login[1]), NVPair("password", login[2])))
        if response is None:
            fail("Login returned no response for %s / %s" % (str(login[1]), str(login[2])))
        elif response.statusCode != 200:
            fail("Login returned status %d for %s / %s" % (response.statusCode, str(login[1]), str(login[2])))

    return result


def createHttpGoToLobby():
    def result(context):
        response = httpGet(baseUrl + "roulette")
        if response is None:
            fail("Lobby request returned no response")
        elif response.statusCode != 200:
            fail("Lobby request returned unexpected status code: %d" % (response.statusCode))
        elif response.text.lower().find("play now") == -1:
            fail("Lobby request failed: " + response.text)
    return result


def createHttpDailyAward():
    def result(context):
        response = httpGet(baseUrl + "dailyAward/topUpResult")
        if response is None:
            fail("Daily award request returned no response")
        elif response.statusCode != 200:
            fail("Daily award request returned unexpected status code: %d" % (response.statusCode))
        elif response.text.find("{") == -1:
            fail("Daily Award request failed: " + response.text)
    return result


def createHttpPublishCommunity():
    def result(context):
        response = httpPost(baseUrl + "game-server/command/community", "publish|SLOTS")
        if isLogEnabled:
            log("community command response " + response.text)

        if response is None:
            fail("Community command returned no response")
        elif response.statusCode != 200:
            fail("Community command returned unexpected status code: %d" % (response.statusCode))
        elif response.text != "OK":
            fail("Community command failed: " + response.text)
    return result


def createHttpPublishFriendsSummary():
    def result(context):
        response = httpPost(baseUrl + "/social/friendsSummary")
        if isLogEnabled:
            log("friends command response " + response.text)

        if response is None:
            fail("Publish friends summary returned no response")
        elif response.statusCode != 200:
            fail("Publish friends summary returned unexpected status code: %d" % (response.statusCode))
        else:
            responseObject = JSONValue.parse(response.text)
            if responseObject is None:
                fail("Publish friends summary returned an unparsable response: %s" % (response.text))
            if responseObject.get("result") != "ok":
                fail("Publish friends summary failed: %s" % (str(responseObject)))
    return result


def createHttpGlobalPlayerList():
    def result(context):
        response = httpGet(baseUrl + "lobbyCommand/globalPlayers")
        if response is None:
            fail("Global players returned no response")
        elif response.statusCode != 200:
            fail("Global players returned unexpected status code: %d" % (response.statusCode))
    return result


def createHttpLobbyInformation():
    def result(context):
        response = httpGet(baseUrl + "public/lobbyInformation")
        if response is None:
            fail("Lobby information returned no response")
        elif response.statusCode != 200:
            fail("Lobby information returned unexpected status code: %d" % (response.statusCode))
    return result


def createHttpGameCommandDefinition():
    def sendCommand(tableId, commandType, arguments, incrementToAck):
        commandTypeAndArguments = [commandType]
        commandTypeAndArguments.extend(map(str, arguments))
        suffix = "|".join(commandTypeAndArguments) + "\n" + "|".join([tableId, "-1", "ACK", str(incrementToAck)])
        log("Sending command: %s" % (suffix))
        response = httpPost(baseUrl + "api/1.0/game/send-command", tableId + "|-1|" + suffix)
        if response is None:
            fail("Command submission returned no response")
        elif response.statusCode != 200:
            fail("Command submission returned unexpected status code: %d" % (response.statusCode))
        else:
            log("Command submission response %s" % (response.text))
    return sendCommand


def createHttpPictureAndLevel():
    def result(playerIds):
        if len(playerIds) > 0:
            response = httpGet(baseUrl + "game-server/command/pictureAndLevel", [NVPair("gameType", "SLOTS"), NVPair("playerIds", ",".join(playerIds))])
            if response is None:
                fail("Picture and level returned no response")
            elif response.statusCode != 200:
                fail("Picture and level returned unexpected status code: %d" % (response.statusCode))
            elif response.text.find("[") == -1:
                fail("fetching pictures and levels failed: " + response.text)
    return result


def createHttpPicture():
    def result(playerIds):
        for playerId in playerIds:
            response = httpGet(baseUrl + "game-server/command/picture", [NVPair("playerId", playerId)])
            if response is None:
                fail("Picture returned no response")
            elif response.statusCode != 200:
                fail("Picture returned unexpected status code: %d" % (response.statusCode))
    return result


def createRabbitConnectionFactory(host):
    rabbitConnectionFactory = ConnectionFactory()
    rabbitConnectionFactory.setUsername(grinder.properties.getProperty("rabbit.username"))
    rabbitConnectionFactory.setPassword(grinder.properties.getProperty("rabbit.password"))
    rabbitConnectionFactory.setVirtualHost(grinder.properties.getProperty("rabbit.virtualHost"))
    rabbitConnectionFactory.setHost(host)
    rabbitConnectionFactory.setPort(5672)
    return rabbitConnectionFactory


def createLightstreamerConnection(host):
    connectionInfo = ConnectionInfo()
    connectionInfo.pushServerUrl = host
    connectionInfo.adapter = "STRATA"

    class PerfConnectionListener(ExtendedConnectionListener):
        def onConnectionEstablished(self):
            log("LS Connection established")

        def onSessionStarted(self, isPolling, controlLink=None):
            log("LS Session Started")

        def onNewBytes(self, newBytes):
            pass

        def onDataError(self, e):
            log("Data Error: " + e)

        def onActivityWarning(self, warningOn):
            pass

        def onEnd(self, cause):
            log("LS End: " + cause)

        def onClose(self):
            log("LS Closed")

        def onFailure(self, e):
            fail("LS Failure: %s" % (e))
    lsClient = LSClient()
    lsClient.openConnection(connectionInfo, PerfConnectionListener())
    return lsClient


def closeLighstreamerConnection(connection):
    try:
        if connection:
            connection.closeConnection()
    except:
        log("Lightstreamer connection close failed")
    return None


def subscribeTo(paths, lsClient, onMessage):
    log("LS Subscribing to %s" % (str(paths)))

    class LSListener(HandyTableListener):
        def onRawUpdatesLost(self, itemPos, itemName, lostUpdates):
            pass

        def onSnapshotEnd(self, itemPos, itemName):
            pass

        def onUnsubscr(self, itemPos, itemName):
            log("LS unsubscribed from %s" % (str(itemName)))
            pass

        def onUnsubscrAll(self):
            log("LS unsubscribed from all")
            pass

        def onUpdate(self, itemPos, itemName, update):
            log("LS Received message " + str(update.getNewValue("contentType")))
            onMessage(update.getNewValue("contentType"), update.getNewValue("body"))

    tableInfo = ExtendedTableInfo(paths, ExtendedTableInfo.RAW, ["contentType", "body"], False)
    tableInfo.setDataAdapter("TABLE")
    subscriptionKeys = lsClient.subscribeItems(tableInfo, LSListener())
    log("LS subscriptions are %s" % (str(subscriptionKeys)))

rabbitConnectionFactories = map(lambda host: createRabbitConnectionFactory(host), grinder.properties.getProperty("rabbit.hosts").split(","))


def getRabbitConnectionFactory(playerId):
    return rabbitConnectionFactories[int(playerId) % len(rabbitConnectionFactories)]


def retrieveBody(delivery):
    if delivery.properties.contentEncoding == "DEF":
        compressed = Base64.decodeBase64(String(delivery.body).getBytes("UTF-8"))
        inflated = ByteArrayOutputStream()
        inflatedOs = InflaterOutputStream(inflated)
        inflatedOs.write(compressed)
        inflatedOs.close()
        return inflated.toString("UTF-8")
    else:
        return String(delivery.body, "UTF-8")


class Lobby:
    def __init__(self):
        self.handlers = {
            'FRIENDS_SUMMARY': self.processFriendSummary
        }
        self.players = createTestFunction("Player Details", self.retrievePlayerDetails)

    def handle(self, messageType, messageBody):
        log("Received message of type " + str(messageType))
        if messageType in self.handlers:
            handler = self.handlers[messageType]
            message = JSONValue.parse(str(String(messageBody, "UTF-8")))
            handler(message)

    def processFriendSummary(self, message):
        requests = set(message.get("summary").get("requests"))
        if len(requests) > 0:
            self.players(requests, ["name, picture"])
        friends_panel = set(message.get("summary").get("online_ids"))
        friends_panel = friends_panel.union(set(message.get("summary").get("offline_ids")))
        if len(friends_panel) > 0:
            self.players(friends_panel, ["name, picture"])

    def retrievePlayerDetails(self):
        def retrieve(playerIds, infoTypes):
            response = httpGet(baseUrl + "/social/players", [NVPair("gameType", "SLOTS"), NVPair("playerIds", ",".join(map(str, playerIds))), NVPair("fields", ",".join(infoTypes))])
            if response is None:
                fail("Player details returned no response for player IDs %s" % (",".join(map(str, playerIds))))
            elif response.statusCode != 200:
                fail("Player details returned status code %d for player IDs %s" % (response.statusCode, ",".join(map(str, playerIds))))
            else:
                result = JSONValue.parse(str(String(response.text, "UTF-8")))
                if result.get("result") != "ok":
                    fail("Player details retrieval failed for %s" % (str(playerIds)))
        return retrieve


def createProcessLobbyMessages():
    lobby = Lobby()

    def result(context):
        def handle_message(contentType, messageBody):
            log("LOBBY processing doc: " + str(messageBody))
            lobby.handle(contentType, messageBody)

        if not context.lobbyLightstreamerConnection:
            log("LOBBY - creating new connection to " + grinder.properties.getProperty("lightstreamer.host"))
            lsClient = createLightstreamerConnection(grinder.properties.getProperty("lightstreamer.host"))
            context.lobbyLightstreamerConnection = lsClient
            subscribeTo(["PLAYER." + context.playerId], lsClient, handle_message)
            log("LOBBY - connection established to PLAYER." + context.playerId)
        else:
            log("LOBBY - connection already established")
    return result


class GameDefinition:
    def __init__(self, gameType, client, variation):
        self.gameType = gameType
        self.client = client
        self.variation = variation


class GameCommand:
    def __init__(self, commandType, commandArgs):
        self.commandType = commandType
        self.args = commandArgs


class WheelDealBotStrategy:
    isTableFull = None
    isPlayer = None
    canLeave = False
    tsLastCommand = time()
    tsLastGetStatusCommand = 0
    commandCooldown = 5

    def processChanges(self, gameStatusChanges, ownPlayerId):
        self.isTableFull = len(gameStatusChanges.players) == 12
        self.isPlayer = ownPlayerId in gameStatusChanges.players
        self.canBet = "Bet" in gameStatusChanges.allowedActions
        self.canLeave = "Leave" in gameStatusChanges.allowedActions

    def getJoin(self):
        return GameCommand("Join", [])

    def getNextCommand(self):
        return GameCommand("Bet", [1, 20])


class HighStakesBotStrategy:
    isTableFull = None
    isPlayer = None
    canLeave = False
    tsLastCommand = time()
    tsLastGetStatusCommand = 0
    commandCooldown = 5
    Betting, Guessing = range(2)
    lastIncrement = 0
    phase = None
    nextGuess = None

    def __init__(self):
        self.phase = self.Betting
        self.changeProcessors = {
            'NewGameStarted': lambda playerId, details: self.moveTo(self.Betting),
            'Players': lambda playerId, details: self.moveTo(self.Betting),
            'SafeCrackIncorrectGuess': lambda playerId, details: self.moveTo(self.Betting),
            'SafeCrackBonusRoundStarted': lambda playerId, details: self.handleBonusRound(playerId, details)
        }
        self.commandBuilders = {
            self.Betting: lambda: GameCommand("Spin", [1, 1]),
            self.Guessing: lambda: self.guess()
        }

    def moveTo(self, phase):
        self.phase = phase

    def handleBonusRound(self, ownPlayerId, details):
        if details[0] == ownPlayerId:
            self.moveTo(self.Guessing)
            self.nextGuess = details[2]

    def guess(self):
        if self.nextGuess is not None:
            command = GameCommand("Guess", [self.nextGuess])
            self.nextGuess = None
            return command

    def processChanges(self, gameStatusChanges, ownPlayerId):
        self.isTableFull = len(gameStatusChanges.players) == 9
        self.isPlayer = ownPlayerId in gameStatusChanges.players
        self.canBet = "Spin" in gameStatusChanges.allowedActions
        self.canLeave = "Leave" in gameStatusChanges.allowedActions
        for change in gameStatusChanges.changes:
            if change.increment < self.lastIncrement:
                continue
            self.lastIncrement = change.increment
            if change.changeType in self.changeProcessors:
                self.changeProcessors[change.changeType](ownPlayerId, change.details)

    def getJoin(self):
        return GameCommand("Join", [])

    def getNextCommand(self):
        if self.phase in self.commandBuilders:
            return self.commandBuilders[self.phase]()
        return None


def createBot(gameDefinition, botStrategyClass):
    definition = gameDefinition

    def createGameBot():
        sendInitialGetStatus = createTestFunction("InitialGetStatus", createHttpGameCommandDefinition)
        sendTimeoutGetStatus = createTestFunction("TimeoutGetStatus", createHttpGameCommandDefinition)
        sendGameCommand = createTestFunction("GameCommand", createHttpGameCommandDefinition)
        sendJoin = createTestFunction("Join", createHttpGameCommandDefinition)
        getPictureAndLevel = createTestFunction("PictureAndLevel", createHttpPictureAndLevel)
        getPicture = createTestFunction("Picture", createHttpPicture)

        def processChanges(botStrategy, gameStatusChanges, context):
            botStrategy.processChanges(gameStatusChanges, context.playerId)
            newPlayerIds = set(gameStatusChanges.players) - context.fetchedPlayerIds
            getPictureAndLevel(newPlayerIds)
            getPicture(newPlayerIds)
            context.fetchedPlayerIds = context.fetchedPlayerIds.union(newPlayerIds)

        def result(context):
            if context.tableId:
                try:
                    botStrategy = context.botStrategy
                    gotGameStatusMessage = False

                    def handleReceivedMessage(contentType, messageBody):
                        def strMessageBody(messageBody):
                            return str(String(messageBody, "UTF-8")).replace("\\n", "\n").replace("\\t", "\t")

                        if contentType in ["GAME_STATUS", "INITIAL_GAME_STATUS"]:
                            log("processing doc: " + strMessageBody(messageBody))
                            gameStatusChanges = GameStatusChanges(messageBody)
                            processChanges(botStrategy, gameStatusChanges, context)
                            context.lastReceivedIncrement = max(context.lastReceivedIncrement, gameStatusChanges.increment)
                        else:
                            log("Not processing doc of type %s with content: %s" % (contentType, strMessageBody(messageBody)))

                    def checkForRabbitMessage():
                        rabbitMessage = context.consumer.nextDelivery(0)
                        if rabbitMessage is not None:
                            log("RabbitMQ delivery received")
                            return rabbitMessage.properties.contentType, retrieveBody(rabbitMessage)
                        else:
                            log("No RabbitMQ delivery available")
                            return None, None

                    def checkForLightstreamerMessage():
                        try:
                            with context.gameLightstreamerMessageQueueLock:
                                lsMessage = context.gameLightstreamerMessageQueue.popleft()
                                log("LS delivery received")
                                return lsMessage[0], lsMessage[1]
                        except IndexError:
                            log("No LS delivery available")
                            return None, None

                    checkHandler = checkForLightstreamerMessage
                    if context.consumer:
                        checkHandler = checkForRabbitMessage
                    while True:
                        contentType, messageBody = checkHandler()
                        if contentType is not None:
                            handleReceivedMessage(contentType, messageBody)
                        else:
                            break

                    if gotGameStatusMessage:
                        if botStrategy.isTableFull and not botStrategy.isPlayer:
                            context.botStrategy = botStrategyClass()
                            context.tableId = None
                            gotGameStatusMessage = False
                            return
                    if time() < botStrategy.tsLastCommand + botStrategy.commandCooldown:
                        return
                    if botStrategy.isPlayer:
                        if botStrategy.canLeave:
                            command = context.botStrategy.getNextCommand()
                            if command is not None:
                                sendGameCommand(context.tableId, command.commandType, command.args, context.lastReceivedIncrement)
                                botStrategy.tsLastCommand = time()
                    else:
                        command = context.botStrategy.getJoin()
                        sendJoin(context.tableId, command.commandType, command.args, context.lastReceivedIncrement)
                        botStrategy.tsLastCommand = time()
                    if time() > botStrategy.tsLastCommand + 10 and time() > botStrategy.tsLastGetStatusCommand + 10:
                        sendTimeoutGetStatus(context.tableId, "GetStatus", [], context.lastReceivedIncrement)
                        botStrategy.tsLastGetStatusCommand = time()

                except Exception:
                    fail("Message processing failed for %s: %s" % (context.playerId, traceback.format_exc()))

            else:
                try:
                    response = httpGet(baseUrl + ("lobbyCommand/mobile/tableLocator?gameType=%s&clientId=%s&variationName=%s" % (definition.gameType, definition.client, definition.variation)))
                    responseObject = JSONValue.parse(response.text)
                    if response is None:
                        fail("Table location failed with response %s" % (response.text))
                    elif response.statusCode != 200:
                        fail("Table location failed with status code: %d" % (responseObject.statusCode))
                    elif responseObject.get("tableId") is None:
                        fail("Table location failed with missing table ID in response %s" % (str(responseObject)))
                    else:
                        tableId = str(responseObject.get("tableId")).rstrip('0').rstrip('.')
                        log("tableId = " + tableId)

                        def connectToRabbit(playerId, tableId, rabbitConnection):
                            if rabbitConnection:
                                rabbitConnection.close()
                            rabbitConnectionFactory = getRabbitConnectionFactory(playerId)
                            rabbitConnection = rabbitConnectionFactory.newConnection()
                            channel = rabbitConnection.createChannel()
                            queueName = channel.queueDeclare().getQueue()
                            channel.queueBind(queueName, "player-direct", "PLAYERTABLE." + playerId + "." + tableId)
                            channel.queueBind(queueName, "player-direct", "PLAYER." + playerId)
                            consumer = QueueingConsumer(channel)
                            channel.basicConsume(queueName, True, consumer)
                            return (rabbitConnection, consumer)

                        def connectToLightstreamer(playerId, tableId, messageHandler):
                            lsClient = createLightstreamerConnection(grinder.properties.getProperty("lightstreamer.host"))
                            subscribeTo(["PLAYER." + playerId,"PLAYERTABLE." + playerId + "." + tableId], lsClient, messageHandler)
                            return lsClient

                        def handleLightstreamerMessage(contentType, messageBody):
                            with context.gameLightstreamerMessageQueueLock:
                                context.gameLightstreamerMessageQueue.append((contentType, messageBody))

                        if useRabbitForGameMessages:
                            log("Using RabbitMQ connection for player " + context.playerId)
                            if context.rabbitConnection:
                                context.rabbitConnection.close()
                            context.rabbitConnection, context.consumer = connectToRabbit(context.playerId, tableId, context.rabbitConnection)
                        else:
                            log("Using LightStreamer connection for player " + context.playerId)
                            context.gameLightstreamerConnection = closeLighstreamerConnection(context.gameLightstreamerConnection)
                            context.gameLightstreamerConnection = connectToLightstreamer(context.playerId, tableId, handleLightstreamerMessage)

                        sendInitialGetStatus(tableId, "GetStatus", [], context.lastReceivedIncrement)
                        context.tableId = tableId
                        context.botStrategy = botStrategyClass()
                        context.botStrategy.tsLastGetStatusCommand = time()

                except Exception:
                    fail("Table connection failed for %s: %s" % (context.playerId, traceback.format_exc()))

        return result
    return createGameBot


class TournamentDetails:
    tournamentId = None
    registeredPlayers = 0
    millisToStart = 0
    amIRegistered = False


def getNextTournamentDetails():
    result = TournamentDetails()

    response = httpPost(baseUrl + "tournaments/next", [NVPair("gameType", "TEXAS_HOLDEM")])
    if response is None:
        fail("Next tournament returned an empty response")
    elif response.statusCode != 200:
        fail("Next tournament returned status: %d" %(response.statusCode))
    else:
        responseObject = JSONValue.parse(response.text)
        if responseObject:
            result.tournamentId = responseObject.get("tournamentId")
            result.registeredPlayers = responseObject.get("registeredPlayers")
            result.millisToStart = responseObject.get("millisToStart")
            result.amIRegistered = responseObject.get("playerRegistered")
        else:
            fail("Unable to parse next tournament response %s" % (response.text))

    return result


def createHttpRegisterForNextTournament():
    playersOnline = grinder.properties.getInt("yazino.playersOnline", 1)
    sessionDuration = grinder.properties.getInt("player.sessionDurationInSeconds", 300)
    maxPlayersPerTournament = grinder.properties.getInt("yazino.maxPlayersPerTournament", 1)

    def result(context):
        tournamentDetails = getNextTournamentDetails()
        if tournamentDetails is None:
            fail("Tournament details query failed")
        elif tournamentDetails.amIRegistered:
            context.tournamentDetails = tournamentDetails
        elif tournamentDetails.registeredPlayers < maxPlayersPerTournament and tournamentDetails.millisToStart < 1000 * (30 + maxPlayersPerTournament * sessionDuration / playersOnline):
            response = httpPost(baseUrl + "game-server/command/tournamentAction", [NVPair("action", "register"), NVPair("tournamentId", str(tournamentDetails.tournamentId))])
            if response is None:
                fail("Tournament registration returned an empty response for tournament %s by player %s" % (str(tournamentDetails.tournamentId), str(context.playerId)))
            elif response.statusCode != 200:
                fail("Tournament registration returned status: %d for tournament %s by player %s" %(response.statusCode, str(tournamentDetails.tournamentId), str(context.playerId)))
            elif response.text != "SUCCESS":
                fail("Tournament registration failed for tournament %s by player %s with response %s" % (str(tournamentDetails.tournamentId), str(context.playerId), response.text))
            else:
                context.tournamentDetails = tournamentDetails

    return result


def createHttpTournamentStatus():
    def result(context):
        if context.tournamentDetails:
            response = httpGet(baseUrl + "game-server/command/tournamentStatus?requestType=TOURNAMENT_STATUS&tournamentId=" + str(context.tournamentDetails.tournamentId))
            if isLogEnabled:
                log("TOURNAMENT_STATUS response " + response.text)

            if not response.text:
                fail("No response returned for tournament status for ID %s" % (str(context.tournamentDetails.tournamentId)))
            elif response.statusCode != 200:
                fail("Tournament status for ID %s returned unexpected status: %d" % (str(context.tournamentDetails.tournamentId), response.statusCode))

            response = httpGet(baseUrl + "game-server/command/tournamentStatus?requestType=TOURNAMENT_PLAYER&tournamentId=" + str(context.tournamentDetails.tournamentId))
            if isLogEnabled:
                log("TOURNAMENT_PLAYER response " + response.text)

            if not response.text:
                fail("No response returned for tournament player for ID %s" % (str(context.tournamentDetails.tournamentId)))
            elif response.statusCode != 200:
                fail("Tournament player for ID %s returned unexpected status: %d" % (str(context.tournamentDetails.tournamentId), response.statusCode))

    return result


def createHttpGetNextTournament():
    gameTypes = ["BLACKJACK", "TEXAS_HOLDEM"]
    random = Random()

    def result(context):
        gameType = gameTypes[random.nextInt() % len(gameTypes)]
        response = httpGet(baseUrl + "game-server/command/nextTournament", [NVPair("gameType", gameType)])
        if response.statusCode != 200:
            fail("Next tournament query for game type %s returned unexpected status: %d" % (gameType, response.statusCode))

    return result


def createHttpPingSession():
    def result(context):
        response = httpPost(baseUrl + "game-server/command/pingSession", "ping")
        if response.statusCode != 200:
            fail("Ping returned unexpected status: %d" % (response.statusCode))

    return result

schedule = [
    [createTestFunction("Fetch Lobby Information", createHttpLobbyInformation), 20],
    [createTestFunction("Fetch Next Tournament", createHttpGetNextTournament), 25],
    [createTestFunction("Ping Session", createHttpPingSession), 35],
    [createTestFunction("Fetch Global Player List", createHttpGlobalPlayerList), 100],
    [createTestFunction("Publish Community", createHttpPublishCommunity), 300],
    [createTestFunction("Publish Friends Summary", createHttpPublishFriendsSummary), 300],
    [createTestFunction("Fetch Tournament Status", createHttpTournamentStatus), 5],
    [createTestFunction("Play Game", createBot(GameDefinition("SLOTS", "Default Slots", "Slots Beginner lvl1-5"), WheelDealBotStrategy)), 1],
#    [createTestFunction("AMQP Game", createBot(GameDefinition("HIGH_STAKES", "Default High Stakes", "High Stakes Low"), HighStakesBotStrategy)), 1],
    [createTestFunction("LS Lobby", createProcessLobbyMessages), 1]
]
playerSource = PlayerSource()
httpLogin = createTestFunction("Login", createHttpLogin)
httpGoToLobby = createTestFunction("Lobby", createHttpGoToLobby)
httpDailyAward = createTestFunction("Daily Award", createHttpDailyAward)
httpRegisterForNextTournament = createTestFunction("RegisterForNextTournament", createHttpRegisterForNextTournament)
httpLeave = createTestFunction("Leave", createHttpGameCommandDefinition)


class TestRunner:
    def __call__(self):
        if isLogEnabled:
            log("testing as %s" % str(playerSource.getLoginDetails(grinder.threadNumber)))

        sessionDuration = grinder.properties.getInt("player.sessionDurationInSeconds", 300)
        if sessionDuration <= 0:
            raise Exception("Session duration is <= 0")
        agents = grinder.properties.getInt("yazino.agents", 1)
        if agents <= 0:
            raise Exception("Agents is <= 0")
        processesPerAgent = grinder.properties.getInt("grinder.processes", 1)
        if processesPerAgent <= 0:
            raise Exception("Processes per agent is <= 0")
        threadsPerProcess = grinder.properties.getInt("grinder.threads", 1)
        if threadsPerProcess <= 0:
            raise Exception("Threads per process is <= 0")
        playersOnline = grinder.properties.getInt("yazino.playersOnline", 1)
        if playersOnline <= 0:
            raise Exception("Players online <= 0")
        if playersOnline < agents:
            raise Exception("Players online < number of agents")

        workerIndex = grinder.processNumber - grinder.firstProcessNumber
        totalThreads = agents * processesPerAgent * threadsPerProcess
        sleepInMilliseconds = int(1000 * sessionDuration * (float(totalThreads) / playersOnline - 1))
        if sleepInMilliseconds < 0:
            raise Exception("Total number of (threads * processes * agents) is smaller than specified number of players")

        delayInMilliseconds = float(1000 * sessionDuration * (grinder.agentNumber + agents * workerIndex + agents * processesPerAgent * grinder.threadNumber) / playersOnline)
        if isLogEnabled:
            log("agent = [%d] process = [%d] thread = [%d] delayInMilliseconds = [%f]" % (grinder.agentNumber, workerIndex, grinder.threadNumber, delayInMilliseconds))

        log("Pre-start: sleeping for %d ms" % (delayInMilliseconds))
        grinder.sleep(long(delayInMilliseconds), 500)
        while True:
            grinder.statistics.setDelayReports(True)
            context = ScheduledFunctionContext()
            initialTime = time()
            httpLogin(context)
            httpGoToLobby(context)
            httpDailyAward(context)
            httpRegisterForNextTournament(context)
            scheduleInvoker = getScheduleInvoker(initialTime, schedule, context)

            while time() < initialTime + sessionDuration or context.tournamentDetails:
                sleepTime = scheduleInvoker()
                log("Session in progress: sleeping for %d ms" % (sleepTime))
                grinder.sleep(sleepTime)

            if context.tableId:
                httpLeave(context.tableId, "Leave", [], context.lastReceivedIncrement)
                context.tableId = None
            if context.rabbitConnection:
                log("Closing RabbitMQ connection for lobby for player %s" % str(playerSource.getLoginDetails(grinder.threadNumber)))
                context.rabbitConnection.close()
                context.rabbitConnection = None
            if context.lobbyLightstreamerConnection:
                log("Closing LightStreamer connection for lobby for player %s" % str(playerSource.getLoginDetails(grinder.threadNumber)))
                context.lobbyLightstreamerConnection = closeLighstreamerConnection(context.lobbyLightstreamerConnection)
            if context.gameLightstreamerConnection:
                log("Closing LightStreamer connection for game for player %s" % str(playerSource.getLoginDetails(grinder.threadNumber)))
                context.gameLightstreamerConnection = closeLighstreamerConnection(context.gameLightstreamerConnection)

            context = None
            log("Pass finished: sleeping for %d ms" % (sleepInMilliseconds))
            grinder.sleep(sleepInMilliseconds)
