/*global window, parent, document, setTimeout, FB, jQuery, swfobject, buyChips, BrowserDetect */

//TODO: Remove buyChips from global scope - I can't see it defined anywhere.

var YAZINO, YAHOO;
YAZINO = YAZINO || {};
YAHOO = YAHOO || {};
YAZINO.actions = {};
//TODO: remove these global functions

function showPaymentsCloseButton() {
    var closeButton = jQuery("#payments_closeButton");
    closeButton.show();
}

function hidePaymentsCloseButton() {
    var closeButton = jQuery("#payments_closeButton");
    closeButton.hide();
}

function hidePaymentsOverlay() {
    var mask, closeButton;
    hidePaymentsCloseButton();
    mask = jQuery("#payments_overlay");
    mask.hide();
    jQuery("#games-area").removeClass('web-4147-hack'); // workaround for WEB-4147
}

function clearPaymentsOverlay() {
    var mask = jQuery("#payments_overlay");
    if (jQuery("#payments_iframe").length > 0) {
        jQuery("#payments_iframe").first().remove();
    }
    mask.hide();
    jQuery("#games-area").removeClass('web-4147-hack'); // workaround for WEB-4147
}

window.showPaymentsOverlay = function (earnChips) {
    var mask, paymentsIframe, closeButton,
        context = window.location.href.indexOf('/welcome') === -1 ? YAZINO.configuration.get('gameType') : 'welcome_page',
        payment = "",
        gameType = "?gameType=" + YAZINO.configuration.get("gameType");

    if (BrowserDetect.browser === 'Firefox' && BrowserDetect.OS === 'Windows') {
        jQuery("#games-area").addClass('web-4147-hack'); // workaround for WEB-4147
    }

    mask = jQuery("#payments_overlay");
    if (jQuery("#payments_iframe").length > 0) {
        jQuery("#payments_iframe").first().remove();
    }
    mask.show();
    if (earnChips) {
        payment = "&paymentMethodType=TRIALPAY";
    }
    paymentsIframe = jQuery("<iframe frameborder='no' scrolling='no' id='payments_iframe' src='/lobbyPartials/cashier" + gameType + payment + "'></iframe>");
    closeButton = jQuery("#payments_closeButton");
    showPaymentsCloseButton();
    mask.prepend(paymentsIframe);
    closeButton.click(function () {
        paymentsIframe.remove();
        closeButton.hide();
        mask.hide();
        jQuery("#games-area").removeClass('web-4147-hack'); // workaround for WEB-4147
    });
};

function showPaymentsOverlayAt(paymentType) {
    window.selectedPaymentType = paymentType;
    window.showPaymentsOverlay('unknown');
}

YAZINO.getLocalStorageInstance = (function () {
    var namespacesUsedOrReserved = [];
    return function (namespace, lockNamespace) {
        var localStorage = {},
            indexKey = '[index]',
            index = (function () {
                var seperator = ',';

                function getKeys() {
                    var currentIndexValue = localStorage.getItem(indexKey) || "";
                    return currentIndexValue.split(seperator);
                }

                function putKeys(newKeys) {
                    var sanitisedKeyList = [],
                        i;
                    for (i in newKeys) {
                        if (newKeys.hasOwnProperty(i) && newKeys[i]) {
                            sanitisedKeyList.push(newKeys[i]);
                        }
                    }
                    localStorage.setItem(indexKey, sanitisedKeyList.join(seperator));
                }

                function prepareKeyName(keyName) {
                    return keyName;
                }

                return {
                    addKey: function (keyName) {
                        var keys = getKeys(),
                            key = prepareKeyName(keyName);
                        if (key === indexKey || !key) {
                            return;
                        }
                        if (keys.indexOf(key) === -1) {
                            keys.push(key);
                        }
                        putKeys(keys);
                    },
                    removeKey: function (keyName) {
                        var keys = getKeys(),
                            key = prepareKeyName(keyName),
                            positionOfKey = keys.indexOf(key);
                        if (key === indexKey) {
                            return;
                        }
                        if (positionOfKey !== -1) {
                            delete keys[positionOfKey];
                        }
                        putKeys(keys);
                    },
                    getKeys: getKeys
                };
            }());
        if (!namespace) {
            throw "no namespace given, can't work with that!";
        }
        if (namespacesUsedOrReserved.indexOf(namespace) > -1) {
            throw "duplicate namespace given, can't work with that!";
        }
        if (lockNamespace) {
            namespacesUsedOrReserved.push(namespace);
        }

        function preInsertFilter(fnName, args) {
            var key = args[0];
            if (!key) {
                throw "no key given, can't namespace that!";
            }
            if (key !== indexKey) {
                if (fnName === 'setItem') {
                    index.addKey(key);
                } else if (fnName === 'removeItem') {
                    index.removeKey(key);
                }
            }
            args[0] = "yazino." + namespace + '.' + key;
        }

        localStorage = YAZINO.generateApiWrapper(window.localStorage, ['getItem', 'setItem', 'removeItem'], preInsertFilter);
        localStorage.get = localStorage.getItem;
        localStorage.set = localStorage.setItem;
        localStorage.isAvailable = function () {
            var result, key = '[tmp]', value = 'true';
            localStorage.setItem(key, value);
            result = localStorage.getItem(key) === value;
            localStorage.removeItem(key);
            return result;
        };
        localStorage.clear = function () {
            var keys = index.getKeys(),
                i;
            for (i in keys) {
                if (keys.hasOwnProperty(i) && keys[i]) {
                    localStorage.removeItem(keys[i]);
                }
            }
        };
        return localStorage;
    };
}());

YAZINO.util = YAZINO.util || {};

jQuery.extend(YAZINO.util, {
    openTournament: function (gameType, tournamentId) {
        window.location.href = YAZINO.configuration.get('baseUrl') + "/tournament/" + tournamentId;
    },
    formatString: function (firstResult) {
        var result = firstResult, i, reg;
        for (i = 0; i < arguments.length - 1; i += 1) {
            reg = new RegExp("\\{" + i + "\\}", "gm");
            result = result.replace(reg, arguments[i + 1]);
        }
        return result;
    },
    popupWindow: function (url, name, width, height) {
        width = width || 750;
        height = height || 650;
        window.open(url, name, "width=" + width + ",height=" + height + ",scrollbars=1,resizable=1");
    },
    relocateTo: function (relativeUrl) {
        window.location.href = relativeUrl;
    },
    isParentFrame: function () {
        try {
            return parent === window || typeof parent.YAZINO === 'undefined';
        } catch (e) {
            return true;
        }
    },
    getParentFrame: function () {
        try {
            return parent === window || typeof parent.YAZINO === 'undefined' ? window : parent;
        } catch (e) {
            return window;
        }
    },
    resizeIframeLightbox: function () {
        if (!YAZINO.util.isParentFrame() && parent.YAZINO && parent.YAZINO.lightboxWidget) {
            YAZINO.logger.log('Is resizing lightbox');
            parent.YAZINO.lightboxWidget.adjustHeights(jQuery('body').height());
        } else {
            YAZINO.logger.log('Isn\'t resizing lightbox', YAZINO.util.isParentFrame(), parent.YAZINO, parent.YAZINO.lightboxWidget);
        }
    },
    formatCurrency: function (iAmount, iDecimalPlaces, sDecimalSeparator, sThousandSeparator,  sPrefix, sPostfix) {
        iDecimalPlaces = isNaN(iDecimalPlaces = Math.abs(iDecimalPlaces)) ? 2 : iDecimalPlaces;
        sDecimalSeparator = sDecimalSeparator || '.';
        sThousandSeparator = sThousandSeparator || ',';
        sPrefix = sPrefix || '';
        sPostfix = sPostfix || '';
        var iSign = iAmount < 0 ? '-' : '',
            sRounded = String(parseInt(iAmount = Math.abs(+iAmount || 0).toFixed(iDecimalPlaces), 10)),
            iSeparatorsReq = sRounded.length > 3 ? sRounded.length % 3 : 0;
        return sPrefix + iSign + (iSeparatorsReq ? sRounded.substr(0, iSeparatorsReq) + sThousandSeparator : '') + sRounded.substr(iSeparatorsReq).replace(/(\d{3})(?=\d)/g, '$1' + sThousandSeparator) + (iDecimalPlaces ? sDecimalSeparator + Math.abs(iAmount - sRounded).toFixed(iDecimalPlaces).slice(2) : '') + sPostfix;
    },
    trackEvent: function (category, action, optionalLabel, optionalValue) {
        if (YAZINO.configuration.get('playerName')) {
            optionalLabel = optionalLabel.replace(YAZINO.configuration.get('playerName'), '{*actor*}');
        }
        YAZINO.businessIntelligence.gaTrack(['_trackEvent', category, action, optionalLabel, optionalValue]);
    },
    // This is a temporary function that uses a second tracker.  This is because the free account does
    // not have capacity to support WEB-1507.  This could be replaced with general support for
    // named trackers, a paid account or a layer of indirection that avoids google-specific code
    // (which is planned)
    //
    // tracker2enabled is initialised in google_analytics.vm
    trackEventForGameType: YAZINO.businessIntelligence.trackEventForGameType,
    comparator: {
        alphabetical: {
            ascending: function (x, y) {
                return x < y  ? -1 : (x === y ? 0 : 1);
            },
            descending: function (x, y) {
                return YAZINO.util.comparator.alphabetical.ascending(x, y) * -1;
            }
        }
    }
});

(function (util) {
    var startNumber = Math.floor(Math.random() * 101),
        clientIdForGameType = {
            "BLACKJACK": "Red Blackjack",
            "ROULETTE": "Default Roulette",
            "TEXAS_HOLDEM": "Default Texas Holdem Poker",
            "SLOTS": "Default Slots",
            "HIGH_STAKES": "Default High Stakes"
        },
        displayNameForGameType = {
            "BLACKJACK": "Blackjack",
            "ROULETTE": "Roulette",
            "TEXAS_HOLDEM": "Texas Hold'em",
            "SLOTS": "Wheel Deal"
        };
    util.getUniqueId = function () {
        startNumber += 1;
        return startNumber;
    };
    util.getClientId = function (gameType, variationName) {
        return clientIdForGameType[gameType];
    };
    util.getGameTypeDisplayName = function (gameType) {
        return displayNameForGameType[gameType];
    };
    util.url = (function () {
        function parseUrl(url) {
            var endOfUrnSchemePosition,
                result = {
                    scheme: undefined,
                    uri: undefined
                };
            if (!url) {
                return result;
            }
            endOfUrnSchemePosition = url.indexOf(':');
            if (endOfUrnSchemePosition === -1) {
                return null;
            }
            result.scheme = url.substr(0, endOfUrnSchemePosition);
            result.uri = url.substr(endOfUrnSchemePosition + 1);
            return result;
        }
        return {
            parse: parseUrl
        };
    }());
}(YAZINO.util));

YAZINO.PublicApi = /** @constructor */function () {
    var launchUrl, self = this;
    launchUrl = function (url, windowId, source) {
        var form, hiddenField;
        YAZINO.logger.log("Opening inline: " + url);
        window.location.href = url;
    };
    this.launchTableByVariation = function (gameType, variationName, src) {
        var windowUrl = YAZINO.util.formatString(
            "/table/find/{1}/{2}/{3}",
            YAZINO.configuration.get('baseUrl'),
            gameType,
            variationName.replace(/ /g, '_'),
            YAZINO.util.getClientId(gameType, variationName).replace(/ /g, '_')
        );
        launchUrl(windowUrl, gameType + YAZINO.util.getUniqueId(), src);
    };
    this.launchTableById = function (gameType, tableId, src) {
        var windowUrl = YAZINO.util.formatString(
            "/table/{1}",
            YAZINO.configuration.get('baseUrl'),
            tableId
        );
        launchUrl(windowUrl, gameType, src);
    };
    this.launchTableSimilarTo = function (gameType, tableId, src) {
        var windowUrl = YAZINO.util.formatString(
            "/table/like/{1}",
            YAZINO.configuration.get('baseUrl'),
            tableId
        );
        launchUrl(windowUrl, gameType + new Date().getTime(), src);
    };
};
YAZINO.publicApi = new YAZINO.PublicApi();

YAZINO.PlainStorageService = function () {
    var storage = jQuery.browser.msie ? jQuery.Cookies : jQuery.Storage; //localStorage behaves differently in MS IE 8 and 9, and this affects autopublishing to FB
    this.get = function (propertyName) {
        return storage.get(propertyName) || "";
    };
    this.set = function (propertyName, propertyValue) {
        storage.set(propertyName, propertyValue.toString());
    };
};

YAZINO.InviteFriendsService = function (storage, modalDialogueService) {
    var self = this,
        threeDays = 3 * 24 * 60 * 60 * 1000,
        timeLastUsedKey = "lastAutoDisplayed",
        isNewPlayer = jQuery.cookie('isNewPlayer') === 'true';
    YAZINO.EventDispatcher.apply(this);
    modalDialogueService.addEventListener("InviteFriendsPopupShown", function (event) {
        self.dispatchEvent({
            eventType: "PopupVisibilityChanged",
            isVisible: true,
            source: YAZINO.configuration.get('gameType') || "AUTOMATIC"
        });
    });
    modalDialogueService.addEventListener("InviteFriendsPopupHidden",
        this.dispatchEvent.curry(this, { eventType: "PopupVisibilityChanged", isVisible: false }));
    if (isNewPlayer) {
        storage.set(timeLastUsedKey, new Date().getTime());
        jQuery.cookie('isNewPlayer', null);
    }
    this.triggerPopup = function () {
        modalDialogueService.requestDialogue("InviteFriendsPopup");
    };
    this.hidePopup = modalDialogueService.dismissDialogue;
    this.triggerPopupIfNotTriggeredRecently = function () {
        var now = new Date().getTime();
        if (YAZINO.configuration.get('playerId') && storage.isAvailable() && (now > parseInt(storage.get(timeLastUsedKey) || 0, 10) + threeDays)) {
            storage.set(timeLastUsedKey, now);
            YAZINO.action.run('inviteFriends', 'auto');
        }
    };
};

YAZINO.createInviteFriendsReminderService = function (inviteFriendsService) {
    var result = {};

    YAZINO.EventDispatcher.apply(result);
    result.sendReminder = function (recipientId, source) {
        if (source.toLowerCase() === 'facebook') {
            // Proof of concept
            var requestCallback = function (response) {

                var redirectUrl = "/friends/acknowledgeInvitedViaFacebook",
                    request_ids;

                if (!response) {
                    return;
                }

                if (response && response.request_ids) {
                    redirectUrl += "?request_ids=" + response.request_ids;
                    redirectUrl += "&source=" + source;
                } else if (response && response.request) {
                    request_ids = response.to;
                    redirectUrl += "?request_ids=" + request_ids;
                    redirectUrl += "&source=" + source;
                } else {
                    YAZINO.logger.warn("Issue with response, got back: %s", response || "");
                    return;
                }

                jQuery.ajax({
                    url: redirectUrl,
                    type: "GET"
                }).done(function () {
                    YAZINO.inviteFriendsReminderSent(recipientId, "facebook");
                }).fail(function (error) {
                    YAZINO.logger.warn('Unable to send invite reminder: ' + error);
                });

            };
            FB.ui({
                method: 'apprequests',
                display: 'iframe',
                filters: "['app_non_users']",
                to: recipientId,
                message: "I'm waiting for you! Come play live social casino games now.",
                title: 'Remind your friends to join the fun at Yazino'
            }, requestCallback);
            // End proof of concept


        } else if (source.toLowerCase() === 'email') {
            jQuery.ajax({
                url: "/friends/sendInvitationReminder",
                type: "POST",
                accepts: {
                    json: "application/json"
                },
                dataType: "json",
                data: {
                    recipientId: recipientId
                }
            }).done(function () {
                result.reminderSent(recipientId, source);
            }).fail(function (error) {
                YAZINO.logger.warn('Unable to send invite reminder: ' + error);
            });
        } else {
            throw "Unsupported source: '" + source + "'.";
        }
    };
    result.reminderSent = function (recipientId, source) {
        result.dispatchEvent({
            eventType: "reminderSentEvent." + recipientId + "." + source
        });
    };
    return result;
};

YAZINO.FacebookOpenGraphService = /** @constructor */function () {
    var url = "/opengraph/credentials",
        isCurrent = function (accessToken) {
            return accessToken && accessToken === YAZINO.configuration.get('facebookAccessToken');
        },
        setAccessToken = function (accessToken) {
            if (!YAZINO.configuration.get('playerId') || YAZINO.configuration.get('playerId') === "") {
                return;
            }
            if (accessToken !== null && !isCurrent(accessToken)) {
                jQuery.ajax({
                    url: url,
                    type: "POST",
                    dataType: "json",
                    accepts: {
                        json: "application/json"
                    },
                    data: {
                        accessToken: accessToken,
                        gameType: YAZINO.configuration.get('gameType'),
                        playerId: YAZINO.configuration.get('playerId')
                    }
                }).done()
                    .fail(function () {
                        YAZINO.logger.error("Failed to POST to: " + url);
                    });

            }
        };
    this.updateLoginStatus = function (fbApi) {
        fbApi.getLoginStatus(function (response) {
            if (response.authResponse) {
                var accessToken = response.authResponse.accessToken;
                YAZINO.logger.log("FB Auth token " + accessToken + " expires: " + response.authResponse.expiresIn);
                setAccessToken(accessToken);
            }
        });
    };
};

YAZINO.createModalDialogueService = function () {
    var result = {}, queue = [],
        dispatchDialogueEvent = function (dialogue, suffix) {
            result.dispatchEvent({ eventType: dialogue.dialogueType + suffix});
        };
    YAZINO.EventDispatcher.apply(result);
    result.requestDialogue = function (dialogueType) {
        var dialogue = {dialogueType: dialogueType};
        if (!queue.length) {
            dispatchDialogueEvent(dialogue, "Shown");
        }
        queue.push(dialogue);
    };
    result.dismissDialogue = function () {
        var dialogue = queue.shift();
        if (dialogue) {
            dispatchDialogueEvent(dialogue, "Hidden");
            if (queue.length) {
                dispatchDialogueEvent(queue[0], "Shown");
            }
        }
    };
    return result;
};

YAZINO.createNewsEventService = function () {
    var result = new YAZINO.EventDispatcher();
    YAZINO.rpcService.addEventListener("NEWS_EVENT", function (event) {
        var document = event.document;
        setTimeout(
            function () {
                result.dispatchEvent({
                    eventType: "NewsReceived",
                    newsType: document.type,
                    title: document.title,
                    shortMessage: document.shortDescription ? document.shortDescription.message : null,
                    message: document.news.message,
                    image: document.image,
                    gameType: document.gameType
                });
            },
            document.delay
        );
    });
    return result;
};

(function (YAZINO) {

    YAZINO.generatePromise = function () {

        var resolvedValue,
            hasResolved = false,
            wasSuccess = false,
            successCallbacks = [],
            failureCallbacks = [];

        function issueCallbacks(callbackStack) {
            var currentCallback;
            currentCallback = callbackStack.pop();
            while (currentCallback) {
                currentCallback(resolvedValue);
                currentCallback = callbackStack.pop();
            }
        }

        function issueSuccesses() {
            issueCallbacks(successCallbacks);
        }

        function issueFailures() {
            issueCallbacks(failureCallbacks);
        }

        function triggerCallbacks() {
            if (hasResolved) {
                if (wasSuccess) {
                    issueSuccesses();
                } else {
                    issueFailures();
                }
            }
        }

        function then(success, failure) {
            if (success) {
                successCallbacks[successCallbacks.length] = success;
            }
            if (failure) {
                failureCallbacks[failureCallbacks.length] = failure;
            }
            triggerCallbacks();
        }

        function resolve(value) {
            resolvedValue = value;
            wasSuccess = true;
            hasResolved = true;
            triggerCallbacks();
        }

        function error(reason) {
            resolvedValue = reason;
            wasSuccess = false;
            hasResolved = true;
            triggerCallbacks();
        }

        return {
            then: then,
            resolve: resolve,
            error: error
        };
    };

}(YAZINO));

YAZINO.facebookOpenGraphService = new YAZINO.FacebookOpenGraphService();

YAZINO.generateFacebookService = function (fbOpenGraphService) {

    var eventDispatcher = new YAZINO.EventDispatcher(),
        getPlayerFacebookData,
        reset,
        init,
        facebookServicePromise = YAZINO.generatePromise(),
        fbApi;

    init = function () {
        eventDispatcher.dispatchEvent(
            {
                eventType: 'FacebookServiceReady',
                ready: true
            }
        );
        fbApi = FB;
        facebookServicePromise.resolve(fbApi);
        fbOpenGraphService.updateLoginStatus(fbApi);
    };

    getPlayerFacebookData = (function () {

        var meData = YAZINO.generatePromise(),
            fbPlayerFacebookDataHasBeenCalled = false;

        return function () {
            if (!fbPlayerFacebookDataHasBeenCalled) {
                fbPlayerFacebookDataHasBeenCalled = true;
                facebookServicePromise.then(function (fb) {
                    fb.api("/me", function (response) {
                        meData.resolve(response);
                    });
                });
            }
            return meData;
        };
    }());

    return {
        getPlayerFacebookData: getPlayerFacebookData,
        addEventListener: eventDispatcher.addEventListener,
        init: init
    };
};


YAZINO.facebookService = YAZINO.generateFacebookService(YAZINO.facebookOpenGraphService);

