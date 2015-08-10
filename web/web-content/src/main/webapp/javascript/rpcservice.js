/*global window, document, jQuery, setTimeout, require, LightstreamerClient, Subscription */

var YAZINO, YAHOO;
YAZINO = YAZINO || {};

(function () {
    var counter = 0,
        createTransport;

    createTransport = (function () {
        var exchange = 'player-direct';
        return function (host, port) {
            var playerId = null,
                tableId = null,
                callbackFunctionName,
                subscribedDocumentTypes = [],
                connected = false;

            function buildRoutingKeys() {
                var routingKeys = [];
                if (playerId && tableId) {
                    routingKeys.push('PLAYERTABLE.' + playerId + '.' + tableId);
                }
                if (playerId) {
                    routingKeys.push('PLAYER.' + playerId);
                }
                if (tableId) {
                    routingKeys.push('TABLE.' + tableId);
                }
                return routingKeys;
            }

            function setTableId(newTableId) {
                tableId = newTableId;
            }

            function setPlayerId(newPlayerId) {
                playerId = newPlayerId;
            }

            function setCallbackFunctionName(newCallbackFunctionName) {
                callbackFunctionName = newCallbackFunctionName;
            }

            function getStatus() {
                YAZINO.logger.debug("status is ", connected);
                return {
                    isConnected: connected
                };
            }

            function connectionListener() {
                return {
                    onListenEnd: function (lsClient) {
                        YAZINO.logger.debug('RpcService: Stopped listening');
                    },
                    onListenStart: function (lsClient) {
                        YAZINO.logger.debug('RpcService: Started listening');
                    },
                    onPropertyChange: function (changedProperty) {
                    },
                    onServerError: function (errorCode, errorMessage) {
                        YAZINO.logger.error('RpcService: Error received ' + errorCode + ' : ' + errorMessage);
                    },
                    onShareAbort: function () {
                    },
                    onStatusChange: function (newStatus) {
                        if (newStatus.toString().match(/^CONNECTED:/)) {
                            connected = true;
                        } else if (newStatus.toString().match(/^DISCONNECTED:/)) {
                            connected = false;
                        }
                    }
                };
            }

            function onMessage(receivedDocumentType, body) {
                var i;

                YAZINO.logger.debug('RpcService: Received message of type ' + receivedDocumentType);

                if (!callbackFunctionName) {
                    return;
                }

                for (i = 0; i < subscribedDocumentTypes.length; i += 1) {
                    if (receivedDocumentType === subscribedDocumentTypes[i]) {
                        window[callbackFunctionName]({
                            documentType: receivedDocumentType,
                            document: body
                        });
                    }
                }
            }

            function asJSON(jsonText) {
                if (jsonText && jsonText.length > 0 && jsonText.match('^{')) {
                    return JSON.parse(jsonText);
                }
                return jsonText;
            }

            function subscribeToRoutingKeys(client, Subscription) {
                var routingKeys = buildRoutingKeys(),
                    i,
                    binding,
                    messageHandler,
                    subscription;

                messageHandler = {
                    onCommandSecondLevelItemLostUpdates: function (lostUpdates, key) {
                    },
                    onCommandSecondLevelSubscriptionError: function (code, message, key) {
                    },
                    onEndOfSnapshot: function (itemName, pos) {
                    },
                    onItemLostUpdates: function (itemName, itemPos, lostUpdates) {
                    },
                    onItemUpdate: function (updateInfo) {
                        onMessage(updateInfo.getValue("contentType"), asJSON(updateInfo.getValue("body")));
                    },
                    onListenEnd: function (subscription) {
                    },
                    onListenStart: function (subscription) {
                    },
                    onSubscription: function () {
                    },
                    onSubscriptionError: function (code, message) {
                        YAZINO.logger.debug('RpcService: Subscription error: ' + code + ' : ' + message);
                    },
                    onUnsubscription: function () {
                    }
                };

                for (i = 0; i < routingKeys.length; i += 1) {
                    binding = '/exchange/' + exchange + '/' + routingKeys[i];
                    YAZINO.logger.debug('RpcService: Subscribing to ' + binding);
                    subscription = new Subscription("RAW", routingKeys[i], ['body', 'contentType']);
                    subscription.addListener(messageHandler);
                    subscription.setDataAdapter("TABLE");
                    client.subscribe(subscription);
                }
            }

            function connect() {
                YAZINO.logger.debug("Connecting.");

                require(["LightstreamerClient", "Subscription"], function (LightstreamerClient, Subscription) {
                    var client = new LightstreamerClient(host + ':' + port, "STRATA");
                    client.connectionOptions.setEarlyWSOpenEnabled(false);
                    client.addListener(connectionListener());
                    client.connectionSharing.enableSharing("YazinoLobbyWeb", "ATTACH", "CREATE");
                    client.connect();

                    subscribeToRoutingKeys(client, Subscription);
                });
            }

            function subscribeToDocument(documentType) {
                subscribedDocumentTypes.push(documentType);
            }

            function sendCommand(methodName, args) {
                var commandUrl;
                if (window.location.protocol === 'https:') {
                    commandUrl = YAZINO.configuration.get('secureCommandUrl');
                } else {
                    commandUrl = YAZINO.configuration.get('commandUrl');
                }
                jQuery.ajax({
                    url: commandUrl + '/' + methodName,
                    datatype: 'html',
                    async: false,
                    type: 'POST',
                    data: args.join('|')
                });
            }

            return {
                setTableId: setTableId,
                setPlayerId: setPlayerId,
                getStatus: getStatus,
                connect: connect,
                subscribeToDocument: subscribeToDocument,
                sendCommand: sendCommand,
                setCallbackFunctionName: setCallbackFunctionName
            };
        };
    }());

    YAZINO.RpcService = /** @constructor */function () {
        var that = this, transport, callbackFunctionName, readyPollerInterval, addEventListener;
        counter += 1;
        callbackFunctionName = "rpcServiceCallback" + new Date().getTime();
        YAZINO.EventDispatcher.apply(this);
        readyPollerInterval = window.setInterval(
            function () {
                var eventType, listenersByEventType;
                listenersByEventType = that.getListenersByEventType();

                if (!transport) {
                    YAZINO.logger.debug("RpcService: no transport is present.");
                    return;
                }
                if (that.getStatus().isConnected) {
                    YAZINO.logger.debug("RpcService: Transport is connected, subscribing.");

                    transport.setCallbackFunctionName(callbackFunctionName);
                    window[callbackFunctionName] = function (event) {
                        event.eventType = event.documentType;
                        that.dispatchEvent(event);
                    };
                    for (eventType in listenersByEventType) {
                        if (listenersByEventType.hasOwnProperty(eventType)) {
                            transport.subscribeToDocument(eventType);
                        }
                    }
                    that.dispatchEvent({
                        eventType: "RpcServiceStatusChanged",
                        isConnected: true
                    });
                    window.clearInterval(readyPollerInterval);
                }
            },
            1000
        );
        this.getStatus = function () {
            return transport && transport.getStatus ? transport.getStatus() : { isConnected: false };
        };
        addEventListener = this.addEventListener;
        this.addEventListener = function (eventType, listener) {
            addEventListener(eventType, listener);
            if (that.getStatus().isConnected) {
                transport.subscribeToDocument(eventType);
            }
        };
        this.send = function (methodName, args) {
            YAZINO.logger.info("EventDispatcher.send methodName = [%s] args = [%s]", methodName, args);
            transport.sendCommand(methodName, args);
        };
        if (!YAZINO.configuration.get('playerId')) {
            YAZINO.logger.debug("RpcService: No player, load stopped");
            return;
        }
        jQuery(document).ready(function () {
            YAZINO.logger.debug("RpcService: loading transport");

            transport = createTransport(YAZINO.configuration.get('lightstreamerSecureHost'),
                YAZINO.configuration.get('lightstreamerSecurePort'));
            transport.setPlayerId(YAZINO.configuration.get('playerId'));
            transport.connect();
        });
    };

    YAZINO.createRpcService = function () {
        if (!YAZINO.configuration.get('onCanvas') && window.top !== window && window.top.YAZINO && window.top.YAZINO.rpcService) {
            YAZINO.logger.debug("RpcService: Parent transport is present");
            return window.top.YAZINO.rpcService;
        }

        YAZINO.logger.debug("RpcService: Creating new transport");
        return new YAZINO.RpcService();
    };
}());