YAZINO.createNotificationService = function (newsEventService, facebookPostingService) {
    var result = new YAZINO.EventDispatcher(),
        eventsAutoPostedToFacebook = {"ACHIEVEMENT": "", "LEVEL": "", "TROPHY": ""},
        actions = {
            openTable: function (event) {
                window.StrataPartnerApi.launchTable(event.message, "invite");
            },
            postNews: function (event) {
                var playerName, gameTypeDisplayName;
                YAZINO.facebookService.getPlayerFacebookData().then(
                    function (playerFacebookData) {
                        setTimeout(facebookPostingService.post.curry(this,
                            event,
                            eventsAutoPostedToFacebook.hasOwnProperty(event.newsType),
                            playerFacebookData),
                            Math.floor(Math.random() * 500));
                    }
                );
            }
        },
        dispatchEventWithAction = function (event, duration, action) {
            result.dispatchEvent({
                eventType: "NotificationEvent",
                title: event.title,
                displayMessage: event.shortMessage || event.message,
                duration: duration,
                action: action
            });
        },
        handleOpenTableEvent = function (event) {
            dispatchEventWithAction(event, 30, {
                name: "Click to play.",
                handler: function () {
                    actions.openTable(event);
                }
            });
        },
        handleSimplePostingEvent = function (event, duration) {
            if (window.StrataPartnerApi.isAutoPosting() && (eventsAutoPostedToFacebook.hasOwnProperty(event.newsType))) {
                actions.postNews(event);
                dispatchEventWithAction(event, duration, {
                    name: "",
                    handler: function () {
                    }
                });
            } else {
                dispatchEventWithAction(event, duration, {
                    name: "Click to post.",
                    handler: function () {
                        actions.postNews(event);
                    }
                });
            }
        },
        handleNonStickyPostingEvent = function (event) {
            handleSimplePostingEvent(event, 30);
        },
        handleStickyPostingEvent = function (event) {
            handleSimplePostingEvent(event, 0);
        },
        newsTypeHandlers = {
            "NEWS": handleNonStickyPostingEvent,
            "ACHIEVEMENT": handleNonStickyPostingEvent,
            "LEVEL": handleNonStickyPostingEvent,
            "TABLE_INVITE": handleOpenTableEvent,
            "TROPHY": handleStickyPostingEvent
        };
    result.postToFacebook = actions.postNews;
    newsEventService.addEventListener("NewsReceived", function (event) {
        newsTypeHandlers[event.newsType](event);
    });
    return result;
};

YAZINO.checkFlash = function () {
    if ((!swfobject.getFlashPlayerVersion() || swfobject.getFlashPlayerVersion().major === 0)) {
        YAZINO.notificationService.dispatchEvent({
            eventType: "NotificationEvent",
            displayMessage: "You need to install Flash player to use this site.",
            duration: 0,
            action: {
                name: "Click to install.",
                handler: function () {
                    var url = "http://get.adobe.com/flashplayer/",
                        options = "toolbar=yes,menubar=yes,location=yes,scrollbars=yes,resizable=yes,width=1004,height=652";
                    window.open(url, "getFlashPlayer", options);
                }
            }
        });
    }
};

YAZINO.PlayerService = /** @constructor */function () {
    var that = this, publishIfConnected, balance, balanceAsNumber, rpcService = YAZINO.rpcService;
    YAZINO.EventDispatcher.apply(this);
    publishIfConnected = function () {
        if (rpcService.getStatus().isConnected) {
            rpcService.send("community", ["publish", YAZINO.configuration.get('gameType')]);
            jQuery.ajax({
                url: "/social/friendsSummary?ts=" + (new Date().getTime()),
                type: "GET",
                dataType: "json",
                success: function (data) {
                    //ignore
                }
            });
        }
    };
    publishIfConnected();
    rpcService.addEventListener("RpcServiceStatusChanged", function (event) {
        publishIfConnected();
    });
    rpcService.addEventListener("PLAYER_BALANCE", function (event) {
        var tmpBalance,
            maxRenderableNumberLength = 1000000000000,
            adjustedBalanceNotation = "";
        balanceAsNumber = parseInt(event.document.balance, 10);
        tmpBalance = balanceAsNumber;
        if (tmpBalance > maxRenderableNumberLength) {
            adjustedBalanceNotation = 'k';
            tmpBalance = Math.round(tmpBalance / 1000);
        }
        balance = YAZINO.util.formatCurrency(tmpBalance, 0, '.', ',') + adjustedBalanceNotation;
        that.dispatchEvent({
            eventType: "BalanceChanged",
            balance: balance,
            balanceAsNumber: balanceAsNumber
        });
    });
    rpcService.addEventListener("PLAYER_XP", function (event) {
        that.dispatchEvent({
            eventType: "ExperienceChanged",
            gameType: event.document.gameType,
            level: event.document.level,
            points: event.document.points,
            toNextLevel: event.document.toNextLevel
        });
    });
    rpcService.addEventListener("TROPHY_STATUS", function (event) {
        that.dispatchEvent({
            eventType: "CollectibleChanged",
            collectibleType: "trophy",
            amount: event.document
        });
    });
    rpcService.addEventListener("MEDAL_STATUS", function (event) {
        that.dispatchEvent({
            eventType: "CollectibleChanged",
            collectibleType: "medal",
            amount: event.document
        });
    });
    rpcService.addEventListener("ACHIEVEMENT_STATUS", function (event) {
        that.dispatchEvent({
            eventType: "CollectibleChanged",
            collectibleType: "achievement",
            amount: event.document.achievements,
            total: event.document.totalAchievements
        });
    });
    rpcService.addEventListener("TABLE_INVITES", function (event) {
        that.dispatchEvent({
            eventType: "TableInvitesChanged",
            data: event.document
        });
    });
    rpcService.addEventListener("FRIENDS_SUMMARY", function (event) {
        that.dispatchEvent({
            eventType: "FriendsSummaryChanged",
            summary: event.document.summary
        });
    });
    this.getBalance = function () {
        return balance;
    };
    this.getBalanceAsNumber = function () {
        return balanceAsNumber;
    };
};

YAZINO.PlayerRelationshipsService = /** @constructor */function (levelingService) {
    var that = this,
        relationshipDetailsByPlayerId = {},
        playersByPlayerId = {},
        fetchGlobalPlayerListTimer,
        tableNameRegExp = new RegExp("(.*)(Low|Medium|High)(.*)");
    YAZINO.EventDispatcher.apply(this);

    function onPlayerRelationshipAndPlayerStatus(event, isPlayerRelationship) {
        var listenersByEventType = that.getListenersByEventType(), eventType, gameType, playerId;
        if (isPlayerRelationship) {
            relationshipDetailsByPlayerId = event;
        } else {
            for (playerId in event) {
                if (event.hasOwnProperty(playerId)) {
                    relationshipDetailsByPlayerId[playerId] = {
                        status: event[playerId],
                        nickname: event[playerId].nickname,
                        levels: event[playerId].levels,
                        relationshipType: "FRIEND"
                    };
                }
            }
        }
        that.dispatchEvent({
            eventType: "FriendsRelationshipsChanged",
            data: that.getOnlineFriendsDetails()
        });
        that.dispatchEvent({
            eventType: "FriendRequestsChanged",
            friendRequests: that.getFriendRequests()
        });
        for (eventType in listenersByEventType) {
            if (listenersByEventType.hasOwnProperty(eventType) && eventType.startsWith('FriendsRelationshipsChanged_')) {
                gameType = eventType.substring('FriendsRelationshipsChanged_'.length);
                that.dispatchEvent({
                    eventType: eventType,
                    data: that.getOnlineFriendsDetailsByGameType(gameType)
                });
            }
        }
    }

    function onGlobalPlayers(playersById) {
        var listenersByEventType = that.getListenersByEventType(), eventType, gameType;
        delete playersById[YAZINO.configuration.get('playerId')];
        playersByPlayerId = playersById;
        for (eventType in listenersByEventType) {
            if (listenersByEventType.hasOwnProperty(eventType) && eventType.startsWith('PlayersOnlineChanged_')) {
                gameType = eventType.substring('PlayersOnlineChanged_'.length);
                that.dispatchEvent({
                    eventType: eventType,
                    data: that.getOnlinePlayersByGameType(gameType)
                });
            }
        }
        that.dispatchEvent({
            eventType: "PlayersOnlineChanged",
            data: that.getOnlinePlayers(playersByPlayerId)
        });
    }

    function adjustRelationshipDetails(details, playerId) {
        details.playerId = playerId;
        jQuery.each(details.status.locations, function (index, location) {
            var locationNameTokens;
            locationNameTokens = location.locationName.match(tableNameRegExp);
            if (locationNameTokens && locationNameTokens.length === 4) {
                location.displayGameType = locationNameTokens[1].toUpperCase();
                location.displayStakeLevel = locationNameTokens[2];
            }
        });
    }

    function adjustPlayerDetails(oPlayer, sPlayerId) {
        var oCopy = {};
        jQuery.extend(true, oCopy, oPlayer);
        oPlayer.playerId = sPlayerId;
        jQuery.each(oPlayer.locations, function (index, location) {
            var locationNameTokens;
            locationNameTokens = location.locationName.match(tableNameRegExp);
            if (locationNameTokens && locationNameTokens.length === 4) {
                location.displayGameType = locationNameTokens[1].toUpperCase();
                location.displayStakeLevel = locationNameTokens[2];
            }
        });
        return oPlayer;
    }

    function filterRelationshipDetails(filterStrategy) {
        var result = [], playerId, details, filteredDetails;
        for (playerId in relationshipDetailsByPlayerId) {
            if (relationshipDetailsByPlayerId.hasOwnProperty(playerId)) {
                details = relationshipDetailsByPlayerId[playerId];
                filteredDetails = filterStrategy(details);
                if (filteredDetails) {
                    adjustRelationshipDetails(filteredDetails, playerId);
                    result.push(filteredDetails);
                }
            }
        }
        result.sort(
            function (details1, details2) {
                var nickname1 = details1.nickname.toLowerCase(),
                    nickname2 = details2.nickname.toLowerCase();
                if (nickname1 > nickname2) {
                    return 1;
                } else if (nickname1 < nickname2) {
                    return -1;
                }
                return 0;
            }
        );
        return result;
    }

    function filterPlayers(filterStrategy) {
        var result = [],
            playerId,
            player,
            filteredPlayer;
        for (playerId in playersByPlayerId) {
            if (playersByPlayerId.hasOwnProperty(playerId)) {
                player = playersByPlayerId[playerId];
                filteredPlayer = filterStrategy(player);
                if (filteredPlayer) {
                    result.push(adjustPlayerDetails(filteredPlayer, playerId));
                }
            }
        }
        return result;
    }

    function fetchGlobalPlayerList() {
        //todo - do this only if there's a listener
        jQuery.ajax({
            url: "/lobbyCommand/globalPlayers",
            type: "GET",
            dataType: "json",
            success: function (data) {
                onGlobalPlayers(data);
            }
        });
    }

    that.addEventListener("_ListenerAdded", function (event) {
        if (event.listenerType.startsWith("PlayersOnlineChanged")) {
            if (!fetchGlobalPlayerListTimer) {
                fetchGlobalPlayerList();
                fetchGlobalPlayerListTimer = window.setInterval(fetchGlobalPlayerList, 10000);
            }
        }
    });
    that.addEventListener("_ListenerRemoved", function (event) {
        if (event.listenerType.startsWith("PlayersOnlineChanged")) {
            if (fetchGlobalPlayerListTimer) {
                window.clearInterval(fetchGlobalPlayerListTimer);
                fetchGlobalPlayerListTimer = null;
            }
        }
    });
    YAZINO.rpcService.addEventListener("PLAYER_RELATIONSHIP", function (event) {
        levelingService.fetchLevels(event.document, function (eventWithLevels) {
            onPlayerRelationshipAndPlayerStatus(eventWithLevels, true);
        });
    });
    YAZINO.rpcService.addEventListener("PLAYER_STATUS", function (event) {
        levelingService.fetchLevels(event.document, function (eventWithLevels) {
            onPlayerRelationshipAndPlayerStatus(eventWithLevels, false);
        });
    });
    this.getOnlinePlayers = function () {
        return filterPlayers(
            function (player) {
                if (player.online) {
                    return jQuery.extend(true, {}, player);
                }
            }
        );
    };
    this.getOnlineFriendsDetails = function () {
        return filterRelationshipDetails(
            function (details) {
                if (details.relationshipType === "FRIEND" && details.status.online && details.status.locations.length) {
                    return jQuery.extend(true, {}, details);
                }
            }
        );
    };
    this.getAllOnlineFriendDetails = function () {
        return filterRelationshipDetails(
            function (details) {
                YAZINO.logger.log('getting online friends', details);
                if (details.relationshipType === "FRIEND" && details.status.online) {
                    return jQuery.extend(true, {}, details);
                }
            }
        );
    };
    this.getOfflineFriendsDetails = function () {
        return filterRelationshipDetails(
            function (details) {
                YAZINO.logger.log('getting offline friends', details);
                if (details.relationshipType === "FRIEND" && !details.status.online) {
                    return jQuery.extend(true, {}, details);
                }
            }
        );
    };
    this.getAllFriendsDetails = function () {
        return filterRelationshipDetails(
            function (details) {
                YAZINO.logger.log('getting all friends', details);
                if (details.relationshipType === "FRIEND") {
                    YAZINO.logger.log('including');
                    return jQuery.extend(true, {}, details);
                } else {
                    YAZINO.logger.log('excluding');
                }
            }
        );
    };
    this.getOnlineFriendsDetailsByGameType = function (gameType) {
        return filterRelationshipDetails(
            function (details) {
                if (details.relationshipType === "FRIEND" && details.status.online) {
                    var result = jQuery.extend(true, {}, details), oldLocations = details.status.locations, i;
                    result.status.locations = [];
                    for (i = 0; i < oldLocations.length; i += 1) {
                        if (oldLocations[i].gameType === gameType) {
                            result.status.locations.push(oldLocations[i]);
                        }
                    }
                    if (result.status.locations.length) {
                        return result;
                    }
                }
            }
        );
    };
    this.getOnlinePlayersByGameType = function (gameType) {
        return filterPlayers(
            function (oPlayer) {
                if (oPlayer.online) {
                    var result = jQuery.extend(true, {}, oPlayer),
                        oldLocations = oPlayer.locations,
                        i;
                    result.locations = [];
                    for (i = 0; i < oldLocations.length; i += 1) {
                        if (oldLocations[i].gameType === gameType) {
                            result.locations.push(oldLocations[i]);
                        }
                    }
                    if (result.locations.length) {
                        return result;
                    }
                }
            }
        );
    };
    this.getFriendRequests = function () {
        return filterRelationshipDetails(
            function (details) {
                if (details.relationshipType === "INVITATION_RECEIVED") {
                    return jQuery.extend(true, {}, details);
                }
            }
        );
    };
    this.acceptFriendRequest = function (playerId) {
        YAZINO.rpcService.send("community", ["request", playerId, "ACCEPT_FRIEND"]);
    };
    this.rejectFriendRequest = function (playerId) {
        YAZINO.rpcService.send("community", ["request", playerId, "REJECT_FRIEND"]);
    };
    this.unfriendRequest = function (buddyId) {
        YAZINO.rpcService.send("community", ["request", buddyId, "REMOVE_FRIEND"]);
    };
};

YAZINO.createLevelingService = function () {
    var result = {},
        playerLevels = {},
        findMissingLevels,
        findMissingLevelsInLocations,
        fetchMissingLevels,
        processMissingLevels,
        addPlayerLevels,
        commandUrl,
        createPlayersMissingLevel = function () {
            var result = {},
                playersMissingLevels = {};
            result.addPlayer = function (gameType, playerId) {
                if (!playersMissingLevels[gameType]) {
                    playersMissingLevels[gameType] = [];
                }
                playersMissingLevels[gameType].push(playerId);
            };
            result.eachGameType = function (callback) {
                var gameType;
                for (gameType in playersMissingLevels) {
                    callback(gameType, playersMissingLevels[gameType]);
                }
            };
            return result;
        };
    findMissingLevelsInLocations = function (missingLevels, playerId, locations) {
        var i = 0;
        for (i = 0; i < locations.length; i += 1) {
            if (!playerLevels[playerId] || !playerLevels[playerId][locations[i].gameType]) {
                missingLevels.addPlayer(locations[i].gameType, playerId);
            }
        }
    };
    findMissingLevels = function (data) {
        var playerId,
            missingLevels = createPlayersMissingLevel();
        for (playerId in data) {
            findMissingLevelsInLocations(missingLevels, playerId, data[playerId].locations || []);
            findMissingLevelsInLocations(missingLevels, playerId, (data[playerId].status && data[playerId].status.locations) || []);
        }
        return missingLevels;
    };
    commandUrl = function () {
        if (window.location.protocol === 'https:') {
            return YAZINO.configuration.get('secureCommandUrl');
        }
        return YAZINO.configuration.get('commandUrl');
    };
    fetchMissingLevels = function (gameType, playerIds) {
        var request = {
            async: false,
            type: "GET",
            dataType: "json",
            url: commandUrl() + "/pictureAndLevel",
            data: {
                gameType: gameType,
                playerIds: playerIds.join(",")
            },
            success: function (data) {
                processMissingLevels(gameType, playerIds, data);
            }
        };
        jQuery.ajax(request);
    };
    processMissingLevels = function (gameType, playerIds, data) {
        var i = 0, levels;
        for (i = 0; i < data.length; i += 1) {
            levels = playerLevels[playerIds[i]] || {};
            levels[gameType] = data[i].level;
            playerLevels[playerIds[i]] = levels;
        }
    };
    addPlayerLevels = function (data) {
        var playerId;
        for (playerId in data) {
            data[playerId].levels = playerLevels[playerId];
        }
    };
    result.fetchLevels = function (document, callback) {
        var missingLevels = findMissingLevels(document);
        missingLevels.eachGameType(fetchMissingLevels);
        addPlayerLevels(document);
        callback(document);
    };
    return result;
};

YAZINO.TableService = /** @constructor */function (configIn) {
    var that = this, models = {
        "true": {},
        "false": {}
    },
        playerService = YAZINO.playerService,
        config = configIn || {};

    YAZINO.EventDispatcher.apply(this);
    this.launchTableByVariation = YAZINO.publicApi.launchTableByVariation;
    this.launchTableById = YAZINO.publicApi.launchTableById;
    this.getTableLauncherWidgetModel = function (gameType, isPrivateTable) {
        isPrivateTable = !!isPrivateTable;
        var Model = /** @constructor */function () {
            var model = this,
                isInitialBalance = true,
                stakeIndex = 0,
                templateName,
                stakes = ["Low", "Medium", "High"],
                stakeNames = { Low: 0, Medium: 1, High: 2 },
                speedNames = { normal: "", fast: "Fast" },
                speedName = "",
                requiredBalance = config.minimumStakes || [],
                minimumBalanceFactor = config.minimumBalanceFactor || 0,
                description = config.variationDescriptions || [],
                defaultTemplateName = config.defaultTemplateName || [],
                variants = config.variants || [],
                inverseVariants = config.inverseVariants || [];
            YAZINO.EventDispatcher.apply(this);

            //TODO move this in DOM
            if (!isPrivateTable) {
                playerService.addEventListener("BalanceChanged", function (event) {
                    var i;
                    if (requiredBalance[stakeIndex] > event.balanceAsNumber || isInitialBalance) {
                        isInitialBalance = false;
                        for (i = requiredBalance.length - 1; i >= 0; i -= 1) {
                            if (event.balanceAsNumber >= minimumBalanceFactor * requiredBalance[i] || i === 0) {
                                model.setStakeIndex(i);
                                break;
                            }
                        }
                    }
                });
            }
            this.setStakeIndex = function (value) {
                stakeIndex = value;
                model.dispatchEvent({ eventType: "StakeChanged", details: model.getStakeDetails()});
            };
            this.getStakeDetails = function () {
                return {
                    stakeIndex: stakeIndex,
                    stakesLength: stakes.length,
                    lowerLimit: requiredBalance[stakeIndex],
                    description: description[stakeIndex]
                };
            };
            this.getNumberOfStakes = function (gameType) {
                return description.length;
            };
            this.setStake = function (index) {
                model.setStakeIndex(index);
            };
            this.changeSpeed = function (speed) {
                if (speedName !== speedNames[speed]) {
                    speedName = speedName ? "" : "Fast";
                    model.dispatchEvent({ eventType: "SpeedChanged", speedName: speedName, speed: speed });
                }
            };
            this.resetTemplateName = function () {
                this.setTemplateName(defaultTemplateName);
            };
            this.setTemplateName = function (value) {
                if (templateName !== value && value) {
                    templateName = value;
                    model.dispatchEvent({ eventType: "TemplateNameChanged", variantId: inverseVariants[value] });
                }
            };
            this.getTemplateName = function () {
                return templateName;
            };
            this.getTemplateNameFor = function (variantId) {
                return variants[variantId];
            };
            this.getGameVariationTemplateName = function () {
                return templateName + " " + (speedName ? speedName + " " : "") + stakes[stakeIndex];
            };
            this.setStakeAndTemplateNameFromGameVariationTemplateName = function (gameVariation) {
                var tableNameTokens = gameVariation.match(new RegExp("(.*)(Low|Medium|High)(.*)"));
                this.setStakeIndex(stakeNames[tableNameTokens[2]]);
                this.setTemplateName(tableNameTokens[1]);
            };
            this.playNow = function () {
                YAZINO.util.trackEventForGameType("lobby", "play-now-clicked", gameType);
                that.launchTableByVariation(gameType, model.getGameVariationTemplateName(), "lobbyPlayNow");
            };
            this.resetTemplateName();
        };
        if (!models[isPrivateTable][gameType]) {
            models[isPrivateTable][gameType] = new Model();
        }
        return models[isPrivateTable][gameType];
    };
    this.getPrivateTableWidgetModel = function (gameType) {
        var result, tableId;
        result = that.getTableLauncherWidgetModel(gameType, true);
        result.setTableId = function (value) {
            tableId = value;
        };
        result.getTableId = function () {
            return tableId;
        };
        result.playTable = function () {
            that.launchTableById(gameType, tableId, "lobbyPlayNow");
        };
        result.setConfig = function (config) {
            YAZINO.logger.log('Accepted new config ', config);
        };
        return result;
    };
};

YAZINO.LobbyService = /** @constructor */function () {
    var that = this, lobbyInformationPoller, fetchLobbyInformation;
    YAZINO.EventDispatcher.apply(this);
    fetchLobbyInformation = function (gameType) {
        var data = (gameType !== undefined && gameType.length > 0) ? { gameType: gameType } : {};
        jQuery.ajax({
            url: YAZINO.configuration.get('lobbyInformationUrl'),
            type: "POST",
            dataType: "json",
            data: gameType ? { gameType: gameType } : {},
            success: function (data) {
                YAZINO.logger.debug("LobbyService.fetchLobbyInformation onSuccess");
                if (data) {
                    that.dispatchEvent({
                        eventType: "LobbyInformationChanged_" + gameType,
                        details: data
                    });
                }
            }
        });
    };
    lobbyInformationPoller = function () {
        var i, listenersByEventType, eventType, gameType;
        listenersByEventType = that.getListenersByEventType();
        for (eventType in listenersByEventType) {
            if (eventType.startsWith("LobbyInformationChanged_")) {
                gameType = eventType.substring("LobbyInformationChanged_".length);
                fetchLobbyInformation(gameType);
            }
        }
    };
    this.facebookConnectLogin = function () {
        if (typeof FB !== 'undefined') {
            FB.getLoginStatus(function (response) {
                if (response.authResponse) {
                    var form, hiddenAccessTokenField, hiddenRememberMeField, rememberMeField;
                    form = document.createElement("form");
                    form.setAttribute("method", "POST");
                    form.setAttribute("action", "/public/connectLogin");
                    hiddenAccessTokenField = document.createElement("input");
                    hiddenAccessTokenField.setAttribute("type", "hidden");
                    hiddenAccessTokenField.setAttribute("name", "access_token");
                    hiddenAccessTokenField.setAttribute("value", response.authResponse.accessToken);
                    form.appendChild(hiddenAccessTokenField);
                    hiddenRememberMeField = document.createElement("input");
                    hiddenRememberMeField.setAttribute("type", "hidden");
                    hiddenRememberMeField.setAttribute("name", "_spring_security_remember_me");
                    rememberMeField = document.getElementById('rememberMe');
                    hiddenRememberMeField.setAttribute("value", rememberMeField ? rememberMeField.checked : false);
                    form.appendChild(hiddenRememberMeField);
                    document.body.appendChild(form);
                    form.setAttribute("target", "_top");
                    form.submit();
                } else if (response.status && response.status === 'unknown') {
                    // this is a best guess and Facebook isn't given us any information
                    window.location.href = YAZINO.configuration.get('baseUrl') + '/not-allowed/' + YAZINO.configuration.get('gameType');
                }
            });
        }
    };
    window.setInterval(lobbyInformationPoller, 30000);
};

YAZINO.createFacebookPostingService = function (storageService) {
    var postsHistoryTimeWindow = 60000,
        postViaDialog = function (request, event, gameSuffix) {
            if (typeof FB !== 'undefined') {
                FB.ui(request, function (response) {
                    if (response && response.post_id) {
                        var postId = event.image || event.message;
                        YAZINO.util.trackEvent('Facebook', 'Post', postId);
                        if (gameSuffix !== "") {
                            YAZINO.util.trackEvent('Facebook', 'PostFor_' + gameSuffix, postId);
                        }
                    }

                    try {
                        YAZINO.logger.warn('Fix Me - document used for global scoping.');
                        document.loader.fbPostDialogClosed();
                    } catch (e) {
                        YAZINO.logger.log(e);
                    }
                });
            } else {
                YAZINO.warn('FB is not instansiated.');
            }
        },
        publishToFeed = function (p) {
            YAZINO.logger.log("publishing to feed: " + p.description);
            if (window.frames.publish_iframe_current.hasPublishStreamPermission) {
                window.frames.publish_iframe_current.publishToFeed(p);
            } else {
                window.frames.publish_iframe_original.publishToFeed(p);
            }
        },
        arePostsEqual = function (p0, p1) {
            if (p0.picture === p1.picture && p0.description === p1.description) {
                return true;
            } else {
                YAZINO.logger.log("the following posts are different");
                YAZINO.logger.dir(p0);
                YAZINO.logger.dir(p1);
                return false;
            }
        },
        describePost = function (post) {
            return ("{name: " + post.name + ", link: " + post.link + ", description: " + post.description + "}");
        },
        sendPost = function (post) {
            var addToPostsHistory = function (p, posts) {
                    var str,
                        serializePosts  = function (data) {
                            return JSON.stringify(data);
                        },
                        copy = {description: p.description, picture: p.picture, date: p.date};

                    YAZINO.logger.log(copy);

                    posts.splice(0, 0, copy);

                    str = serializePosts(posts);
                    storageService.set("yzposts", str);
                },
                publishPostIfNotSent = function (str, p) {
                    var sent = false,
                        posts,
                        i,
                        trimPosts = function (posts) {
                            var i,
                                cutoffTime = (new Date()).getTime() - postsHistoryTimeWindow;

                            for (i = posts.length - 1; i >= 0; i = i - 1) {
                                if (posts[i].date < cutoffTime) {
                                    posts.splice(i, 1);
                                }
                            }
                            return posts;
                        };
                    YAZINO.logger.log("\nyzposts: " + str + "\n");
                    if (str === null || str === "") {
                        posts = [];
                    } else {
                        posts = jQuery.parseJSON(str);
                        posts = trimPosts(posts);
                    }

                    YAZINO.logger.dir(posts);

                    for (i in posts) {
                        if (arePostsEqual(p, posts[i])) {
                            sent = true;
                            break;
                        }
                    }

                    if (!sent) {
                        YAZINO.logger.log("publishing now: " + describePost(p));
                        addToPostsHistory(p, posts);
                        publishToFeed(p);
                    } else {
                        YAZINO.logger.log("already published: " + describePost(p));
                    }
                };

            post.date = (new Date()).getTime();
            try {
                publishPostIfNotSent(storageService.get("yzposts"), post);
            } catch (e) {
                YAZINO.logger.log("error trying to publish post " + e.message);
            }
        },
        createLink = function (event) {
            if (YAZINO.configuration.get('facebookAppsEnabled')) {
                return YAZINO.configuration.get('externalLobbyUrl') + "/" + event.postedAchievementTitleLink;
            } else {
                var facebookLoginUrl = YAZINO.configuration.get('facebookLoginUrl');
                if (typeof FB !== 'undefined') {
                    facebookLoginUrl = facebookLoginUrl.replace('{0}', FB._apiKey);
                }
                facebookLoginUrl = facebookLoginUrl.replace('{1}{2}', YAZINO.configuration.get('gameType'));
                if (facebookLoginUrl.indexOf(':8080') !== -1) {
                    facebookLoginUrl = facebookLoginUrl.replace(':8080', '');
                } else if (facebookLoginUrl.indexOf(':80') !== -1) {
                    facebookLoginUrl = facebookLoginUrl.replace(':80', '');
                } else if (facebookLoginUrl.indexOf(':443') !== -1) {
                    facebookLoginUrl = facebookLoginUrl.replace(':443', '');
                }
                return facebookLoginUrl;
            }
        },
        createMessage = function (message, facebookPlayerData) {
            var nameRegex = new RegExp('[$]{name}', 'g');
            return message.replace(nameRegex, facebookPlayerData.name);
        },
        createRequest = function (event, facebookPlayerData) {
            var yazinoLink = createLink(event);
            return {
                method: 'feed',
                name: createMessage(event.postedAchievementTitleText, facebookPlayerData),
                description: createMessage(event.message, facebookPlayerData),
                link: yazinoLink,
                display: "popup",
                actions: {
                    "name": event.postedAchievementActionText,         //26 character limit
                    "link": yazinoLink
                }
            };
        },
        result = {};
    window.sendPost = sendPost;
    result.post = function (event, allowAutoPosting, facebookPlayerData) {

        if (!event.postedAchievementTitleText) {
            event.postedAchievementTitleText = "Yazino - Let's Play!";
        }
        if (!event.postedAchievementTitleLink) {
            event.postedAchievementTitleLink = '';
        }
        if (!event.postedAchievementActionText) {
            event.postedAchievementActionText = "Yazino - Let's Play!";
        }
        if (!event.postedAchievementActionLink) {
            event.postedAchievementActionLink = '';
        }
        var url = event.postedAchievementActionLink,
            fixedUrl = !url.match(/.*\/$/) ? url + "/" : url,
            actionUrl = event.image ? fixedUrl + "?sourceId=" + event.image : fixedUrl,
            request = createRequest(event, facebookPlayerData),
            postIdX = event.image || event.message,
            gameSuffix = String(YAZINO.configuration.get('gameType'));
        if (event.image && !event.relativeUrl) {
            request.picture = YAZINO.configuration.get('permanentContentUrl') + "/images/news/" + event.image + ".png";
        } else if (event.image && event.image.length > 0 && (event.relativeUrl || event.image.indexOf("/") === 0)) {
            request.picture = YAZINO.configuration.get('permanentContentUrl') + event.image;
        }

        YAZINO.util.trackEvent('Facebook', 'ClickOnPost', postIdX);

        if (gameSuffix !== "null" || gameSuffix !== "undefined") {
            if (YAZINO.configuration.get('inGame')) {
                gameSuffix += "_IN_GAME";
            }
            YAZINO.util.trackEvent('Facebook', 'ClickFor_' + gameSuffix, postIdX);
        } else {
            gameSuffix = "";
        }

        if (window.StrataPartnerApi.isAutoPosting() && allowAutoPosting) {
            window.sendPost(request);
        } else {
            postViaDialog(request, event, gameSuffix);
        }
    };
    return result;
};

YAZINO.FacebookFriendsSelectorService = /** @constructor */function () {
    YAZINO.EventDispatcher.apply(this);
    var that = this,
        facebookFriends = [],
        allRegisteredFriends = [],
        isFacebookComplete = false,
        isPlayerRelationshipComplete = false,
        i,
        dispatchUnregisteredFacebookFriends;
    this.init = function () {
        var fbInitialiser;
        fbInitialiser = function () {
            if (typeof FB === 'undefined') {
                setTimeout(fbInitialiser, 100);
            } else {
                FB.getLoginStatus(function (response) {
                    if (YAZINO.configuration.get('facebookCanvasActionsAllowed')) {
                        if (response.authResponse) {
                            FB.api('/me/friends?fields=name,picture&return_ssl_resources=1', function (friends) {
                                facebookFriends = [];
                                for (i = 0; i < friends.data.length; i += 1) {
                                    facebookFriends.push(friends.data[i]);
                                }
                                if (isPlayerRelationshipComplete) {
                                    dispatchUnregisteredFacebookFriends();
                                } else {
                                    isFacebookComplete = true;
                                }
                            });
                        }
                    } else {
                        facebookFriends = [];
                        if (isPlayerRelationshipComplete) {
                            dispatchUnregisteredFacebookFriends();
                        } else {
                            isFacebookComplete = true;
                        }
                    }
                });
            }
        };
        setTimeout(fbInitialiser, 10);
    };
    YAZINO.playerRelationshipsService.addEventListener("FriendsRelationshipsChanged", function (event) {
        if (!isPlayerRelationshipComplete) {
            var otherFriends = YAZINO.playerRelationshipsService.getAllFriendsDetails();
            jQuery.each(otherFriends, function (key, value) {
                allRegisteredFriends.push(value.nickname);
            });
            if (isFacebookComplete) {
                dispatchUnregisteredFacebookFriends();
            } else {
                isPlayerRelationshipComplete = true;
            }
        }
    });
    dispatchUnregisteredFacebookFriends = function () {
        var unregisteredFacebookFriends = [], i;
        for (i = 0; i < facebookFriends.length; i += 1) {
            if (jQuery.inArray(facebookFriends[i].name, allRegisteredFriends) === -1) {
                if (facebookFriends[i].name.length > 15) {
                    facebookFriends[i].name = facebookFriends[i].name.substring(0, 15) + '...';
                }
                unregisteredFacebookFriends.push(facebookFriends[i]);
            }
        }
        that.dispatchEvent({
            eventType: "UnregisteredFacebookFriends",
            data: unregisteredFacebookFriends
        });
    };
};

YAZINO.FriendSelectorService = /** @constructor */function () {
    YAZINO.EventDispatcher.apply(this);
    var that = this, onlineFriends = {}, offlineFriends = {}, selectedFriends = {},
        makeAjaxCall = function (url, methodType, dataType, data, successFtn) {
            jQuery.ajax({
                url: url,
                type: methodType,
                dataType: dataType,
                data: data,
                success: successFtn
            });
        };
    this.loadFriends = function () {
        onlineFriends = YAZINO.playerRelationshipsService.getAllOnlineFriendDetails();
        offlineFriends = YAZINO.playerRelationshipsService.getOfflineFriendsDetails();
        selectedFriends = {};
        var allFriends = [];
        jQuery.each(onlineFriends, function (key, value) {
            allFriends.push(value);
        });
        jQuery.each(offlineFriends, function (key, value) {
            allFriends.push(value);
        });
        that.dispatchEvent({
            eventType: "AvailableFriendsChanged",
            friends: allFriends
        });
    };
    this.selectFriend = function (playerId) {
        selectedFriends[playerId] = playerId;
    };
    this.deselectFriend = function (playerId) {
        delete selectedFriends[playerId];
    };
    this.sendInvites = function (tableId, message) {
        var friendIds = [];
        jQuery.each(selectedFriends, function (key, value) {
            friendIds.push(value);
        });
        if (friendIds.length > 0) {
            makeAjaxCall(
                "/lobby/inviteFriends",
                "POST",
                "json",
                { tableId: tableId, friendIds: friendIds.join(","), message: message },
                that.onSuccess
            );
        }
    };
    this.onSuccess = function (data) {
        if (!data.error) {
            that.dispatchEvent({
                eventType: "FriendsInvited",
                details: data
            });
        } else {
            that.dispatchEvent({
                eventType: "FriendsInvitationError",
                error: data.error
            });
        }
    };
};

YAZINO.TournamentService = /** @constructor */function () {
    var that = this, fetchNextTournamentInfo, tournamentInfoPoller, startNumber;
    YAZINO.EventDispatcher.apply(this);
    fetchNextTournamentInfo = function (gameType) {
        jQuery.ajax({
            url: YAZINO.configuration.get('nextTournamentUrl'),
            type: "POST",
            dataType: "json",
            data: { gameType: gameType },
            success: function (data) {
                that.dispatchEvent({
                    eventType: "NextTournamentInfoChanged_" + gameType,
                    details: data
                });
            }
        });
    };
    tournamentInfoPoller = function () {
        var listenersByEventType, eventType, gameType;
        listenersByEventType = that.getListenersByEventType();
        for (eventType in listenersByEventType) {
            if (eventType.startsWith("NextTournamentInfoChanged_")) {
                gameType = eventType.substring("NextTournamentInfoChanged_".length);
                fetchNextTournamentInfo(gameType);
            }
        }
    };
    this.fetchTournamentSchedule = function (gameType) {
        jQuery.ajax({
            url: YAZINO.configuration.get('tournamentScheduleUrl'),
            type: "POST",
            dataType: "json",
            data: { gameType: gameType },
            success: function (data) {
                that.dispatchEvent({
                    eventType: "TournamentScheduleChanged_" + gameType,
                    details: data
                });
            }
        });
    };
    this.tournamentSchedulePoller = function () {
        var listenersByEventType, eventType, gameType;
        listenersByEventType = that.getListenersByEventType();
        for (eventType in listenersByEventType) {
            if (eventType.startsWith("TournamentScheduleChanged_")) {
                gameType = eventType.substring("TournamentScheduleChanged_".length);
                that.fetchTournamentSchedule(gameType);
            }
        }
    };
    window.setInterval(that.tournamentSchedulePoller, 30000);
    window.setInterval(tournamentInfoPoller, 30000);
    this.register = function (gameType, tournamentId, callback) {
        jQuery.ajax({
            url: "/lobbyCommand/registerTournament",
            type: "POST",
            dataType: "json",
            data: { tournamentId: tournamentId },
            success: function (data) {
                var errorMessage = {
                    'MAX_PLAYERS_EXCEEDED': 'Tournament is full',
                    'TRANSFER_FAILED': 'You do not have enough chips to register'
                }[data.result];
                if (errorMessage) {
                    YAZINO.notificationService.dispatchEvent({
                        eventType: "NotificationEvent",
                        displayMessage: errorMessage,
                        duration: 30
                    });
                    return;
                }
                if (!callback) {
                    fetchNextTournamentInfo(gameType);
                } else {
                    callback(gameType);
                }
            }
        });
    };
    this.openTournament = YAZINO.util.openTournament;
};

YAZINO.CountdownService = /** @constructor */function () {
    var that = this,
        ONE_SECOND = 1000,
        ONE_MINUTE = 60 * ONE_SECOND,
        ONE_HOUR = 60 * ONE_MINUTE,
        ONE_DAY = 24 * ONE_HOUR,
        getUnits,
        updateTimeout,
        timer;
    getUnits = function (millis, unit, suffix) {
        return Math.floor(millis / unit).toFixed(0) + suffix;
    };
    updateTimeout = function (element, uniqueId, stringPrefix, callback) {
        if (!element.attr("millisToStart")) {
            return;
        }
        var remainingString = "", days, hours, minutes, seconds,
            millisToStart = parseInt(element.attr("millisToStart") || -1, 10), newString;
        if (millisToStart > 0) {
            if (millisToStart < ONE_MINUTE) {
                remainingString = getUnits(millisToStart, ONE_SECOND, "s");
            } else if (millisToStart < ONE_HOUR) {
                minutes = getUnits(millisToStart, ONE_MINUTE, "m");
                seconds = getUnits(millisToStart % ONE_MINUTE, ONE_SECOND, "s");
                remainingString = minutes + ", " + seconds;
            } else if (millisToStart < ONE_DAY) {
                hours = getUnits(millisToStart, ONE_HOUR, "h");
                minutes = getUnits(millisToStart % ONE_HOUR, ONE_MINUTE, "m");
                remainingString = hours + ", " + minutes;
            } else {
                days = getUnits(millisToStart, ONE_DAY, "d");
                hours = getUnits(millisToStart % ONE_DAY, ONE_HOUR, "h");
                remainingString = days + ", " + hours;
            }
            millisToStart -= 1000;
            if (millisToStart < 0) {
                millisToStart = 0;
            }
            element.attr("millisToStart", millisToStart);
        } else if (millisToStart === 0) {
            element.attr("millisToStart", "");
            if (callback) {
                newString = callback(element);
                if (newString) {
                    remainingString = newString;
                }
            }
        }
        element.html(remainingString ? stringPrefix + remainingString : "");
    };
    this.countDown = function (element, uniqueId, stringPrefix, callback) {
        window.setInterval(
            function () {
                updateTimeout(element, uniqueId, stringPrefix, callback);
            },
            1000
        );
        updateTimeout(element, uniqueId, stringPrefix, callback);
    };
};

YAZINO.GameLoaderService = function () {
    var loaderLoadedDeferred = jQuery.Deferred();
    YAZINO.onLoaderLoaded = loaderLoadedDeferred.resolve;
    this.setIsMuted = function (isMuted) {
        loaderLoadedDeferred.done(
            function () {
                document.getElementById('loader').setIsMuted(isMuted);
            }
        );
    };
};

YAZINO.legacyDailyAwardDataProvider = (function () {
    function getAjaxData(deferredResponse) {
        jQuery.ajax({
            url: '/dailyAward/web',
            type: "POST",
            dataType: "json",
            success: function (data) {
                var i, dailyAwardDetails;
                if (data && data.dailyAwardConfig) {
                    dailyAwardDetails = data.dailyAwardConfig;
                    // Grab all top-level adata from response:
                    for (i in data) {
                        if (data.hasOwnProperty(i) && i !== 'dailyAwardConfig') {
                            dailyAwardDetails[i] = data[i];
                        }
                    }
                    deferredResponse.config = dailyAwardDetails;
                    deferredResponse.resolve();
                    if (data.balance) {
                        YAZINO.dispatchDocument({
                            eventType: "PLAYER_BALANCE",
                            document : {
                                balance: data.balance
                            }
                        });
                    }
                } else {
                    deferredResponse.reject();
                }
            },
            error: deferredResponse.reject
        });
    }

    return {
        getConfig: getAjaxData,
        isAvailable: function () {
            return !!YAZINO.configuration.get('requestTopUp');
        },
        acknowledge: function () {},
        reset: function () {}
    };

}());

YAZINO.dailyAwardDataProvider = (function ($, yaz) {

    var acknowledgementPending, result;

    function convertConfigToLegacyFormat(config) {
        var result = {
                consecutiveDaysPlayed: null,
                iosImage: null,
                mainImage: null,
                mainImageLink: null,
                maxRewards: null,
                newsBackgroundImage: null,
                newsBackgroundImageLink: null,
                newsHeader: null,
                newsText: null,
                progressiveAward: null,
                promotionId: null,
                promotionValueList: null,
                rewardChips: null,
                secondaryImage: null,
                secondaryImageLink: null,
                topupAmount: null
            };

        result.consecutiveDaysPlayed = config.topUpDetails.progressive.consecutiveDaysPlayed;
        result.progressiveAward = "AWARD_" + (config.topUpDetails.progressive.consecutiveDaysPlayed + 1);
        result.promotionValueList = config.topUpDetails.progressive.amounts;
        if (config.articles[0]) {
            result.mainImage = config.articles[0].imageUrl;
            result.mainImageLink = config.articles[0].imageLink;
        }
        if (config.articles[1]) {
            result.secondaryImageLink = result.secondaryImage = config.articles[1].imageUrl;
            result.secondaryImageLink = config.articles[1].imageLink;
        }
        result.topupAmount = config.topUpDetails.totalAmount;

        return result;
    }

    function isAvailable() {
        return yaz.util.isParentFrame() && yaz.configuration.get('dailyAward.status') === 'CREDITED';
    }

    result = {
        getConfig: function (deferred) {
            if (isAvailable()) {
                deferred.config = convertConfigToLegacyFormat(yaz.configuration.get('dailyAward'));
                deferred.resolve();
                acknowledgementPending = function () {
                    $.ajax({
                        url: "/dailyAward/topUpAcknowledged",
                        data: {
                            lastTopUpDate: yaz.configuration.get('dailyAward.topUpDetails.date')
                        },
                        type: 'POST'
                    });
                };
                if (yaz.configuration.get('dailyAward.instantAcknowledge')) {
                    acknowledgementPending();
                    acknowledgementPending = function () {};
                }
            }
        },
        acknowledge : function () {
            if (acknowledgementPending) {
                acknowledgementPending();
                yaz.configuration.set('dailyAward', {status: 'ACKNOWLEDGED'});
                result.reset();
            } else {
                throw "can't acknowledge dailyAward before it's been displayed.";
            }
        },
        isAvailable: isAvailable,
        reset: function () {
            acknowledgementPending = null;
        }
    };

    return result;
}(jQuery, YAZINO));


YAZINO.createDailyAwardService = function (modalDialogueService, dataProvider) {
    var result = new YAZINO.EventDispatcher(),
        popupGrantedDeferred = new jQuery.Deferred(),
        dataProviderDeferred = new jQuery.Deferred();
    result.dismissDialogue = function () {
        jQuery("#games-area").show(); // workaround for bug FLS-1501
        modalDialogueService.dismissDialogue();
    };
    result.init = function () {
        if (dataProvider.isAvailable()) {
            modalDialogueService.addEventListener("DailyAwardPopupShown", popupGrantedDeferred.resolve);
            modalDialogueService.addEventListener("DailyAwardPopupHidden", result.dispatchEvent);
            modalDialogueService.addEventListener("DailyAwardPopupHidden", function () {
                dataProvider.acknowledge();
            });
            modalDialogueService.requestDialogue("DailyAwardPopup");
            jQuery.when(popupGrantedDeferred, dataProviderDeferred).then(
                function () {
                    jQuery("#games-area").hide(); // workaround for bug FLS-1501
                    result.dispatchEvent({
                        eventType: "DailyAwardPopupShown",
                        dailyAwardDetails: dataProviderDeferred.config
                    });
                },
                modalDialogueService.dismissDialogue
            );
            dataProvider.getConfig(dataProviderDeferred);
        }
        return result;
    };
    return result;
};

(function ($, yazino) {
    YAZINO.generateActionContainer = function (externalActionsMap) {
        var actionsMap = externalActionsMap || {};

        function createAction(actionName, func) {
            actionsMap[actionName] = function (ctaRef) {
                yazino.businessIntelligence.track.yazinoAction(actionName, ctaRef, yazino.configuration.get('gameType'));
                func();
            };
        }

        function callAction(actionName, ctaRef) {
            actionsMap[actionName](ctaRef);
        }

        function containsAction(actionName) {
            return typeof actionsMap[actionName] === 'function';
        }

        return {
            create: createAction,
            run: callAction,
            contains: containsAction
        };

    };
    YAZINO.action = YAZINO.generateActionContainer(YAZINO.actions);
}(jQuery, YAZINO));


(function (actionManager, fb, fbogService) {
    function addLightboxIframeAction(actionName, url) {
        actionManager.create(actionName, function () {
            YAZINO.lightboxWidget.createIframe(url);
        });
    }

    addLightboxIframeAction('invitationStatement', '/player/invitations');
    addLightboxIframeAction('playerProfile', '/player/profile');
    addLightboxIframeAction('sendChallenge', '/challenge');

    actionManager.create('buyChips', function () {
        window.showPaymentsOverlay(false);
        fbogService.updateLoginStatus(fb);
    });
    actionManager.create('earnChips', function () {
        window.showPaymentsOverlay(true);
    });
    actionManager.create('closeLightbox', function () {
        YAZINO.util.getParentFrame().YAZINO.lightboxWidget.kill();
    });
}(YAZINO.action, (typeof (FB) !== 'undefined' ? FB : {}), YAZINO.facebookOpenGraphService));

(function ($, yazino) {
    var cachedBalance = "",
        widgetsRegistered = [],
        listenerCreated = false;

    function updateBalanceInAllWidgets() {
        $.each(widgetsRegistered, function () {
            this.text(cachedBalance);
        });
    }

    function listenerBehaviour(event) {
        YAZINO.logger.debug("balanceWidget on%s balance=[%d]", event.eventType, event.balance);
        cachedBalance = event.balance;
        updateBalanceInAllWidgets();
    }

    $.fn.extend({
        playerBalanceWidget: /** @this {!Object} */function () {

            if (!listenerCreated) {
                listenerCreated = true;
                YAZINO.playerService.addEventListener("BalanceChanged", listenerBehaviour);
            }
            return this.each(
                function () {
                    widgetsRegistered.push($(this).find('.balance'));
                    updateBalanceInAllWidgets();
                }
            );
        }
    });
}(jQuery, YAZINO));


jQuery.fn.extend({
    profileBoxWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this), setPercentage;
                setPercentage = function (value) {
                    widget.find(".levelPercentageProgress").css("width", value + "%");
                    widget.find(".levelPercentageValue").css("left", value + "px");
                };
                YAZINO.playerService.addEventListener("CollectibleChanged", function (event) {
                    switch (event.collectibleType) {
                    case "medal":
                        widget.find(".medals").text(event.amount);
                        break;
                    case "trophy":
                        widget.find(".trophies").text(event.amount);
                        break;
                    case "achievement":
                        widget.find(".achievements").text(event.amount);
                        widget.find(".totalAchievements").text(event.total);
                        break;
                    }
                });
                YAZINO.playerService.addEventListener("ExperienceChanged", function (event) {
                    var points, toNextLevel, percentage;
                    if (event.gameType === YAZINO.configuration.get('gameType')) {
                        widget.find(".currentLevel").text(event.level);
                        points = event.points;
                        toNextLevel = event.toNextLevel;
                        percentage = toNextLevel ? Math.round(100 * points / toNextLevel) : 0;
                        widget.find(".levelPoints").text(event.points);
                        widget.find(".levelToNext").text(event.toNextLevel);
                        setPercentage(percentage);
                    }
                });
            }
        );
    },
    tableLauncherWidget: /** @this {!Object} */function (tableService, isPrivateTable) {
        return this.each(
            function () {
                var widget = jQuery(this), model, gameType, templateName, fastSpeedWidget, normalSpeedWidget, parentElement;
                gameType = widget.attr("gameType");
                model = tableService.getTableLauncherWidgetModel(gameType, isPrivateTable);
                fastSpeedWidget = widget.find(".fast");
                normalSpeedWidget = widget.find(".normal");
                if (!isPrivateTable) {
                    widget.find(".playNow:not(#bt-goTableNow)").click(model.playNow);
                }
                model.addEventListener("StakeChanged", function (event) {
                    try {
                        if (widget.find('.control-limiter').first().slider('value') !== (event.details.stakeIndex + 1)) {
                            widget.find('.control-limiter').slider('value', event.details.stakeIndex + 1);
                        }
                    } catch (e) {
                        // occurs under IE8 on page init. Harmless for our needs. Go Microsoft!
                    }
                    widget.find('.stake').text(event.details.description);
                    if (!isPrivateTable) {
                        var balance = YAZINO.playerService.getBalanceAsNumber();
                        if (balance < event.details.lowerLimit) {
                            widget.find(".playNow").hide();
                            widget.find(".buyChips").show();
                        } else {
                            widget.find(".playNow").show();
                            widget.find(".buyChips").hide();
                        }
                    }
                });
                model.addEventListener("SpeedChanged", function (event) {
                    jQuery(".control-bt.speed a").each(function () {
                        var el = jQuery(this);
                        if (el.hasClass(event.speed)) {
                            el.addClass("current");
                        } else {
                            el.removeClass("current");
                        }
                    });
                });
                model.addEventListener("TemplateNameChanged",
                    function (event) {
                        jQuery(".control-bt.variant a").each(function () {
                            var el = jQuery(this);
                            if (el.hasClass(event.variantId)) {
                                el.addClass("current");
                            } else {
                                el.removeClass("current");
                            }
                        });
                    });
                widget.find('.control-limiter').slider({
                    min: 1,
                    max: model.getNumberOfStakes(gameType),
                    value: [1],
                    animate: true,
                    slide: function (event, ui) {
                        setTimeout(function () {
                            model.setStake(ui.value - 1);
                        }, 0);
                    }
                });
                widget.find('.slide-area').click(function (click) {
                    var value, click_position, slider_position;
                    value = jQuery(this).find(".control-limiter").slider("option", "value");
                    click_position = click.pageX - jQuery(this).offset().left;
                    slider_position = click_position / jQuery(this).width();
                    model.setStake(slider_position > 0.5 ? model.getNumberOfStakes(gameType) - 1 : 0);
                });
                widget.find('.slide-control').appendTo(widget.find('.ui-slider-handle'));
                widget.find('.slide-area').each(function (index, parent) {
                    model.setStake(0);
                });
                widget.find('.control-bt.speed a').click(function () {
                    model.changeSpeed(jQuery(this).hasClass("fast") ? "fast" : "normal");
                });
                widget.find('.control-bt.variant a').click(function () {
                    var variantId = jQuery(this).attr('class');
                    variantId = variantId.replace('current', '').replace(' ', '');
                    YAZINO.logger.debug('variant ID: ' + variantId);
                    model.setTemplateName(model.getTemplateNameFor(variantId));
                });
            }
        );
    },
    tournamentInfoWidget: /** @this {!Object} */function (tournamentService) {
        return this.each(
            function () {
                var widget = jQuery(this), gameType, registerButton, playButton, tournamentIdWidget, registering;
                gameType = widget.attr("gameType");
                registerButton = widget.find(".registerButton");
                playButton = widget.find(".playButton");
                tournamentIdWidget = widget.find(".tournamentId_" + gameType);
                registering = false;
                registerButton.click(function () {
                    tournamentService.register(gameType, tournamentIdWidget.val());
                    registering = true;
                    playButton.attr("value", "Registering...");
                });
                playButton.click(function () {
                    tournamentService.openTournament(gameType, tournamentIdWidget.val());
                });
                tournamentService.addEventListener("NextTournamentInfoChanged_" + gameType, function (event) {
                    YAZINO.logger.debug("tournamentInfoWidget on%s", event.eventType);
                    if (!event.details) {
                        return;
                    }
                    // Skip the event if we think we should be registered but dont recieve a success response
                    if (registering && !event.details.playerRegistered) {
                        registering = false;
                        return;
                    }
                    tournamentIdWidget.val(event.details.tournamentId);
                    widget.find(".tournamentName").html((event.details && event.details.name) || "");
                    widget.find(".registrationFee").html(YAZINO.util.formatCurrency(event.details.registrationFee, 0, '.', ',', '') || "");
                    widget.find(".prizePool").html(YAZINO.util.formatCurrency(event.details.prizePool, 0, '.', ',', '') || "");
                    widget.find(".registeredPlayers").html((event.details && event.details.registeredPlayers) || "0");
                    widget.find(".registeredFriends").html((event.details && event.details.registeredFriends) || "0");
                    widget.find(".countdownWidget").attr("millisToStart", event.details && event.details.millisToStart > 0 ? event.details.millisToStart : "");
                    widget.find(".registered").css("display", event.details && event.details.playerRegistered ? "block" : "none");
                    if (event.details && event.details.playerRegistered) {
                        playButton.css("display", "block");
                        playButton.attr("value", event.details.inProgress ? "Play" : "Details");
                        registerButton.css("display", "none");
                    } else {
                        playButton.css("display", "none");
                        if (YAZINO.playerService.getBalanceAsNumber() >= event.details.registrationFee) {
                            registerButton.css("display", "block");
                        } else {
                            registerButton.css("display", "none");
                        }
                    }
                });
            }
        );
    },
    tournamentResultsWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this),
                    resultsTab = widget.find(".results-tab"),
                    fameTab = widget.find(".fame-tab"),
                    results = widget.find(".tournamentLastResultsWidget"),
                    fame = widget.find(".hallOfFameWidget"),
                    friendsList = results.find(".list.friends"),
                    worldTab = results.find(".tabs .world"),
                    worldList = results.find(".list.world");
                fameTab.click(
                    function () {
                        fameTab.addClass("active");
                        fameTab.removeClass("unactive");
                        resultsTab.addClass("unactive");
                        resultsTab.removeClass("active");
                        fame.addClass("active");
                        results.removeClass("active");
                        friendsList.removeClass("active");
                        worldList.removeClass("active");
                        results.hide();
                        fame.show();
                    }
                );
                resultsTab.click(
                    function () {
                        fameTab.addClass("unactive");
                        fameTab.removeClass("active");
                        resultsTab.addClass("active");
                        resultsTab.removeClass("unactive");
                        results.addClass("active");
                        fame.removeClass("active");
                        worldTab.click();
                        results.show();
                        fame.hide();
                    }
                );
            }
        );
    },
    tournamentLastResultsWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this),
                    worldTab = widget.find(".tabs .world"),
                    friendsTab = widget.find(".tabs .friends"),
                    worldList = widget.find(".list.world"),
                    friendsList = widget.find(".list.friends");
                worldList.scrollWidget();
                friendsList.scrollWidget();
                worldList.addClass("active");
                jQuery(".first .payout, .second .payout, .third .payout").each(function () {
                    var that = jQuery(this),
                        text = that.text();
                    that.text(YAZINO.util.formatCurrency(text, 0, '.', ',', '$'));
                });
                worldTab.click(function () {
                    worldTab.addClass("active");
                    friendsTab.removeClass("active");
                    worldList.show();
                    worldList.addClass("active");
                    friendsList.hide();
                    friendsList.removeClass("active");
                });
                friendsTab.click(function () {
                    worldTab.removeClass("active");
                    friendsTab.addClass("active");
                    worldList.hide();
                    worldList.removeClass("active");
                    friendsList.show();
                    friendsList.addClass("active");
                });
            }
        );
    },
    hallOfFameWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this);
                widget.scrollWidget();
            }
        );
    },
    trophyLeaderboardWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this), worldtab, friendstab, worldlist, friendslist;
                worldtab = widget.find(".world");
                friendstab = widget.find(".friends");
                worldlist = widget.find(".list.world");
                worldlist.scrollWidget();
                friendslist = widget.find(".list.friends");
                friendslist.scrollWidget();
                friendslist.slider = function () {
                    worldlist.find(".slider");
                };
                worldlist.slider = function () {
                    worldlist.find(".slider");
                };
                friendslist.slider();
                worldtab.click(function () {
                    worldtab.addClass("active");
                    friendstab.removeClass("active");
                    worldlist.show();
                    friendslist.hide();
                });
                friendstab.click(function () {
                    worldtab.removeClass("active");
                    friendstab.addClass("active");
                    worldlist.hide();
                    friendslist.show();
                });
            }
        );
    },
    scrollWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this);
                widget.attr("lastTop", 0);
                widget.upButton = function () {
                    return jQuery(this).parent().parent().find(".up a");
                };
                widget.downButton = function () {
                    return jQuery(this).parent().parent().find(".down a");
                };
                widget.slider = function () {
                    var that = jQuery(this);
                    if (that.hasClass("slider")) {
                        return that;
                    }
                    return jQuery(this).find(".slider");
                };
                widget.pageSize = function () {
                    return widget.slider().parent().height() - 20;
                };
                widget.lastTop = function () {
                    return parseInt(jQuery(this).attr("lastTop"), 10);
                };
                widget.isAtBottom = function () {
                    return (widget.lastTop() - widget.pageSize()) + widget.slider().height() < 0;
                };
                widget.isAtTop = function () {
                    return widget.lastTop() >= 0;
                };
                widget.isActive = function () {
                    return widget.hasClass("active");
                };
                widget.moveTo = function (topVal) {
                    var that = widget;
                    if (topVal + that.slider().height() < 0) {
                        return;
                    }
                    if (topVal > 0) {
                        topVal = 0;
                    }
                    that.slider().animate({ top: topVal }, 500);
                    that.attr("lastTop", topVal);
                };
                widget.toggleButtons = function () {
                    var that = widget;
                    if (that.isAtTop()) {
                        that.upButton().hide();
                    } else {
                        that.upButton().show();
                    }
                    if (that.isAtBottom()) {
                        that.downButton().hide();
                    } else {
                        that.downButton().show();
                    }
                };
                widget.downButton().click(
                    function () {
                        if (!widget.isActive()) {
                            return;
                        }
                        widget.moveTo(widget.lastTop() - widget.pageSize());
                    }
                );
                widget.upButton().click(
                    function () {
                        if (!widget.isActive()) {
                            return;
                        }
                        widget.moveTo(widget.lastTop() + widget.pageSize());
                    }
                );
                widget.refresh = function () {
                    var topVal, height;
                    if (!widget.isActive()) {
                        return;
                    }
                    topVal = widget.lastTop();
                    height = widget.slider().height();
                    if (topVal + height < 0) {
                        topVal = -height + height / 10;
                        widget.moveTo(topVal);
                    }
                    widget.toggleButtons();
                };
                window.setInterval(widget.refresh, 500);
            }
        );
    },
    tournamentScheduleWidget: /** @this {!Object} */function (tournamentService) {
        return this.each(
            function () {
                var widget = jQuery(this), gameType, scroller, pane;
                gameType = widget.attr("gameType");
                scroller = widget.parent().scrollWidget();
                tournamentService.addEventListener("TournamentScheduleChanged_" + gameType, function (event) {
                    YAZINO.logger.debug("tournamentScheduleWidget on%s", event.eventType);
                    widget.empty();
                    jQuery.each(event.details, function () {
                        var detail = jQuery(this)[0],
                            millisToStart = detail.millisToStart > 0 ? detail.millisToStart : "",
                            playerRegistered = detail.playerRegistered,
                            newItem = widget.parent().find(".tournamentScheduleTemplate li").first().clone(),
                            registerButton,
                            options;
                        widget.append(newItem);
                        if (millisToStart) {
                            newItem.find(".countdownWidget").attr("millisToStart", millisToStart);
                        } else {
                            newItem.find(".countdownWidget").text("STARTED");
                            newItem.find(".countdownWidget").removeClass("countdownWidget");
                        }
                        newItem.find(".prize").append(YAZINO.util.formatCurrency(detail.prizePool, 0, '.', ',', '$'));
                        newItem.find(".entry").append(YAZINO.util.formatCurrency(detail.registrationFee, 0, '.', ',', '$'));
                        newItem.find(".players").append(detail.registeredPlayers);
                        newItem.find(".friends").append(detail.registeredFriends);
                        if (detail.playerRegistered) {
                            registerButton = newItem.find(".details");
                            registerButton.removeClass("hidden");
                            registerButton.click(function () {
                                tournamentService.openTournament(gameType, detail.tournamentId);
                            });
                            if (millisToStart <= 0) {
                                registerButton.text("play");
                            }
                        } else {
                            registerButton = newItem.find(".doregister");
                            if (millisToStart > 0) {
                                registerButton.removeClass("hidden");
                                registerButton.click(function () {
                                    registerButton.addClass("hidden");
                                    registerButton.parent().find(".registering").removeClass("hidden");
                                    tournamentService.register(gameType, detail.tournamentId, tournamentService.tournamentSchedulePoller);
                                });
                            } else {
                                registerButton.addClass("hidden");
                            }
                        }
                        options = {
                            callback: function (widget) {
                                if (widget) {
                                    widget.parent().find(".doregister").addClass("hidden");
                                    widget.parent().find(".details").text("play");
                                }
                                tournamentService.tournamentSchedulePoller();
                                return "STARTED";
                            }
                        };
                        newItem.find(".countdownWidget").countdownWidget(options);
                    });
                });
                tournamentService.tournamentSchedulePoller();
            }
        );
    },
    newTabForExternalLinks: function () {
        return this.each(function () {
            jQuery(this).find('a').each(function () {
                var href = jQuery(this).attr('href');
                function matchesBaseDomain() {
                    function stripOffProtocol(input) {
                        if (input.startsWith('http://')) {
                            return input.substr(7);
                        } else if (input.startsWith('https://')) {
                            return input.substr(8);
                        }
                        return input;
                    }
                    function stripOffUri(input) {
                        var positionOfUri = input.indexOf('/');
                        if (positionOfUri > -1) {
                            return input.substr(0, positionOfUri);
                        }
                        return input;
                    }
                    function stripOffPort(input) {
                        var positionOfPort = input.indexOf(':');
                        if (positionOfPort > -1) {
                            return input.substr(0, positionOfPort);
                        }
                        return input;
                    }
                    function justDomain(input) {
                        return stripOffPort(stripOffUri(stripOffProtocol(input)));
                    }

                    return justDomain(href) === justDomain(YAZINO.configuration.get('baseUrl'));
                }
                if ((href.startsWith('http://') || href.startsWith('https://')) && !matchesBaseDomain()) {
                    jQuery(this).attr('target', '_blank');
                }
            });
        });
    }
});

(function ($) {
    YAZINO.newUserWelcomeMessage = function (config, lightboxWidget, localStorage) {
        var lightboxDom,
            itemHasBeenDisplayedKey = 'displayedTo_' + config.get('playerId');
        if (config.get('userDetails.isNewPlayer') && !localStorage.getItem(itemHasBeenDisplayedKey)) {
            lightboxDom = $('<div/>')
                .append(
                    $('<img/>')
                        .addClass('main')
                        .attr({
                            src: config.get('newPlayerPopup.image'),
                            alt: config.get('newPlayerPopup.alternativeText')
                        })
                )
                .append(
                    $('<a/>')
                        .addClass('featureLink')
                        .addClass('major')
                        .attr('href', 'yazino:closeLightbox')
                        .append($('<span/>').addClass('button').text('Collect your free chips'))
                )
                .mapInternalUrls();

            lightboxWidget.createFromElem(lightboxDom, 'newUser');
            localStorage.setItem(itemHasBeenDisplayedKey, true);
        }
    };
}(jQuery));

(function () {
    var countdownService = new YAZINO.CountdownService();
    jQuery.fn.extend({
        countdownWidget: /** @this {!Object} */function (options) {
            return this.each(
                function () {
                    var widget = jQuery(this),
                        prefix = (options && options.prefix) || "";
                    countdownService.countDown(widget, widget.attr("id"), prefix, options && options.callback);
                }
            );
        }
    });
}());

YAZINO.FacebookUserDetailsService = function () {
    var service = {};
    YAZINO.EventDispatcher.apply(service);
    service.getFacebookUserDetails = function (fbUserIds) {
        if (!fbUserIds || fbUserIds.length === 0) {
            YAZINO.logger.warn("No facebook IDs provided. ignoring...");
            return;
        }
        FB.api("/", {
            ids: fbUserIds,
            fields: ['name', 'picture']
        }, function (response) {
            if (!response) {
                YAZINO.logger.error("Empty Facebook response. Users = " + fbUserIds);
                return;
            }
            if (response.error) {
                YAZINO.logger.error("Error retrieving Facebook details: " + response.error.message
                    + ". Users = " + fbUserIds);
                return;
            }
            service.dispatchEvent({eventType: "FbUserDetailsRetrieved", data: response});
        });
    };
    return service;
};

jQuery.fn.extend({
    facebookUserDetailsWidget:/** @this {!Object} */function (facebookUserDetailsService) {
        var retrieveFacebookUserDetails = function (widget) {
            var fbUserId, fbUserIds = [];
            widget.find("[data-fb-user-id]").each(function () {
                fbUserId = jQuery(this).attr("data-fb-user-id");
                if (fbUserIds.indexOf(fbUserId) < 0) {
                    fbUserIds.push(fbUserId);
                }
            });
            facebookUserDetailsService.getFacebookUserDetails(fbUserIds);
        };
        return this.each(
            function () {
                var widget = jQuery(this);
                facebookUserDetailsService.addEventListener("FbUserDetailsRetrieved", function (event) {
                    var fbId, fbUserDetails, detailsContainer, pictureUrl;
                    for (fbId in event.data) {
                        fbUserDetails = event.data[fbId];
                        if (typeof fbUserDetails.picture === 'object') {
                            pictureUrl = fbUserDetails.picture.data.url; // Facebook October 2012 breaking API change
                        } else {
                            pictureUrl = fbUserDetails.picture;
                        }
                        detailsContainer = widget.find("[data-fb-user-id='" + fbId + "']").children().andSelf();
                        detailsContainer.find(".fbName").text(fbUserDetails.name);
                        detailsContainer.find(".fbPicture").attr("src", pictureUrl);
                    }
                });
                retrieveFacebookUserDetails(widget);
            }
        );
    }
});

YAZINO.FriendsPanel = function (oSliderFriendsWidget, oSliderWorldWidget, oSliderInivitesWidget) {
    if (jQuery('#my-tables-created').length === 0) {
        (function () {
            var bInit = true,
                options = oSliderFriendsWidget.getOptions(),
                doThisOnceOnly;
            options.service.addEventListener(options.eventName, function (event) {
                if (bInit) {
                    if (!event.data.length && !options.facebookFriends.length) {
                        jQuery('#friends-panel-head h3.tab2:first').click();
                    } else {
                        jQuery('#friends-panel-head h3.tab1:first').click();
                    }
                    bInit = false;
                }
            });
            doThisOnceOnly = true;
            if (options.facebookFriendsSelectorService) {
                options.facebookFriendsSelectorService.addEventListener(options.facebookEventName, function (event) {
                    if (doThisOnceOnly) {
                        if (event.data.length === 0 && options.preProcessData(event.data).length === 0) {
                            jQuery('#friends-panel-head h3.tab2:first').click();
                        } else {
                            jQuery('#friends-panel-head h3.tab1:first').click();
                        }
                        doThisOnceOnly = false;
                    }
                });
            }
        }());
    } else {
        (function () {
            var bInvitesInit = true,
                bFriendsInit = true,
                bInvitesDisplayed = false,
                invitesOptions = oSliderInivitesWidget.getOptions(),
                friendsOptions = oSliderFriendsWidget.getOptions();
            invitesOptions.service.addEventListener(invitesOptions.eventName, function (event) {
                if (bInvitesInit) {
                    if (event.data.length > 0) {
                        jQuery('#friends-panel-head h3.tab3:first').click();
                        bInvitesDisplayed = true;
                        bFriendsInit = false;
                    }
                    bInvitesInit = false;
                }
            });
            if (!bInvitesDisplayed) {
                friendsOptions.service.addEventListener(friendsOptions.eventName, function (event) {
                    if (bFriendsInit && !bInvitesDisplayed) {
                        if (event.data.length === 0) {
                            jQuery('#friends-panel-head h3.tab1:first').click();
                        }
                        bFriendsInit = false;
                    }
                });
            }
        }());
    }
};

window.trackPostPopup = function (postId) {
    var gameSuffix = String(YAZINO.configuration.get('gameType'));
    if (gameSuffix !== "null" || gameSuffix !== "undefined") {
        if (YAZINO.configuration.get('inGame')) {
            gameSuffix += "_IN_GAME";
        }
        YAZINO.util.trackEvent('Facebook', 'AchievementFor_' + gameSuffix, postId);
    }
    YAZINO.util.trackEvent('Facebook', 'Achievement', postId);
};

(function () {
    YAZINO.slider = {
        type: {
            FRIENDS: 'friends',
            WORLD: 'world',
            INVITES: 'tableInvites'
        }
    };
    var SliderControllerFactory = {
        getController: function (oOptions, widget) {
            var Controller = /** @constructor */function (options) {
                var arrData = [], facebookFriends = [], facebookFriend, iArrLength, iRangeStart = -1, iSlidesPerPage = options.slidesPerPage,
                    iRangeEnd = iSlidesPerPage, iScrollStart = -1, iScrollEnd = iSlidesPerPage,
                    iScrollPos = null, eHtmls, refresh, drawSlides, offSlides, that = this, iTimeout, eventListener;
                drawSlides = function () {
                    var sliderItem, i, j, n, arrHtml = [], randomNumbers, randomNumber;
                    for (i = iScrollStart; i < iScrollEnd; i += 1) {
                        sliderItem = arrData[i];
                        arrHtml.push(oOptions.drawSlide(sliderItem));
                    }
                    if (options.drawFacebookSlide && (iScrollPos > 0 || iScrollPos === null) && iRangeEnd <= iSlidesPerPage && iScrollEnd <= iSlidesPerPage) {
                        facebookFriends = options.facebookFriends;
                        randomNumbers = [];
                        for (i = 0, n = iSlidesPerPage - iScrollEnd; i < n && i < facebookFriends.length; i += 1) {
                            i = randomNumbers.length;
                            while (randomNumbers.length === i) {
                                randomNumber = Math.floor(Math.random() * facebookFriends.length);
                                if (jQuery.inArray(randomNumber, randomNumbers) === -1) {
                                    randomNumbers.push(randomNumber);
                                    facebookFriend = facebookFriends[randomNumber];
                                    arrHtml.push(oOptions.drawFacebookSlide(facebookFriend));
                                }
                            }
                        }
                    }
                    if (options.drawEmptySlide && (iScrollPos > 0 || iScrollPos === null) && iRangeEnd <= iSlidesPerPage && iScrollEnd <= iSlidesPerPage) {
                        for (i = 0, n = iSlidesPerPage - iScrollEnd;  i < n; i += 1) {
                            arrHtml.push(oOptions.drawEmptySlide());
                        }
                    }
                    return arrHtml;
                };
                refresh = function () {
                    iArrLength = arrData.length;
                    iRangeStart = -1;
                    that.scroll(Number.MIN_VALUE);
                };
                YAZINO.EventDispatcher.apply(this);
                this.scroll = function (iBy) {
                    var bScroll = false;
                    if (iBy !== 0) {
                        if (iBy === Number.MAX_VALUE && iArrLength > iSlidesPerPage && (iRangeStart !== (iArrLength - iSlidesPerPage))) {
                            bScroll = true;
                            iRangeStart = iScrollStart = iArrLength - iSlidesPerPage;
                            iRangeEnd = iScrollEnd = iArrLength;
                        } else if (iBy === Number.MIN_VALUE && (iRangeStart !== 0)) {
                            bScroll = true;
                            iRangeStart = iScrollStart = 0;
                            iRangeEnd = iSlidesPerPage;
                            if (iArrLength > iSlidesPerPage) {
                                iScrollEnd = iSlidesPerPage;
                            } else {
                                iScrollEnd = iArrLength;
                            }
                        } else if (iBy > 0 && iRangeStart < (iArrLength - iSlidesPerPage)) {
                            bScroll = true;
                            if ((iRangeEnd + iBy) <= iArrLength) {
                                iRangeStart = iScrollStart = iRangeEnd;
                                iRangeEnd = iScrollEnd = iRangeStart + iBy;
                            } else {
                                iRangeStart = iArrLength - iSlidesPerPage;
                                iScrollStart = iRangeEnd;
                                iRangeEnd = iScrollEnd = iArrLength;
                            }
                        } else if (iBy < 0 && iRangeStart > 0) {
                            bScroll = true;
                            if (iRangeStart + iBy >= 0) {
                                iScrollStart = iRangeStart += iBy;
                                iScrollEnd = iScrollStart + iSlidesPerPage;
                            } else {
                                iScrollEnd = iRangeStart;
                                iRangeStart = iScrollStart = 0;
                            }
                            iRangeEnd = iRangeStart + iSlidesPerPage;
                        }
                    }
                    if (bScroll) {
                        eHtmls = drawSlides();
                        if (iArrLength > iSlidesPerPage) {
                            if (iRangeStart === 0) {
                                iScrollPos = 1;
                            } else if ((iRangeStart > 0) && (iRangeStart < (iArrLength - iSlidesPerPage))) {
                                iScrollPos = 0;
                            } else {
                                iScrollPos = -1;
                            }
                        } else {
                            iScrollPos = null;
                        }
                        widget.dispatchEvent({eventType: 'loadSlides', slides: eHtmls, scrollPos: iScrollPos});
                    } else {
                        widget.dispatchEvent({eventType: 'endOfSlides', scrollPos: iScrollPos});
                    }
                };
                this.getOffSlides = function (iNumber) {
                    return offSlides.call(this, iNumber);
                };
                if (!options.data) {
                    iTimeout = setTimeout(function () {
                        if (!arrData) {
                            arrData = [];
                        }
                        refresh();
                    }, 20000);
                }
                eventListener = function (event) {
                    if (iTimeout) {
                        window.clearTimeout(iTimeout);
                        iTimeout = null;
                    }
                    if (options.preProcessData) {
                        arrData = options.preProcessData(event.data);
                    } else {
                        arrData = event.data;
                    }
                    if (arrData) {
                        refresh();
                    }
                };
                if (options.visibilityController) {
                    options.visibilityController.addEventListener("VisibilityChanged", function (event) {
                        if (event.visible) {
                            options.service.addEventListener(options.eventName, eventListener);
                        } else {
                            options.service.removeEventListener(options.eventName, eventListener);
                        }
                    });
                } else {
                    options.service.addEventListener(options.eventName, eventListener);
                }
                if (options.facebookFriendsSelectorService) {
                    options.facebookFriendsSelectorService.init();
                    options.facebookFriendsSelectorService.addEventListener(options.facebookEventName, function (event) {
                        options.facebookFriends = event.data;
                        refresh();
                    });
                }
            };
            return new Controller(oOptions);
        }
    },
        SliderWidget = /** @constructor */function (eDiv, options) {
            var eSlides = jQuery('li', eDiv),
                iSlidesPerPage = eSlides.length || options.slidesPerPage,
                iSliderWidth = parseInt(eDiv.css('width'), 10),
                iScrollDir = 0,
                bProcessing = false,
                arrHtml,
                eCtrlForward,
                eCtrlEnd,
                eCtrlBack,
                controller,
                eCtrlStart,
                scroll,
                loadData,
                activate,
                deactivate,
                unused;
            YAZINO.EventDispatcher.apply(this);
            scroll = function () {
                var i = 0,
                    j = i,
                    k,
                    n = arrHtml.length,
                    updateSlides,
                    iAddedWidth,
                    iUlWidth,
                    iLiWidth,
                    eLi,
                    eLis,
                    eNewLi;
                updateSlides = function () {
                    var eHtml,
                        eLi;
                    eSlides.each(function (index) {
                        eLi = jQuery(this);
                        eHtml = arrHtml[i];
                        i += 1;
                        eLi.empty();
                        eLi.html(eHtml && eHtml.mapInternalUrls());
                    });
                };
                if (iScrollDir !== 0) {
                    if (iScrollDir === 1) {
                        iAddedWidth = (parseInt(jQuery('>:first-child', eDiv).css('width'), 10))  * n;
                        iUlWidth = iSliderWidth + iAddedWidth;
                        eDiv.css('width', iUlWidth + 'px');
                        for (unused = true; i < n; i += 1) {
                            eLi = jQuery('<li></li>');
                            eDiv.append(eLi.html(arrHtml[i]));
                        }
                        eLis = eDiv.children();
                        eDiv.animate({'left': -iAddedWidth}, 700, function () {
                            for (i = 0; i < n; i += 1) {
                                jQuery(eLis[i]).css('display', 'none');
                            }
                            eDiv.css({'left': 0, 'width': iSliderWidth + 'px'});
                            setTimeout(function () {
                                for (i = 0; i < n; i += 1) {
                                    jQuery(eLis[i]).remove();
                                }
                                bProcessing = false;
                            }, 1);
                        });
                    } else if (iScrollDir === -1) {
                        iAddedWidth = (parseInt(jQuery('>:first-child', eDiv).css('width'), 10))  * n;
                        iUlWidth = iSliderWidth + iAddedWidth;
                        eLi = jQuery('>:first-child', eDiv);
                        eDiv.css({'width': iUlWidth + 'px', 'left': -iAddedWidth + 'px'});
                        for (i = n - 1; i >= 0; i -= 1) {
                            eNewLi = jQuery('<li></li>');
                            eLi.before(eNewLi.html(arrHtml[i]));
                            eLi = eNewLi;
                        }
                        eLis = eDiv.children();
                        eDiv.animate({'left': 0}, 700, function () {
                            for (k = eLis.length,  i = iSlidesPerPage; i < k; i += 1) {
                                jQuery(eLis[i]).css('display', 'none');
                            }
                            eDiv.css('width', iSliderWidth + 'px');
                            setTimeout(function () {
                                for (i = eLis.length - 1, n = i - n; i > n; i -= 1) {
                                    jQuery(eLis[i]).remove();
                                }
                                bProcessing = false;
                            }, 1);
                        });
                    } else if (iScrollDir === 2) {
                        eLi = jQuery('>:last-child', eDiv);
                        iLiWidth = parseInt(eLi.css('width'), 10);
                        jQuery('>:first-child', eDiv).before(eLi.clone());
                        eDiv.css({'width': parseInt(eDiv.css('width'), 10) + parseInt(eLi.css('width'), 10) + 'px', 'left': -(iLiWidth / 2) + 'px'});
                        updateSlides();
                        eDiv.animate({'left': -iLiWidth}, 700, function () {
                            jQuery('>:first-child', eDiv).remove();
                            eDiv.css('left', '0px');
                            bProcessing = false;
                        });
                    } else if (iScrollDir === -2) {
                        eLi = jQuery('>:first-child', eDiv);
                        iLiWidth = parseInt(eLi.css('width'), 10);
                        jQuery('>:last-child', eDiv).after(eLi.clone());
                        eDiv.css({'width': parseInt(eDiv.css('width'), 10) + parseInt(eLi.css('width'), 10) + 'px', 'left': -(iLiWidth / 2) + 'px'});
                        updateSlides();
                        eDiv.animate({'left': 0}, 700, function () {
                            jQuery('>:last-child', eDiv).remove();
                            eDiv.css('left', '0px');
                            bProcessing = false;
                        });
                    }
                } else {
                    updateSlides();
                }
                iScrollDir = 0;
            };
            if (options.controls) {
                eCtrlForward = options.controls.forward;
                if (eCtrlForward) {
                    eCtrlForward.bind('click', function () {
                        if (bProcessing) {
                            return false;
                        }
                        bProcessing = true;
                        iScrollDir = 1;
                        controller.scroll(iSlidesPerPage);
                    });
                }
                eCtrlEnd = options.controls.end;
                if (eCtrlEnd) {
                    eCtrlEnd.bind('click', function () {
                        if (bProcessing) {
                            return false;
                        }
                        bProcessing = true;
                        iScrollDir = 2;
                        controller.scroll(Number.MAX_VALUE);
                    });
                }
                eCtrlBack = options.controls.back;
                if (eCtrlBack) {
                    eCtrlBack.bind('click', function () {
                        if (bProcessing) {
                            return false;
                        }
                        bProcessing = true;
                        iScrollDir = -1;
                        controller.scroll(-iSlidesPerPage);
                    });
                }
                eCtrlStart = options.controls.start;
                if (eCtrlStart) {
                    eCtrlStart.bind('click', function () {
                        if (bProcessing) {
                            return false;
                        }
                        bProcessing = true;
                        iScrollDir = -2;
                        controller.scroll(Number.MIN_VALUE);
                    });
                }
            }
            loadData = function (event) {
                if (!event) {
                    throw 'No slides - can\'t load data';
                }
                arrHtml = event.slides;
                scroll();
            };
            activate = function () {
                var i, n;
                for (i = 0, n = arguments.length; i < n; i += 1) {
                    if (arguments[i]) {
                        arguments[i].css('visibility', 'visible');
                    }
                }
            };
            deactivate = function () {
                var i, n;
                for (i = 0, n = arguments.length; i < n; i += 1) {
                    if (arguments[i]) {
                        arguments[i].css('visibility', 'hidden');
                    }
                }
            };
            this.addEventListener('loadSlides', function (event) {
                if (!event) {
                    throw 'No slides - can\'t load data';
                }
                var iScrollPos = event.scrollPos;
                arrHtml = event.slides;
                scroll();
                if (iScrollPos !== null) {
                    if (iScrollPos === 0) {
                        activate(eCtrlForward, eCtrlEnd, eCtrlBack, eCtrlStart);
                    } else if (iScrollPos < 0) {
                        deactivate(eCtrlForward, eCtrlEnd);
                        activate(eCtrlBack, eCtrlStart);
                    } else {
                        deactivate(eCtrlBack, eCtrlStart);
                        activate(eCtrlForward, eCtrlEnd);
                    }
                } else {
                    deactivate(eCtrlForward, eCtrlEnd, eCtrlBack, eCtrlStart);
                }
            });
            this.addEventListener('endOfSlides', function (event) {
                bProcessing = false;
            });
            if (!options.slidesPerPage) {
                options.slidesPerPage = iSlidesPerPage;
            }
            controller = SliderControllerFactory.getController(options, this);
        };
    jQuery.fn.extend({
        sliderWidget: /** @this {!Object} */function (options) {
            this.getOptions = function () {
                return options;
            };
            return this.each(
                function () {
                    var sliderWidget = new SliderWidget(jQuery(this), options);
                }
            );
        }
    });
}());

jQuery.fn.extend({
    friendRequestsWidget: /** @this {!Object} */function () {
        var createAcceptFriendRequestHandler = function (playerId) {
            return function (event) {
                YAZINO.playerRelationshipsService.acceptFriendRequest(playerId);
            };
        }, createRejectFriendRequestHandler = function (playerId) {
            return function (event) {
                YAZINO.playerRelationshipsService.rejectFriendRequest(playerId);
            };
        };

        return this.each(
            function () {
                var rootElement = jQuery(this),
                    requestCountElement = jQuery('<p/>').addClass('requestCount'),
                    widget = rootElement.find('.friendRequestArea'),
                    friendRequestArea = jQuery('<ul/>').appendTo(widget);

                function updateNumberOfRequests(numberOfRequests) {
                    if (numberOfRequests > 0) {
                        requestCountElement.text(numberOfRequests);
                        requestCountElement.prependTo(rootElement);
                        widget.show();
                    } else {
                        requestCountElement.remove();
                        widget.hide();
                    }
                }

                function updateRequestList(requestList, successCallbackGenerator, failureCallbackGenerator) {
                    friendRequestArea.empty();
                    jQuery.each(requestList, function (index, request) {
                        var listElement = jQuery('<li/>'), divElement = jQuery('<div class=\"friend-request\"/>'), actionDiv = jQuery('<div class="actions">');
                        listElement.append(divElement);

                        divElement.append(jQuery('<p class="name"/>').text(request.nickname));
                        divElement.append(jQuery('<img class="picture" alt=":-)" width="32" height="32">').attr("src",
                            request.status.pictureUrl || YAZINO.configuration.get('contentUrl') + '/images/gloss/friend-bar-none-photo.png'));
                        divElement.append(actionDiv);
                        actionDiv.append(jQuery('<a class="acceptFriendRequest" href="#" title="Accept this player as a friend">Confirm</a>')
                            .click(successCallbackGenerator(request.playerId)));
                        actionDiv.append(jQuery('<a class="rejectFriendRequest" href="#" title="Ignore requests from this player">Ignore</a>')
                            .click(failureCallbackGenerator(request.playerId)));
                        friendRequestArea.append(listElement);
                    });
                }

                YAZINO.playerRelationshipsService.addEventListener("FriendRequestsChanged", function (event) {
                    updateNumberOfRequests(event.friendRequests.length);
                    updateRequestList(event.friendRequests, createAcceptFriendRequestHandler, createRejectFriendRequestHandler);
                });
            }
        );
    }
});


(function ($, YAZINO) {

    var mask,
        surround,
        lightboxKillerClass = 'lightboxKillerClose',
        isShowing = false,
        closeCallback;

    function createFreshElements() {
        mask = $(document.createElement('div'));
        surround = $(document.createElement('div'));
        mask.addClass('masked-container');
        surround.addClass('dialog');
        surround.appendTo(mask);
        closeCallback = undefined;
    }

    function refreshElements() {
        if (mask) {
            mask.remove();
        }
        createFreshElements();
    }

    function hide(callback) {
        isShowing = false;
        $(mask).fadeOut('fast', function () {
            var externalCallback = closeCallback;
            refreshElements();
            if (typeof callback === 'function') {
                callback();
            }
            if (typeof externalCallback === 'function') {
                externalCallback();
            }
        });
    }

    function hideIfShowing(callback) {
        if (isShowing) {
            hide(callback);
        }
    }

    function adjustHeights(currentHeightOfDialog) {
        var currentParentBodyHeight = $('body').height() + 'px';
        currentHeightOfDialog = parseInt(currentHeightOfDialog, 10) + 'px';
        surround.css('height', currentHeightOfDialog);
        mask.css('min-height', currentParentBodyHeight);
    }

    function display(elem, className) {
        var body = $('body'),
            closeButton = $('<span/>')
                .addClass('lightboxMainClose')
                .append($('<img/>')
                    .attr({src: YAZINO.configuration.get('contentUrl') + '/images/lightbox-close.png', alt: 'Close', title: 'Close'})
                    .click(hide)
                    .hover(function () {
                        $(this).attr('src', YAZINO.configuration.get('contentUrl') + '/images/lightbox-close-rollover.png');
                    }, function () {
                        $(this).attr('src', YAZINO.configuration.get('contentUrl') + '/images/lightbox-close.png');
                    }));
        surround.addClass(className);
        surround.children().remove();
        surround.append(closeButton);
        surround.append(elem);
        mask.appendTo(body);
        mask.fadeIn();
        mask.click(function (e) {
            if (e.target === this) { // only if the mask itself is clicked, not children of it
                hide();
            }
        });
        isShowing = true;
        return {
            setCloseCallback: function (callback) {
                closeCallback = callback;
            }
        };
    }

    function setupCloseButtons(container) {
        container.find('.' + lightboxKillerClass).lightboxKillerWidget();
    }

    function showIframe(pathToIFrame, className, isScrollable) {
        var iFrame = $(document.createElement('iframe'));
        iFrame.attr('src', pathToIFrame + (pathToIFrame.indexOf('?') > -1 ? '&' : '?') + 'partial=true');
        iFrame.attr('scrolling', isScrollable ? 'yes' : 'no');
        iFrame.addClass('dialog-contents');
        iFrame.load(function () {
            setupCloseButtons(iFrame.contents());
        });
        return display(iFrame, className);
    }

    function showElem(elem, className) {
        setupCloseButtons(elem);
        return display(elem, className);
    }

    function iframeClickHandler() {
        var className = $(this).attr('data-lightboxWidget-class'),
            isScrollable = $(this).attr('data-lightboxWidget-scrollable') === 'true';
        YAZINO.lightboxWidget.createIframe($(this).attr('href'), className, isScrollable);
        return false;
    }

    function confirmationBox(title, body, onConfirmation) {
        var box = jQuery("<div/>")
            .append("<h1 class='confirmationTitle'>" + title + "</h1>")
            .append("<p class='confirmationBody'>" + body + "</p>")
            .append(jQuery("<span/>").addClass("featureLink")
                .append(jQuery("<a href='#'/>").addClass('cancelButton button').text('Cancel').click(function () {
                    YAZINO.lightboxWidget.kill();
                })))
            .append(jQuery("<span/>").addClass("featureLink")
                .append(jQuery("<a href='#'/>").addClass("confirmButton button").text('Confirm').click(function () {
                    YAZINO.lightboxWidget.kill(hideIfShowing(onConfirmation));
                })));

        showElem(box, "confirmationBox");
    }

    createFreshElements();

    YAZINO.lightboxWidget = YAZINO.lightboxWidget || {};
    YAZINO.lightboxWidget.kill = hideIfShowing;
    YAZINO.lightboxWidget.createIframe = showIframe;
    YAZINO.lightboxWidget.createFromElem = showElem;
    YAZINO.lightboxWidget.isOpen = function () { return isShowing; };
    YAZINO.lightboxWidget.adjustHeights = adjustHeights;
    YAZINO.lightboxWidget.confirmationBox = confirmationBox;

    $(document).ready(function () {
        $('body').keydown(function (e) {
            if (e.keyCode === 27) {
                hideIfShowing();
            }
            return true;
        });
    });

    $.fn.extend({
        lightboxWidget: /** @this {!Object} */function (config) {
            this.each(
                function () {
//                    switch (config.type) {
//                    default:
                    jQuery(this).click(iframeClickHandler);
//                        break;
//                    }
                }
            );
        },
        lightboxKillerWidget: /** @this {!Object} */function () {
            this.each(
                function () {
                    $(this).click(YAZINO.lightboxWidget.kill);
                }
            );
        }
    });

}(jQuery, YAZINO));


(function () {
    var FacebookTableInviteWidget = /** @constructor */function (model, gameType, privateTableWidget) {
        var inviteWidget = privateTableWidget.find(".invite"),
            mask = jQuery("#maskedContainer");
        this.show = function () {
            var data = {
                gameType: gameType,
                tableId: model.getTableId(),
                tableName: inviteWidget.find(".tableName").text(),
                templateName: inviteWidget.find(".templateName").text()
            };


            mask.empty();
            mask.html('<iframe scrolling="no" id="facebookFriendsIFrame" src="/friends/inviteToPrivateTableFromFacebook?' + jQuery.param(data) + '" class="dialog" frameborder="0"></iframe>');
            mask.fadeIn();
        };
    },
        TableInviteWidget = /** @constructor */function (model, gameType, privateTableWidget, friendSelectorService) {
            var inviteWidget = privateTableWidget.find(".invite"),
                widget = jQuery(".tableInviteWidget"),
                template = widget.find(".friendTemplate"),
                container = widget.find(".friendsContainer"),
                countWidget = widget.find(".selectedFriendsCount"),
                showDialog = function () {
                    widget.find(".friendSelectionContainer").hide();
                    widget.find(".dialogContainer").show();
                },
                showLoading = function () {
                    showDialog();
                    widget.find(".dialogContainer .sendingInvitations").show();
                    widget.find(".dialogContainer .invitationResult").hide();
                },
                createSelectionHandler = function (friendId, friendWidget) {
                    return function () {
                        var frame = friendWidget.find(".backgroundFrame");
                        if (frame.hasClass("active")) {
                            friendSelectorService.deselectFriend(friendId);
                            frame.removeClass("active");
                        } else {
                            friendSelectorService.selectFriend(friendId);
                            frame.addClass("active");
                        }
                        countWidget.text(container.find("div.active").length);
                        if (container.find("div.active").length > 0) {
                            jQuery("#window-btn-invite").click(function () {
                                jQuery("#window-btn-invite").unbind('click');
                                showLoading();
                                var message = widget.find(".msg").val();
                                friendSelectorService.sendInvites(model.getTableId(), message);
                            });
                        } else {
                            jQuery("#window-btn-invite").unbind('click');
                        }
                    };
                },
                closeWidget = function () {
                    jQuery("#window-select-friends").hide();
                },
                openWidget = function () {
                    widget.find(".dialogContainer").hide();
                    widget.find(".friendSelectionContainer").show();
                    jQuery("#window-select-friends").show();
                },
                showInvitationResult = function () {
                    showDialog();
                    widget.find(".dialogContainer .sendingInvitations").hide();
                    widget.find(".dialogContainer .invitationResult").show();
                };
            friendSelectorService.addEventListener("AvailableFriendsChanged", function (event) {
                var friends = event.friends, friend, friendId, friendWidget, i;
                if (friends) {
                    container.empty();
                    for (i = 0; i < friends.length; i += 1) {
                        friend = friends[i];
                        friendId = friend.playerId;
                        friendWidget = template.clone();
                        if (friend.status.online) {
                            friendWidget.find("p").addClass("online");
                        }
                        friendWidget.find(".friendName").text(friend.nickname);
                        friendWidget.find("img").attr("src", friend.pictureUrl);
                        friendWidget.click(createSelectionHandler(friendId, friendWidget));
                        friendWidget.show().appendTo(container);
                    }
                }
            });
            friendSelectorService.addEventListener("FriendsInvited", function (event) {
                showInvitationResult();
                widget.find(".dialogContainer .invitationResult .failure").hide();
                widget.find(".dialogContainer .invitationResult .success").show();
            });
            friendSelectorService.addEventListener("FriendsInvitationError", function (event) {
                showInvitationResult();
                widget.find(".dialogContainer .invitationResult .success").hide();
                widget.find(".dialogContainer .invitationResult .failure").show();
            });
            this.exists = function () {
                return widget && widget.length;
            };
            this.show = function () {
                friendSelectorService.loadFriends();
                widget.find(".tableName").text(inviteWidget.find(".tableName").text());
                widget.find(".gameType").text(gameType);
                widget.find(".tableDetails").text(inviteWidget.find(".stakeDescription").text());
                widget.find(".tableInviteLink").val(window.location.protocol + "//" + window.location.host
                    +  "/table/" + model.getTableId());
                openWidget();
                widget.find('.scroll-pane').jScrollPane({ showArrows: true, scrollbarWidth: 19, dragMaxHeight: 43 });
                widget.find(".closeButton").click(closeWidget);
            };
        };
}());

jQuery.fn.extend({
    tableTabbedPanelWidget: /** @this {!Object} */function (fnCallback) {
        return this.each(
            function () {
                var widget = jQuery(this),
                    ePrevH3,
                    eDiv,
                    eH3s = jQuery('h3', widget);
                jQuery(this).bind('click', function (event) {
                    if (event.target.nodeName.toUpperCase() === 'H3') {
                        if (event.target === ePrevH3) {
                            return false;
                        }
                        if (widget.hasClass('roll')) {
                            widget.removeClass('roll');
                        } else {
                            eDiv = jQuery('div.roll:first', widget);
                            if (eDiv.length) {
                                eDiv.removeClass('roll');
                            }
                        }
                        if (ePrevH3) {
                            jQuery(ePrevH3).data('active', false);
                        }
                        this.handleTabClick(event, ePrevH3);
                        ePrevH3 = event.target;
                        jQuery(ePrevH3).data('active', true);
                    }
                });
                eH3s.bind('mouseenter', function (event) {
                    var eH3 = jQuery(event.target);
                    if (eH3.data('active')) {
                        return false;
                    }
                    eH3.parent().addClass('roll');
                });
                eH3s.bind('mouseleave', function (event) {
                    var eH3 = jQuery(event.target);
                    if (eH3.data('active')) {
                        return false;
                    }
                    eH3.parent().removeClass('roll');
                });
                ePrevH3 = eH3s[0];
                jQuery(ePrevH3).data('active', true);
                fnCallback.apply(this);
            }
        );
    }
});

jQuery.fn.extend({
    lobbyInformationWidget: /** @this {!Object} */function (lobbyService) {
        return this.each(
            function () {
                var widget = jQuery(this),
                    gameType = widget.attr("gameType") || "";
                YAZINO.logger.debug("Initialising lobbyInformationWidget");
                lobbyService.addEventListener("LobbyInformationChanged_" + gameType, function (event) {
                    widget.find(".onlinePlayers").text(event.details.onlinePlayers);
                    widget.find(".activeTables").text(event.details.activeTables);
                });
                YAZINO.playerService.addEventListener("FriendsSummaryChanged", function (event) {
                    YAZINO.logger.debug("Received message online: " + event.summary.online
                        + "; friends:" + event.summary.friends);
                    widget.find(".onlineFriends").text(event.summary.online);
                    widget.find(".totalFriends").text(event.summary.friends);
                });
                YAZINO.playerService.addEventListener("TableInvitesChanged", function (event) {
                    widget.find('span.invitations:first').text((event.data) ? event.data.length : 0);
                });
            }
        );
    }
});

jQuery.fn.extend({
    notificationWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                if (YAZINO.configuration.get('inGame')) {
                    return;
                }
                var template = jQuery(this),
                    animationTime = 200,
                    open = function (event) {
                        var currentPanelId = 'notificationWidget-' + new Date().getTime(),
                            warningPanel = template.clone(),
                            message = event.displayMessage,
                            closePanel = function () {
                                warningPanel.slideUp(animationTime, function () {
                                    warningPanel.remove();
                                });
                            };
                        warningPanel.attr('id', currentPanelId);
                        warningPanel.insertAfter(template);
                        if (event.action && event.action.name) {
                            message += ' <strong>' + event.action.name + '</strong>';
                        }
                        if (event.style) {
                            jQuery('#' + currentPanelId).addClass('site-warning-' + event.style);
                        }
                        jQuery('#' + currentPanelId + ' .message').html(message);
                        warningPanel.unbind("click");
                        warningPanel.bind("click", function () {
                            if (event.action && event.action.handler && typeof event.action.handler === "function") {
                                event.action.handler();
                            }
                            setTimeout(function () {
                                warningPanel.slideUp(animationTime);
                            }, 200);
                        });
                        jQuery('#' + currentPanelId + ' .button-close').bind("click", function () {
                            setTimeout(closePanel, 200);
                            return false;
                        });
                        if (event.duration > 0) {
                            setTimeout(closePanel, event.duration * 1000);
                        }
                        warningPanel.slideDown(animationTime);
                    };
                YAZINO.notificationService.addEventListener("NotificationEvent", function (event) {
                    open(event);
                });
            }
        );
    }
});

jQuery.fn.extend({
    autoSubmitOnAnyChange: /** @this {!Object} */function () {
        return this.each(
            function () {
                var formElem = jQuery(this),
                    submitParentForm = function () {
                        formElem.submit();
                    };
                formElem.find('input[type="submit"]').hide();
                formElem.find('input, select, checkbox').change(submitParentForm);
                formElem.find('checkbox').click(submitParentForm);
            }
        );
    }
});

jQuery.fn.extend({
    dropMenuWidget: /** @this {!Object} */function (divToDisplay) {
        return this.each(
            function () {
                var widget = jQuery(this), dropMenu = widget.find(divToDisplay);
                widget.hover(function () {
                    dropMenu.show();
                }, function () {
                    dropMenu.hide();
                });
            }
        );
    }
});

jQuery.fn.extend({
    loaderSoundControlWidget: /** @this {!Object} */function (gameLoaderService) {
        return this.each(
            function () {
                var widget = jQuery(this);
                widget.find('input[type="radio"]').change(
                    function () {
                        gameLoaderService.setIsMuted(jQuery(this).val() === 'false');
                    }
                );
            }
        );
    }
});

(function ($) {
    $.fn.extend({
        dailyAwardWidget: /** @this {!Object} */function (dailyAwardService) {
            return this.each(
                function () {
                    var container = $(this);
                    dailyAwardService.addEventListener("DailyAwardPopupShown", function (event) {
                        var dailyAwardDetails = event.dailyAwardDetails,
                            image,
                            totalDaysForProgressiveAward = 4,
                            currentDayRendering = 0,
                            currentDayDisplay,
                            lightboxControl,
                            addBanner = function (container, imgSrc, imgLink) {
                                if (imgSrc) {
                                    image = $(document.createElement('img'));
                                    image.attr("src", imgSrc);
                                    if (imgLink) {
                                        $(document.createElement('a')).attr('href', imgLink)
                                            .appendTo(container).append(image);
                                    } else {
                                        container.append(image);
                                    }
                                }
                            };

                        addBanner(container.find('.main'), dailyAwardDetails.mainImage, dailyAwardDetails.mainImageLink);
                        addBanner(container.find('.second'), dailyAwardDetails.secondaryImage, dailyAwardDetails.secondaryImageLink);
                        container.find(".balance").text(YAZINO.util.formatCurrency(dailyAwardDetails.balance));

                        while (currentDayRendering <= totalDaysForProgressiveAward) {
                            currentDayDisplay = $(document.createElement('div'));
                            currentDayDisplay.addClass('dayDisplay').addClass('day' + currentDayRendering);

                            if (currentDayRendering > dailyAwardDetails.consecutiveDaysPlayed) {
                                currentDayDisplay.addClass('future');
                                if ((currentDayRendering - 1) === dailyAwardDetails.consecutiveDaysPlayed) {
                                    currentDayDisplay.addClass('tomorrow');
                                    $(document.createElement('h4')).text('TOMORROW').appendTo(currentDayDisplay);
                                }
                            } else if (currentDayRendering === dailyAwardDetails.consecutiveDaysPlayed) {
                                currentDayDisplay.addClass('today');
                                $(document.createElement('h4')).text('TODAY').appendTo(currentDayDisplay);
                            } else {
                                currentDayDisplay.addClass('past');
                            }
                            $(document.createElement('p')).addClass('chips').text(YAZINO.util.formatCurrency(dailyAwardDetails.promotionValueList[currentDayRendering], 0)).appendTo(currentDayDisplay);
                            container.find('.dailyBonusDisplay').append(currentDayDisplay);
                            currentDayRendering += 1;
                        }

                        container.remove().show().mapInternalUrls().newTabForExternalLinks();
                        lightboxControl = YAZINO.lightboxWidget.createFromElem(container, 'dailyAwardLightbox');
                        lightboxControl.setCloseCallback(dailyAwardService.dismissDialogue);
                    });
                }
            );
        }
    });
}(jQuery));

(function () {
    var createLauncher = function (page) {
        return function (id) {
            window.open(
                page + id,
                YAZINO.configuration.get('gameServerTitle') + id,
                ""
            );
        };
    }, createLeaveHandler = function () {
        var result = {},
            tableId,
            sendLeaveCommand = function () {
                if (!tableId) {
                    return;
                }
                jQuery.ajax({
                    url: YAZINO.configuration.get('secureCommandUrl') + '/giga',
                    datatype: 'html',
                    async: false,
                    type: 'POST',
                    data: tableId + '|-1|Leave'
                });
            };

        result.setTableId = function (newTableId) {
            tableId = newTableId;
        };

        if (YAZINO.configuration.get('inGame')) {
            jQuery(window).unload(sendLeaveCommand);
        }

        return result;
    }, leaveHandler = createLeaveHandler();

    window.StrataPartnerApi = {
        launchTable: createLauncher(YAZINO.configuration.get('launchPage')),
        launchTournament: createLauncher(YAZINO.configuration.get('tournamentLaunchPage')),
        openCashier: function (trackingRef) {
            if (typeof trackingRef === 'undefined') {
                trackingRef = 'from_flash';
            }
            window.showPaymentsOverlay(trackingRef);
        },
        leaveTable: window.close,
        setTableId: leaveHandler.setTableId,
        isAutoPosting: function () {
            var currentHasPermission, originalHasPermission, parentCurrentHasPermission, parentOriginalHasPermission;
            currentHasPermission = window.frames.publish_iframe_current ? window.frames.publish_iframe_current.hasPublishStreamPermission : false;
            originalHasPermission = window.frames.publish_iframe_original ? window.frames.publish_iframe_original.hasPublishStreamPermission : false;
            parentCurrentHasPermission = window.parent.frames.publish_iframe_current ? window.parent.frames.publish_iframe_current.hasPublishStreamPermission : false;
            parentOriginalHasPermission = window.parent.frames.publish_iframe_original ? window.parent.frames.publish_iframe_original.hasPublishStreamPermission : false;
            return currentHasPermission || originalHasPermission || parentCurrentHasPermission || parentOriginalHasPermission;
        }
    };
}());

(function (yaz) {
    var config = yaz.configuration,
        track = yaz.businessIntelligence.track.invite;

    yaz.setupInvitations = function (invitationService) {
        var endProcess = yaz.lightboxWidget.kill;

        function startProcess(ctaRef) {
            var isFacebook = YAZINO.configuration.get('onCanvas'),
                startUrl = isFacebook ? '/invitation/facebook' : '/invitation',
                lightbox = yaz.lightboxWidget.createIframe(startUrl, 'invitationDialog', false);

            track.open(isFacebook ? 'FACEBOOK' : 'EMAIL');
            lightbox.setCloseCallback(invitationService.hidePopup);
        }

        invitationService.addEventListener("PopupVisibilityChanged", function (event) {
            if (event.isVisible) {
                startProcess(event.ctaRef);
            } else {
                endProcess();
            }
        });

        yaz.action.create('inviteFriends', invitationService.triggerPopup);

        invitationService.triggerPopupIfNotTriggeredRecently();
    };

}(YAZINO));


jQuery.fn.extend({
    inviteFriendReminderWidget: /** @this {!Object} */function (inviteFriendsReminderService) {
        return this.each(
            function () {
                var widget = jQuery(this),
                    link = widget.find('a.sendReminder'),
                    recipientId = widget.attr('data-invite-recipient-id'),
                    source = widget.attr('data-invite-source');

                if (!recipientId) {
                    throw {
                        message: "Missing recipient id."
                    };
                }

                if (!source) {
                    throw {
                        message: "Missing source."
                    };
                }

                inviteFriendsReminderService.addEventListener("reminderSentEvent." + recipientId + "." + source, function () {
                    widget.removeClass('pending');
                    widget.addClass('reminded');
                    // Update HTML structure (the bits not controlled by CSS) to match what would be
                    // rendered on reload - we need DRYer solution but deferring this as about to
                    // revisit this when adding search filter
                    widget.find(".sendReminder").hide();
                    widget.find(".status").text("Pending");
                    widget.find(".reminderSent").show();
                });

                link.click(function () {
                    if (widget.hasClass('pending')) {
                        inviteFriendsReminderService.sendReminder(recipientId, source);
                    }
                    return false;
                });
            }
        );
    }
});

jQuery.fn.extend({
    invitationStatementWidget: /** @this {!Object} */function () {
        return this.each(
            function () {
                var widget = jQuery(this),
                    link = widget.find('.sendMoreInvitations > .featureLink');

                link.click(function () {
                    var parentContext = window.parent.YAZINO;
                    parentContext.lightboxWidget.kill(function () {
                        parentContext.action.run('inviteFriends', 'INVITE_STATEMENT');
                    });
                    return false;
                });
            }
        );
    }
});

(function ($, yaz) {
    yaz.initSliderWidget = function (tableService) {
        var sInviteFriendsUrl = (YAZINO.configuration.get('facebookConnect') && YAZINO.configuration.get('facebookCanvasActionsAllowed')) ? '/lobby/inviteFacebookFriends' : '/lobby/friends?initialView=inviteFriends',
            sliderWidgetFriends = $('#friends-panel div.tab-body-1:first ul.slides:first'),
            sliderWidgetWorld = $('#friends-panel div.tab-body-2:first ul.slides:first'),
            sliderWidgetInvites = $('#friends-panel div.tab-body-3:first ul.slides:first'),
            facebookFriendsSelectorService = new YAZINO.FacebookFriendsSelectorService(),
            publishIframeContainerCurrent,
            publishIframeContainerOriginal,
            publishIframeCurrent,
            publishIframeOriginal,
            VisibilityController = /** @constructor */function () {
                YAZINO.EventDispatcher.apply(this);
                var self = this;
                this.elementVisible = function (name, visible) {
                    self.dispatchEvent({
                        eventType: "VisibilityChanged",
                        elementName: name,
                        visible: visible
                    });
                };
            },
            visibilityController = new VisibilityController(),
            friendsPanel;
        $("#cash-game-head").tableTabbedPanelWidget(function () {
            var widget = jQuery(this), header = widget, eContainer = widget.next();
            this.handleTabClick = function (event, ePrevH3) {
                var iNewIdx = 1, iOldIdx = 1, eH3 = jQuery(event.target);
                ePrevH3 = jQuery(ePrevH3);
                while ((ePrevH3 = jQuery(ePrevH3.prev())).length) {
                    iOldIdx += 1;
                }
                while ((eH3 = jQuery(eH3.prev())).length) {
                    iNewIdx += 1;
                }
                widget.removeClass('check-' + iOldIdx);
                widget.addClass('check-' + iNewIdx);
                if (iNewIdx === 2) {
                    jQuery("#cash-game-body div.line-7-error:first").css("visibility", "hidden");
                }
                jQuery('div.tab-' + iOldIdx  + ':first', eContainer).hide();
                jQuery('div.tab-' + iNewIdx + ':first', eContainer).show();
            };
        });
        $("#friends-panel-head").tableTabbedPanelWidget(function () {
            var widget = jQuery(this), eFriendsPanel = $('#friends-panel');
            this.handleTabClick = function (event, ePrevH3) {
                jQuery('div.active:first', widget).removeClass('active');
                var eH3 = jQuery(event.target), sTab;
                eH3.parent().addClass('active');
                sTab = eH3.text().toUpperCase();
                if (sTab.indexOf('FRIENDS') >= 0) {
                    $('div.tab-body-1:first', eFriendsPanel).show();
                    $('div.tab-body-2:first', eFriendsPanel).hide();
                    $('div.tab-body-3:first', eFriendsPanel).hide();
                    visibilityController.elementVisible("WORLD", false);
                } else if (sTab.indexOf('WORLD') >= 0) {
                    $('div.tab-body-1:first', eFriendsPanel).hide();
                    $('div.tab-body-2:first', eFriendsPanel).show();
                    $('div.tab-body-3:first', eFriendsPanel).hide();
                    visibilityController.elementVisible("WORLD", true);
                } else if (sTab.indexOf('MY INVITES') >= 0) {
                    $('div.tab-body-1:first', eFriendsPanel).hide();
                    $('div.tab-body-2:first', eFriendsPanel).hide();
                    $('div.tab-body-3:first', eFriendsPanel).show();
                    visibilityController.elementVisible("WORLD", false);
                } else {
                    throw 'FriendsPanelTabs: unknown tab \'' + sTab + '\'';
                }
            };
        });
        sliderWidgetFriends.sliderWidget({
            service: YAZINO.playerRelationshipsService,
            facebookFriendsSelectorService: facebookFriendsSelectorService,
            type: YAZINO.slider.type.FRIENDS,
            preProcessData: function (arrFriends) {
                var i, j, n, m, oPlayer, friends = [], sNickname, oLocation;
                for (i = 0, n = arrFriends.length; i < n; i += 1) {
                    oPlayer = arrFriends[i];
                    for (j = 0, m = oPlayer.status.locations.length; j < m; j += 1) {
                        oLocation = oPlayer.status.locations[j];
                        sNickname = oPlayer.status.nickname;
                        if (sNickname.length > 15) {
                            sNickname = sNickname.substring(0, 15) + '...';
                        }
                        if (oLocation.displayStakeLevel) {
                            friends.push({
                                'nickname': sNickname,
                                'pictureUrl': oPlayer.status.pictureUrl,
                                'balanceSnapshot': YAZINO.util.formatCurrency(oPlayer.status.balanceSnapshot, 0, '.', ',', '$'),
                                'gameType': oLocation.gameType,
                                'level': oPlayer.levels[oLocation.gameType],
                                'displayGameType': oLocation.displayGameType,
                                'displayStakeLevel': oLocation.displayStakeLevel,
                                'locationId': oLocation.locationId,
                                'playerId': oPlayer.playerId
                            });
                        }
                    }
                }
                return friends;
            },
            facebookFriends: [],
            drawSlide: function (oPlayer) {
                var eHtml,
                    sHtml = '<div class="friend-bar color-' + oPlayer.gameType + ' font-friend-bar" style="z-index:1330">' +
                        '<div class="on" style="z-index:1320"><h3 class="font-friend-bar-head ">' + oPlayer.nickname + '</h3>' +
                        '<div style="z-index: 1310;"><img width="52" height="52" alt="" src="' + oPlayer.pictureUrl + '">' +
                        '<div class="font-friend-circle circle" style="z-index: 1300;">' + oPlayer.level + '</div>' + '</div>' +
                        '<p><span class="bold">' + oPlayer.displayGameType + '</span></p>' +
                        '<p>' + oPlayer.displayStakeLevel + '</p><p><span class="grey">' + oPlayer.balanceSnapshot +
                        '</span></p><a href="#" class="join">JOIN</a>' +
                        '</div><div class="off font-friend-bar-grey"><h3 class="font-friend-bar-head">Invite</h3>' +
                        '<div><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/friend-bar-none-photo.png" alt=""  width="52" height="52"/>' +
                        '<img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/free-chips.png" alt="free chips!" style="z-index:2000;position:absolute;top:35px;left:30px" width="60" height="60"/>' +
                        '</div><b><p></p><p><p><p>INVITE NOW<p>& GET</p><a href="yazino:inviteFriends">FREE CHIPS</a></p></p></p></b></div></div>';
                eHtml = jQuery(sHtml);
                jQuery('a.join:first', eHtml).bind('click', function () {
                    tableService.launchTableById(oPlayer.gameType, oPlayer.locationId, 'self');
                });
                return eHtml;
            },
            drawFacebookSlide: function (facebookFriend) {
                var pictureUrl;
                if (typeof facebookFriend.picture === 'object') {
                    pictureUrl = facebookFriend.picture.data.url; // Facebook October 2012 breaking API change
                } else {
                    pictureUrl = facebookFriend.picture;
                }
                return jQuery('<div class="friend-bar color-grey font-friend-bar "  style="z-index:1330">' +
                    '<div class="off font-friend-bar-grey" style="color:#575757" ><h3 class="font-friend-bar-head"  style="color:#575757">' + facebookFriend.name + '</h3>' +
                    '<div><img src="' + pictureUrl + '" alt=""  width="52" height="52"/><img src="' +
                    YAZINO.configuration.get('contentUrl') + '/images/gloss/free-chips.png" alt="free chips!" style="z-index:2000;position:absolute;top:35px;left:30px" width="60" height="60"/>' +
                    '</div><b><p></p><p><p><p>INVITE NOW<p>& GET</p><a href="yazino:inviteFriends">FREE CHIPS</a></p></p></p></b></div></div>');
            },
            drawEmptySlide: function () {
                return jQuery('<div class="friend-bar color-grey font-friend-bar" style="z-index:1330">' +
                    '<div class="off font-friend-bar-grey" style="color:#575757"><h3 class="font-friend-bar-head" style="color:#575757">Invite</h3>' +
                    '<div><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/friend-bar-none-photo.png" alt=""  width="52" height="52"/><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/free-chips.png" alt="free chips!" style="z-index:2000;position:absolute;top:35px;left:30px" width="60" height="60"/>' +
                    '</div><b><p></p><p><p><p>INVITE NOW<p>& GET</p><a href="yazino:inviteFriends" data-ctaContext="FRIENDS_EMPTY_PANEL">FREE CHIPS</a></p></p></p></b></div></div>');
            },
            eventName: (sliderWidgetFriends.attr('gameType')) ? 'FriendsRelationshipsChanged_' + sliderWidgetFriends.attr('gameType') : 'FriendsRelationshipsChanged',
            facebookEventName: 'UnregisteredFacebookFriends', // friends on facebook not yet registered with Yazino.
            controls: {
                back: $('#friends-panel div.tab-body-1:first p.right-slide1:first'),
                forward: $('#friends-panel div.tab-body-1:first p.left-slide1:first')
            }
        });
        sliderWidgetWorld.sliderWidget({
            service: YAZINO.playerRelationshipsService,
            visibilityController: visibilityController,
            type: YAZINO.slider.type.WORLD,
            preProcessData: function (arrPlayers) {
                var i, j, n, m, oPlayer, worldPlayers = [], sNickname, oLocation, bShow;
                for (i = 0, n = arrPlayers.length; i < n; i += 1) {
                    oPlayer = arrPlayers[i];
                    for (j = 0, m = oPlayer.locations.length; j < m; j += 1) {
                        oLocation = oPlayer.locations[j];
                        sNickname = oPlayer.nickname;
                        if (sNickname.length > 15) {
                            sNickname = sNickname.substring(0, 15) + '...';
                        }
                        // the following line is a temporary filter for the canvas test
                        bShow = !YAZINO.configuration.get('onCanvas') || YAZINO.configuration.get('gameType') === oLocation.gameType;
                        if (oLocation.displayStakeLevel && bShow) {
                            worldPlayers.push({
                                'nickname': sNickname,
                                'pictureUrl': oPlayer.pictureUrl,
                                'balanceSnapshot': YAZINO.util.formatCurrency(oPlayer.balanceSnapshot, 0, '.', ',', '$'),
                                'gameType': oLocation.gameType,
                                'level': oPlayer.levels[oLocation.gameType],
                                'displayGameType': oLocation.displayGameType,
                                'displayStakeLevel': oLocation.displayStakeLevel,
                                'locationId': oLocation.locationId,
                                'playerId': oPlayer.playerId
                            });
                        }
                    }
                }
                return worldPlayers;
            },
            drawSlide: function (oPlayer) {
                var eHtml,
                    sHtml = '<div class="friend-bar color-' + oPlayer.gameType + ' font-friend-bar" style="z-index:1330">' +
                        '<div class="on" style="z-index:1320"><h3 class="font-friend-bar-head">' + oPlayer.nickname + '</h3>' +
                        '<div style="z-index: 1310;"><img width="52" height="52" alt="" src="' + oPlayer.pictureUrl + '">' +
                        '<div class="font-friend-circle circle" style="z-index: 1300;">' + oPlayer.level + '</div>' + '</div>' +
                        '<p><span class="bold">' + oPlayer.displayGameType + '</span></p>' +
                        '<p>' + oPlayer.displayStakeLevel + '</p><p><span class="grey">' + oPlayer.balanceSnapshot +
                        '</span></p><a href="#" class="join">JOIN</a>' +
                        '</div><div class="off font-friend-bar-grey"><h3 class="font-friend-bar-head">Invite</h3>' +
                        '<div><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/friend-bar-none-photo.png" alt=""  width="52" height="52"/>' +
                        '<img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/free-chips.png" alt="free chips!" style="z-index:2000;position:absolute;top:35px;left:30px" width="60" height="60"/>' +
                        '</div><b><p></p><p><p><p>INVITE NOW<p>& GET</p><a href="yazino:inviteFriends" data-ctaContext="WORLD_PANEL">FREE CHIPS</a></p></p></p></b></div></div>';
                eHtml = jQuery(sHtml);
                jQuery('a.join:first', eHtml).bind('click', function () {
                    tableService.launchTableById(oPlayer.gameType, oPlayer.locationId, 'self');
                });
                return eHtml;
            },
            drawEmptySlide: function () {
                return jQuery('<div class="friend-bar color-grey font-friend-bar" style="z-index:1330">' +
                    '<div class="off font-friend-bar-grey" style="color:#575757"><h3 class="font-friend-bar-head" style="color:#575757">Invite</h3>' +
                    '<div><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/friend-bar-none-photo.png" alt=""  width="52" height="52"/><img src="' + YAZINO.configuration.get('contentUrl') + '/images/gloss/free-chips.png" alt="free chips!" style="z-index:2000;position:absolute;top:35px;left:30px" width="60" height="60"/>' +
                    '</div><b><p></p><p><p><p>INVITE NOW<p>& GET</p><a href="yazino:inviteFriends" data-ctaContext="WORLD_EMPTY_PANEL">FREE CHIPS</a></p></p></p></b></div></div>');
            },
            eventName: (sliderWidgetWorld.attr('gameType')) ? 'PlayersOnlineChanged_' + sliderWidgetWorld.attr('gameType') : 'PlayersOnlineChanged',
            controls: {
                back: $('#friends-panel div.tab-body-2:first p.right-slide2:first'),
                forward: $('#friends-panel div.tab-body-2:first p.left-slide2:first')
            }
        });
        sliderWidgetInvites.sliderWidget({
            service: YAZINO.playerService,
            type: YAZINO.slider.type.INVITES,
            drawSlide: function (oInvite) {
                var eHtml,
                    sHtml = '<div class="friend-bar color-' + oInvite.gameType + ' font-friend-bar">' +
                        '<div class="on"><h3 class="font-friend-bar-head">' + oInvite.invitorName + '</h3>' +
                        '<div class="photo-bar"><img src="' + oInvite.invitorPictureUrl + '" alt=""  width="52" height="52"/>' +
                        '</div><p><span class="invite-grey">invited you to his</span></p>' +
                        '<p><span class="bold">' + oInvite.gameTitle + '</span></p><p><span class="invite-grey">Table</span></p>' +
                        '<a href="#" class="play">PLAY NOW</a></div></div>';
                eHtml = jQuery(sHtml);
                jQuery('a.play:first', eHtml).bind('click', function () {
                    tableService.launchTableById(oInvite.gameType, oInvite.tableId, 'self');
                });
                return eHtml;
            },
            eventName: 'TableInvitesChanged',
            controls: {
                back: $('#friends-panel div.tab-body-3:first p.right-slide3:first'),
                forward: $('#friends-panel div.tab-body-3:first p.left-slide3:first')
            }
        });
        friendsPanel = new YAZINO.FriendsPanel(sliderWidgetFriends, sliderWidgetWorld, sliderWidgetInvites);
        publishIframeContainerCurrent = jQuery("#publish_iframe_container_current");
        if (publishIframeContainerCurrent.length > 0) {
            publishIframeCurrent = jQuery("<iframe frameborder='no' scrolling='no' id='publish_iframe_current' name='publish_iframe_current' src='/lobby/publishCurrent'></iframe>");
            publishIframeContainerCurrent.prepend(publishIframeCurrent);
        } else {
            YAZINO.logger.info('not loading facebook current iframe');
        }

        publishIframeContainerOriginal = jQuery("#publish_iframe_container_original");
        if (publishIframeContainerOriginal.length > 0) {
            publishIframeOriginal = jQuery("<iframe frameborder='no' scrolling='no' id='publish_iframe_original' name='publish_iframe_original' src='/lobby/publishOriginal'></iframe>");
            publishIframeContainerOriginal.prepend(publishIframeOriginal);
        } else {
            YAZINO.logger.info('not loading facebook original iframe');
        }
    };
}(jQuery, YAZINO));

// Declare services and attach behaviors
(function ($) {
    YAZINO.rpcService = YAZINO.createRpcService();
    YAZINO.notificationService = YAZINO.createNotificationService(YAZINO.createNewsEventService(), YAZINO.createFacebookPostingService(new YAZINO.PlainStorageService()));
    YAZINO.playerService = new YAZINO.PlayerService();
    YAZINO.playerRelationshipsService = new YAZINO.PlayerRelationshipsService(YAZINO.createLevelingService());

    $(document).ready(
        function () {
            var tournamentService = new YAZINO.TournamentService(),
                tableService = new YAZINO.TableService(YAZINO.configuration.get('gameConfig')),
                modalDialogueService = YAZINO.createModalDialogueService(),
                inviteFriendsService = new YAZINO.InviteFriendsService(YAZINO.getLocalStorageInstance('inviteFriendsService'), modalDialogueService),
                inviteFriendsReminderService = YAZINO.createInviteFriendsReminderService(inviteFriendsService),
                mask = jQuery("#maskedContainer");

            YAZINO.tableService = tableService;
            (function () {
                var rootElem = $("#dailyChipAward"),
                    dataProvider = YAZINO.legacyDailyAwardDataProvider,
                    dailyAwardService;
                if (YAZINO.configuration.get('contains')("dailyAward")) { // new setup
                    rootElem = $('<div id="dailyChipAward" style="display: none;"><div class="banner main">' +
                        '<p class="balance"></p><div class="dailyBonusDisplay"></div></div><div class="banner second">' +
                        '</div><input type="button" class="lightboxKillerClose yaz-std-close-button" value="Close"/></div>');
                    dataProvider = YAZINO.dailyAwardDataProvider;
                    rootElem.playerBalanceWidget();
                }
                dailyAwardService = YAZINO.createDailyAwardService(modalDialogueService, dataProvider);
                rootElem.dailyAwardWidget(dailyAwardService);
                dailyAwardService.init();
            }());
            $(".countdownWidget").countdownWidget();
            $(".playerBalanceWidget").playerBalanceWidget();
            $(".profileBoxWidget").profileBoxWidget();
            $(".tournamentInfoWidget").tournamentInfoWidget(tournamentService);
            $(".tournamentScheduleWidget").tournamentScheduleWidget(tournamentService);
            $(".trophyLeaderboardWidget").trophyLeaderboardWidget();
            $(".tournamentResultsWidget").tournamentResultsWidget();
            $(".hallOfFameWidget").hallOfFameWidget();
            $(".tournamentLastResultsWidget").tournamentLastResultsWidget();
            $(".tableLauncherWidget").tableLauncherWidget(tableService);
            $(".friendRequestsWidget").friendRequestsWidget();
            $(".lobbyInformationWidget").lobbyInformationWidget(new YAZINO.LobbyService());
            $('.subNav .preferences').dropMenuWidget('.drop-menu');
            $('.loaderSoundControlWidget').loaderSoundControlWidget(new YAZINO.GameLoaderService());
            $('.notificationWidget').notificationWidget();
            $('.iFramePopUp').lightboxWidget({type: "iframe"});
            $('.lightboxKillerClose').lightboxKillerWidget();
            $('.loginForm').yazinoLoginAndRegistrationForm();
            $('.invitee').inviteFriendReminderWidget(inviteFriendsReminderService);

            $(".fbUserDetailsWidget").facebookUserDetailsWidget(YAZINO.FacebookUserDetailsService());
            $(".invitationStatement").invitationStatementWidget();
            $("body").mapInternalUrls();
            $.getJSON('/player/balance?playerId=' + YAZINO.configuration.get('playerId') + '&stopCachingOfRequestInIE=' + new Date().getTime(), function (data) {
                YAZINO.dispatchDocument({eventType: "PLAYER_BALANCE", document : data});
            });

            YAZINO.setupInvitations(inviteFriendsService);
            YAZINO.inviteFriendsReminderSent = inviteFriendsReminderService.reminderSent;
            YAZINO.closeInviteFriends = inviteFriendsService.hidePopup;
            YAZINO.closeTableInviteFriends = function () {
                mask.fadeOut();
                mask.empty();
            };
            YAZINO.dispatchDocument = YAZINO.rpcService.dispatchEvent;
            YAZINO.legacyRpcService = YAZINO.rpcService;
            YAZINO.checkFlash();
            YAZINO.util.resizeIframeLightbox();
            window.StrataPartnerApi.postToFacebook = YAZINO.notificationService.postToFacebook;
            window.StrataPartnerApi.streamPostPopupCalled = window.trackPostPopup;

            if (jQuery("#achievements-overlay").achievementsWidget && jQuery("#achievements-overlay").achievementsWidget().size() === 0) {
                jQuery("body").achievementsContainer();
            }
            YAZINO.achievementsDisplay = jQuery().achievementsDisplay;
            YAZINO.achievementsDisplaySetup = jQuery().achievementsContainerSetUp;

            YAZINO.initSliderWidget(tableService);

            if (YAZINO.util.isParentFrame()) {
                YAZINO.newUserWelcomeMessage(YAZINO.configuration, YAZINO.lightboxWidget, YAZINO.getLocalStorageInstance('newUserWelcomeMessage', true));
            }

            /* refactor to more generic advertising tracking solution */
            $('.appAdverts a').click(function () {
                var platform = $(this).attr('data-platform'),
                    endpoint = YAZINO.businessIntelligence.track.internalAdverts.mobileApps[platform];
                if (typeof endpoint !== 'function') {
                    YAZINO.logger.log('platform [' + platform + '] doesn\'t exist in YAZINO.businessIntelligence.track.internalAdverts.mobileApps');
                }
                endpoint('right-hand-of-game', $(this).attr('data-game'));
            });
        }
    );
}(jQuery));

// Interactions for /player area

YAZINO.util.formFieldStorageUtil = (function ($) {
    var defaultConfig = {
            acceptedValueAttr: 'data-yaz-util-formstorage-accepted-value',
            editedValueAttr: 'data-yaz-util-formstorage-edited-value'
        };

    function init(initConfig) {
        var config = {};

        jQuery.extend(true, config, defaultConfig, initConfig || {});

        function fieldHasAcceptedValue(field) {
            return !!$(field).attr(config.acceptedValueAttr);
        }

        function setAcceptedValueForField(field, acceptedValue) {
            $(field).attr(config.acceptedValueAttr, acceptedValue);
        }

        function getAcceptedValueForField(field) {
            return $(field).attr(config.acceptedValueAttr);
        }

        function getHumanReadableValueFromFieldAndValue(fieldToRead, technicalValue) {
            var field = $(fieldToRead);
            if (field.is('select')) {
                return field.find('option[value="' + technicalValue + '"]').text();
            }
            if (field.is("[type='password']")) {
                return '*****';
            }
            return technicalValue;
        }

        function getAcceptedValueForFieldAsHumanReadable(field) {
            return getHumanReadableValueFromFieldAndValue(field, getAcceptedValueForField(field));
        }

        function acceptCurrentValueForField(field) {
            setAcceptedValueForField(field, $(field).val());
        }

        function recoverAcceptValueForField(field) {
            $(field).val($(field).attr(config.acceptedValueAttr));
        }

        function fieldHasEditedValue(field) {
            return !!$(field).hasAttr(config.editedValueAttr);
        }

        function setEditedValueForField(field, acceptedValue) {
            $(field).attr(config.editedValueAttr, acceptedValue);
        }

        function getEditedValueForField(field) {
            return $(field).attr(config.editedValueAttr);
        }

        function storeCurrentEditedValueForField(field) {
            setEditedValueForField(field, $(field).val());
        }

        function initField(field) {
            var storeEditedValueHandlerGenerator = (function (fieldForHandler) {
                var field = fieldForHandler;
                return function () {
                    storeCurrentEditedValueForField(field);
                };
            }(field));
            if (!fieldHasAcceptedValue(field)) {
                acceptCurrentValueForField(field);
            }
            $(field).
                blur(storeEditedValueHandlerGenerator).
                click(storeEditedValueHandlerGenerator);
        }

        return {
            setAcceptedValueForField: setAcceptedValueForField,
            getAcceptedValueForField: getAcceptedValueForField,
            getAcceptedValueForFieldAsHumanReadable: getAcceptedValueForFieldAsHumanReadable,
            acceptCurrentValueForField: acceptCurrentValueForField,
            getEditedValueForField: getEditedValueForField,
            recoverAcceptValueForField: recoverAcceptValueForField,
            initField: initField
        };
    }

    return {
        init: init
    };
}(jQuery));

YAZINO.PlayerProfileUi = (function ($) {
    var defaultConfig;

    defaultConfig = {
        editCtaText: 'EditCtaText',
        formErrorClass: 'errorForm',
        errorMessageClass: 'errorMessage',
        lockFormsWithClass: 'lockedForm'
    };

    function createReadOnlyFieldForField(fieldObj, id) {
        var readOnlyField = $(document.createElement('output'));
        readOnlyField.attr('id', id).addClass('readOnlyField').insertAfter(fieldObj);
    }

    function setReadOnlyFieldValueForField(field, config) {
        var fieldObj = $(field),
            fieldId = fieldObj.attr('id'),
            readOnlyFieldId = fieldId + "_read_only",
            readOnlyField = $('#' + readOnlyFieldId);
        if (!fieldId) {
            return;
        }
        if (readOnlyField.length === 0) {
            createReadOnlyFieldForField(fieldObj, readOnlyFieldId);
            readOnlyField = $('#' + readOnlyFieldId);
        }
        readOnlyField.text(config.fieldStatusManager.getAcceptedValueForFieldAsHumanReadable(field));
    }

    function removeErrorStateFromForm(form, config) {
        $(form).removeClass(config.formErrorClass).find('.' + config.errorMessageClass).remove();
    }

    function isOpen(formToInspect) {
        return !formToInspect.hasClass('closed');
    }

    function findFieldsInForm(form) {
        return $(form).find('input, select, textarea').not('[type="submit"]').not('[type="reset"]').not('[type="hidden"]').not('input[type="file"]');
    }

    function closeForm(formToClose, config) {
        findFieldsInForm(formToClose).each(function () {
            setReadOnlyFieldValueForField(this, config);
            config.fieldStatusManager.recoverAcceptValueForField(this, config);
        });
        removeErrorStateFromForm(formToClose, config);
        $(formToClose).addClass('closed');
        YAZINO.util.resizeIframeLightbox();
    }

    function openForm(formToOpen, config) {
        formToOpen.removeClass('closed');
        YAZINO.util.resizeIframeLightbox();
    }

    function editCtaHandlerGenerator(formToHandle, config) {
        function handler() {
            YAZINO.logger.log(config);
            config.allRelatedForms.each(function () {
                closeForm(this, config);
            });
            openForm(formToHandle);
            return false;
        }
        return handler;
    }

    function resetFormHandlerGenerator(formToHandle, config) {
        function handler() {
            YAZINO.logger.log('Reseting form');
            closeForm(formToHandle, config);
            return false;
        }
        return handler;
    }

    function submitFormHandlerGenerator(formToHandle, config) {
        function handler() {
            /*
            YAZINO.logger.log('Form submit will go here - not yet configured');
            findFieldsInForm(formToHandle).each(function () {
                config.fieldStatusManager.acceptCurrentValueForField(this);
            });
            closeForm(formToHandle, config);
            return false;*/
        }
        return handler;
    }

    function addEditButton(form, config) {
        var editButton = $(document.createElement('a'));
        YAZINO.logger.log('Adding Edit Button.');
        YAZINO.logger.log(config);
        editButton.text(config.editCtaText).attr('href', '#');
        editButton.addClass('editCta');
        editButton.click(editCtaHandlerGenerator(form, config));
        form.append(editButton);
    }

    function addFormEventHandlers(form, config) {
        var submitButton = form.find('input[type="submit"]'),
            resetButton = form.find('input[type="reset"]');
        YAZINO.logger.log('Adding Form Handlers.');
        YAZINO.logger.log(config);
        resetButton.click(resetFormHandlerGenerator(form, config));
        submitButton.click(submitFormHandlerGenerator(form, config));
    }

    function configureFormUiEvents(form, config) {
        if ($(form).hasClass(config.lockFormsWithClass)) {
            return;
        }
        addEditButton(form, config);
        addFormEventHandlers(form, config);
    }

    function init(formElemsInput, initConfig) {
        var formElems = $(formElemsInput),
            config = {},
            fieldStatusManager = YAZINO.util.formFieldStorageUtil.init();

        function setUpAdditionalElementsForForms() {
            formElems.each(function () {
                var currentForm = $(this);
                configureFormUiEvents(currentForm, config);
                findFieldsInForm(currentForm).each(function () {
                    fieldStatusManager.initField($(this));
                });
            });
        }

        function closeAllNonErroringForms() {
            formElems.each(function () {
                var currentForm = $(this);
                if (!currentForm.hasClass(config.formErrorClass)) {
                    closeForm($(this), config);
                }
            });
        }

        function getOpenForm() {
            formElems.each(function () {
                if (isOpen(this)) {
                    return this;
                }
            });
            return false;
        }

        jQuery.extend(true, config, defaultConfig, initConfig || {}, {allRelatedForms: formElems});

        config.fieldStatusManager = fieldStatusManager;

        YAZINO.logger.log('initiating PlayerProfileUi for [' + formElems.length + '] form(s)');

        setUpAdditionalElementsForForms();
        closeAllNonErroringForms();

        return {
            closeAllForms: closeAllNonErroringForms,
            getOpenForm: getOpenForm
        };
    }

    return init;

}(jQuery));

// End of Interactions for /player area

// Start of HTML5 fallback tools

YAZINO.util.html5tools = (function ($) {

    function isHandledByBrowser(elem) {
        return !$(elem).is('[type="text"]'); // if the browser doesn't support this input type it'll fallback to the default 'text'
    }

    function createHtml5DateFromHtml5DateTime(datetime) {
        var val = datetime, timePos = val.indexOf('T');
        YAZINO.logger.log('removing time if present in [' + val + ']');
        if (timePos > -1) {
            val = val.substr(0, timePos);
            YAZINO.logger.log('time removed. value is [' + val + ']');
        }
        return val;
    }

    function setupDateInput(elem, overrideHtml5Browser) {
        var loader,
            inputField,
            calendarElem,
            calendar,
            currentCalendarDate;

        function removeTimeIfPresent() {
            $(inputField).val(createHtml5DateFromHtml5DateTime($(inputField).val()));
        }

        function convertHtmlDateToJsDate(htmlDateString) {
            return new Date(htmlDateString);
        }

        function leftFillLeadingZerosForDayAndMonth(number) {
            return (parseInt(number, 10) < 10 ? '0' : '') + parseInt(number, 10);
        }

        function createHtmlDateFromParts(year, month, day) {
            return year +  '-' + leftFillLeadingZerosForDayAndMonth(month) + '-' + leftFillLeadingZerosForDayAndMonth(day);
        }

        function convertJsDateToHtmlDateString(jsDate) {
            return createHtmlDateFromParts(jsDate.getFullYear(), jsDate.getMonth(), jsDate.getDay());
        }

        if (!overrideHtml5Browser && isHandledByBrowser(elem)) {
            return;
        }

        function setDate(date) {
            var selectedDates, firstDate;
            if (typeof calendar === 'undefined' || typeof calendar.select === 'undefined') {
                YAZINO.logger.log('early return because calendar is not yet initiated while trying to set date.');
                return;
            }
            if (typeof date !== 'Date') {
                date = new Date(date);
            }
            calendar.select(date);
            selectedDates = calendar.getSelectedDates();
            if (selectedDates.length > 0) {
                firstDate = selectedDates[0];
                calendar.cfg.setProperty("pagedate", (firstDate.getMonth() + 1) + "/" + firstDate.getFullYear());
                calendar.render();
            }
        }

        function getDate() {
            return currentCalendarDate;
        }

        function updateDateFromInputField() {
            var fieldValue = $(inputField).val();
            YAZINO.logger.log('updating calendar from field [' + fieldValue + '].');
            if (fieldValue.length < 10) {
                return;
            }
            setDate(fieldValue);
        }

        inputField = elem;

        calendarElem = document.createElement('div');
        $(calendarElem).addClass('calendarForInput').insertAfter(inputField);
        // Instantiate and configure YUI Loader:
        loader = new YAHOO.util.YUILoader({
            base: "",
            require: ["calendar"],
            loadOptional: false,
            combine: true,
            filter: "MIN",
            allowRollup: true,
            onSuccess: function () {

                function handleCalendarDateChange(type, args, obj) {
                    var dates = args[0],
                        date = dates[0],
                        year = date[0],
                        month = date[1],
                        day = date[2],
                        htmlDateString = createHtmlDateFromParts(year, month, day);


                    currentCalendarDate = convertHtmlDateToJsDate(htmlDateString);
                    inputField.value = htmlDateString;

                    $(calendarElem).removeClass('active');
                }

                calendar = new YAHOO.widget.Calendar('', calendarElem, {navigator: {monthFormat: YAHOO.widget.Calendar.SHORT}});
                calendar.selectEvent.subscribe(handleCalendarDateChange, calendar, true);
                calendar.render();

                updateDateFromInputField();

            }
        });

        // Load the files using the insert() method.
        loader.insert();

        $(inputField).focus(function () {
            YAZINO.logger.log('focussed');
            $(calendarElem).addClass('active');
        }).change(function () {
            YAZINO.logger.log('changed');
            updateDateFromInputField();
        });

        removeTimeIfPresent();

        return {
            setDate: setDate,
            getDate: getDate
        };

    }

    return {
        setupDateInput: setupDateInput,
        createHtml5DateFromHtml5DateTime: createHtml5DateFromHtml5DateTime
    };

}(jQuery));

// End of HTML5 fallback tools

// Login and registration form

(function ($) {

    function updateDisplayHandlerGenerator(formElem) {

        function isInNewUserState(formElem) {
            var returnVal = formElem.find('input[value="new-user"]').is(':checked');
            return returnVal;
        }

        function turnRequiredOn(elems) {
            elems.filter('.required-when-enabled').attr('required', 'required');
            elems.find('.required-when-enabled').attr('required', 'required');
        }

        function turnRequiredOff(elems) {
            elems.filter('.required-when-enabled').removeAttr('required');
            elems.find('.required-when-enabled').removeAttr('required');
        }

        function disableInputs(elem) {
            elem.addClass('disabled')
                .find('input[type="text"],input[type="password"],input[type="checkbox"]').attr('disabled', 'disabled');
            turnRequiredOff(elem);
        }

        function enableInputs(elem) {
            elem.removeClass('disabled')
                .find('input[type="text"],input[type="password"],input[type="checkbox"]').removeAttr('disabled');
            turnRequiredOn(elem);
        }

        function showArea(elem) {
            elem.css('display', 'block').find('.required-when-enabled').attr('required', 'required');
        }

        function hideArea(elem) {
            elem.css('display', 'none').find('.required-when-enabled').removeAttr('required');
        }

        return function () {
            var enabledForExistingUsers = formElem.find('.only-active-for-existing-users'),
                enabledForNewUsers = formElem.find('.only-active-for-new-users'),
                showForExistingUsers = formElem.find('.only-shown-for-existing-users, .only-shown-for-existing-users input'),
                showForNewUsers = formElem.find('.only-shown-for-new-users, .only-shown-for-new-users input'),
                isNewUser = isInNewUserState(formElem);
            if (isNewUser) {
                disableInputs(enabledForExistingUsers);
                enableInputs(enabledForNewUsers);
                showArea(showForNewUsers);
                hideArea(showForExistingUsers);
            } else {
                enableInputs(enabledForExistingUsers);
                disableInputs(enabledForNewUsers);
                showArea(showForExistingUsers);
                hideArea(showForNewUsers);
            }
            parent.YAZINO.lightboxWidget.adjustHeights($('body').height());
            return true;
        };
    }

    function initForm(formElem) {
        var updateDisplay = updateDisplayHandlerGenerator(formElem);
        formElem.find('input[value="new-user"], input[value="existing-user"]').click(updateDisplay);
        updateDisplay(); // default view
        formElem.find('#email').eq(0).focus();
    }

    $.fn.extend({
        yazinoLoginAndRegistrationForm: /** @this {!Object} */function () {
            this.each(
                function () {
                    initForm($(this));
                    $(this).find('.facebook img').each(function () {
                        var unhoverImgSrc,
                            hoverImgSrc;
                        $(this).hover(function () {
                            if (!unhoverImgSrc || !hoverImgSrc) {
                                unhoverImgSrc = $(this).attr('src');
                                hoverImgSrc = unhoverImgSrc.replace(".png", "Rollover.png");
                            }
                            $(this).attr('src', hoverImgSrc);
                        }, function () {
                            $(this).attr('src', unhoverImgSrc);
                        });
                    });
                }
            );
        }
    });
}(jQuery));

// Internal URL Mapper

(function ($, yazino) {
    var mappers = {
        'yazino': YAZINO.actions
    };
    yazino.util.internalUrlMapper = {
        map: function (jqElem) {
            var ALLOW_DEFAULT_ACTION = true, PREVENT_DEFAULT_ACTION = false,
                parsedUrl = yazino.util.url.parse(jqElem.attr('href')),
                contextAttrName = 'data-ctaContext',
                detectedContext;

            if (parsedUrl && mappers[parsedUrl.scheme] && typeof mappers[parsedUrl.scheme][parsedUrl.uri] !== 'undefined') {
                detectedContext = jqElem.attr(contextAttrName) || jqElem.closest('[' + contextAttrName + ']').attr(contextAttrName) || 'site-url';
                mappers[parsedUrl.scheme][parsedUrl.uri](detectedContext);
                return PREVENT_DEFAULT_ACTION;
            }
            return ALLOW_DEFAULT_ACTION;
        }
    };

    $.fn.extend({
        mapInternalUrls: function () {
            var hasBeenInitAttr = 'data-mapInternalUrls-has-been-initialized';
            this.find('a').each(function () {
                if ($(this).attr(hasBeenInitAttr) === 'true') {
                    return;
                }
                $(this).click(function () {
                    return yazino.util.internalUrlMapper.map($(this));
                }).attr(hasBeenInitAttr, 'true');
            });
            return this;
        }
    });
}(jQuery, YAZINO));

(function ($) {
    $.fn.extend({
        callOutBox: function (message, hoursHiddenFor) {
            var storage = YAZINO.getLocalStorageInstance('callOutBox');
            hoursHiddenFor = hoursHiddenFor || 24;
            function getSanitisedMessage() {
                return $('<div/>').html(message).text().split(' ').join('_');
            }
            return this.each(function () {
                if (!message) {
                    return;
                }
                var messageUsed = storage.getItem(getSanitisedMessage()),
                    closeButtonSrc = YAZINO.configuration.get('contentUrl') + '/images/lightbox-close.png',
                    closeButtonHoverSrc = YAZINO.configuration.get('contentUrl') + '/images/lightbox-close-rollover.png',
                    callOutBox;
                YAZINO.logger.log('got message [%s] from key [%s]', messageUsed, getSanitisedMessage());
                if (messageUsed) {
                    if (messageUsed > (new Date().getTime()) - (hoursHiddenFor * 60 * 60 * 1000)) {
                        return;
                    } else {
                        storage.removeItem(getSanitisedMessage());
                    }
                }
                callOutBox = $('<article/>')
                    .addClass("callOutBox")
                    .append($('<div/>')
                        .addClass('message')
                        .html(message)
                        .mapInternalUrls()
                        .newTabForExternalLinks())
                    .insertAfter($(this));
                if (storage.isAvailable()) {
                    callOutBox.append($('<img/>')
                        .addClass('close')
                        .attr({
                            alt: 'Close',
                            src: closeButtonSrc
                        })
                        .click(function () {
                            callOutBox.remove();
                            storage.setItem(getSanitisedMessage(), new Date().getTime());
                        })
                        .hover(function () {
                            $(this).attr('src', closeButtonHoverSrc);
                        }, function () {
                            $(this).attr('src', closeButtonSrc);
                        }));
                }
            });
        }
    });
}(jQuery));
