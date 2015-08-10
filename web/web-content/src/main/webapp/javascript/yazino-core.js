/*global window, document, BrowserDetect, ActiveXObject, XMLHttpRequest */

Function.prototype.curry = function (scope) {
    var fn = this, args = Array.prototype.slice.call(arguments, 1);
    return function () {
        fn.apply(scope || window, args.concat(Array.prototype.slice.apply(arguments)));
    };
};
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (str) {
        return this.substr(0, str.length) === str;
    };
}
if (!String.prototype.endsWith) {
    String.prototype.endsWith = function (str) {
        return this.substr(-str.length) === str;
    };
}
if (!Array.prototype.indexOf) {
    Array.prototype.indexOf = function (searchElement) {
        "use strict";
        var len = this.length, k;
        for (k = 0; k < len; k += 1) {
            if (this[k] === searchElement) {
                return k;
            }
        }
        return -1;
    };
}

var YAZINO;
YAZINO = YAZINO || {};

YAZINO.generateApiWrapper = function (originalApi, endpointList, argCleaner, fallback) {
    originalApi = originalApi || {};
    endpointList = endpointList || ['pointA', 'pointB'];
    argCleaner = argCleaner || function (fnName, args) {};

    var endpointsTested = {};

    function endpointExists(endpointName) {
        if (typeof endpointsTested[endpointName] === 'undefined') {
            endpointsTested[endpointName] = (typeof originalApi[endpointName] === 'function');
        }
        return endpointsTested[endpointName];
    }

    function generateEndpoint(endpointName) {
        return function () {
            var args = Array.prototype.slice.call(arguments);
            if (endpointExists(endpointName)) {
                argCleaner(endpointName, args);
                return originalApi[endpointName].apply(originalApi, args);
            }
            if (typeof fallback === 'function') {
                return fallback(endpointName, endpointList, args);
            }
            if (YAZINO.logger && YAZINO.logger.warn && endpointName !== 'warn') {
                YAZINO.logger.warn('endpoint [%s] doesn\'t exist in underlying API', endpointName);
            }

        };
    }

    return (function () {
        var endpointIndex,
            endpointName,
            wrappedApi = {};
        for (endpointIndex in endpointList) {
            if (endpointList.hasOwnProperty(endpointIndex)) {
                endpointName = endpointList[endpointIndex];
                wrappedApi[endpointName] = generateEndpoint(endpointName);
            }
        }
        return wrappedApi;
    }());
};

YAZINO.logger = YAZINO.generateApiWrapper(window.console,
    ['log', 'debug', 'info', 'warn', 'error', 'dir'],
    function (endpointName, arrayToClean) {
        var i;
        for (i in arrayToClean) {
            if (arrayToClean.hasOwnProperty(i) && typeof arrayToClean[i] === 'undefined') {
                arrayToClean[i] = '<<undefined>>';
            }
        }
    }, function (desiredEndpoint, availableEndpoints, args) {
        if (window.console !== undefined) {
            if (Function.prototype.bind && window.console) {
                if (typeof window.console[desiredEndpoint] === "object") {
                    Function.prototype.bind.call(window.console[desiredEndpoint], window.console).apply(window.console, args);
                } else if (typeof window.console.log === "object") {
                    window.console.log(args);
                }// else no idea....
            }// else you're in IE hell!
        }
    });


(function (yazino) {
    if (yazino.configurationFactory) {
        return;
    }
    yazino.configurationFactory = function (configIn) {
        var config = configIn || {};

        function isArray(item) {
            return item && typeof item === 'object' && item.constructor === Array;
        }

        function createBlankClone(item) {
            if (item && typeof item === 'object') {
                return isArray(item) ? [] : {};
            } else {
                return null;
            }
        }

        function deepCopy(item) {
            var i,
                clone = createBlankClone(item);
            if (typeof item !== 'object') {
                return item;
            }
            for (i in item) {
                if (item.hasOwnProperty(i)) {
                    clone[i] = typeof item[i] !== 'object' ? item[i] : deepCopy(item[i]);
                }
            }
            return clone;
        }

        function lookupKey(key, finalValueCallback, eachNodeCallback) {
            var keyParts = key.split('.'),
                targetNodeDepth = keyParts.length - 1,
                currentNodeDepth,
                partialKey,
                selectedNode = config;
            for (currentNodeDepth = 0; currentNodeDepth < targetNodeDepth; currentNodeDepth += 1) {
                partialKey = keyParts[currentNodeDepth];
                if (eachNodeCallback) {
                    eachNodeCallback(selectedNode, partialKey);
                }
                if (selectedNode[partialKey]) {
                    selectedNode = selectedNode[partialKey];
                } else {
                    YAZINO.logger.warn("unreachable configuration key '" + key + "' because '" + keyParts.splice(0, currentNodeDepth + 1).join(".") + "' did not exist");
                    break;
                }
            }
            finalValueCallback(selectedNode, keyParts[targetNodeDepth]);
        }

        config = deepCopy(config);

        config.set = function (key, value) {
            YAZINO.logger.info('storing into config [%s]', key, value);
            lookupKey(key, function (parent, finalKey) {
                parent[finalKey] = deepCopy(value);
            }, function (parent, partialKey) {
                if (!parent[partialKey]) {
                    parent[partialKey] = {};
                }
            });
        };

        config.get = function (key, defaultValue) {
            var value;
            lookupKey(key, function (parent, key) {
                value = parent[key];
            });
            if (typeof value === 'undefined') {
                YAZINO.logger.info("couldn't find key [%s], returning default [%s]", key, defaultValue);
                return defaultValue;
            }
            return deepCopy(value);
        };

        config.contains = function (key) {
            return typeof config.get(key) !== 'undefined';
        };

        return config;
    };
    yazino.originalConfig = yazino.configuration = yazino.configurationFactory();
}(YAZINO));

(function (yazino) {
    yazino.util = yazino.util || {};
    yazino.util.ajax = yazino.util.ajax || {};

    var log = yazino.logger,
        ajaxLib = yazino.util.ajax;

    // dependency-free POST because yazino-core.js must not have any dependencies
    function postJsonAsynchronously(url, data, successCallback, failureCallback) {
        var ASYNCHRONOUS = true,
            request = ajaxLib.requestFactory();
        if (request) {
            request.onreadystatechange = function () {
                if (request.readyState === 4) {
                    if (request.status < 300) {
                        successCallback();
                    } else {
                        failureCallback(request.status);
                    }
                }
            };
            request.open("POST", url, ASYNCHRONOUS);
            request.setRequestHeader("Content-type", "application/json");
            if (typeof data !== 'object') {
                data = {data: data};
            }
            request.send(JSON.stringify(data || {}));
        } else {
            log.error("Unable to POST using native Ajax.");
        }
    }
    yazino.util.ajax.postJsonAsynchronously = postJsonAsynchronously;
    yazino.util.ajax.requestFactory = function () {
        return window.ActiveXObject ? new ActiveXObject("Microsoft.XMLHTTP") : (XMLHttpRequest && new XMLHttpRequest()) || null;
    };
}(YAZINO));

(function (yazino) {
    var kmq = {},
        gaq = {},
        bi = {},
        log = yazino.logger;

    if (yazino.businessIntelligence) {
        log.error('Duplicate business intelligence almost created - core should only be included once.');
        return;
    }

    function reset() { // for use by tests
        kmq = {container: {tracker: []}, varName: 'tracker'};
        gaq = {container: {tracker: []}, varName: 'tracker'};
    }

    reset();

    function moveOldEventsToNewQue(oldQue, newQue) {
        while (oldQue.length > 0) {
            newQue[newQue.length] = oldQue.pop();
        }
        return newQue;
    }

    function yazinoTrack(eventName, eventProperties) {
        var url = "/tracking/event?name=" + eventName,
            data = eventProperties || {};
        log.debug('tracking event with Yazino [' + eventName + ']', eventProperties);
        YAZINO.util.ajax.postJsonAsynchronously(url, data, function () {
            log.debug("Successfully tracked [" + eventName + "] using internal tracking");
        }, function (status) {
            var logFunction = status === 503 ? log.warn : log.error;
            logFunction("failed to track [" + eventName + "] due to status code [" + status + "] using internal tracking");
        });
    }

    function safeYazinoTrackEvent(eventName, eventProperties) {
        try {
            yazinoTrack(eventName, eventProperties);
        } catch (e) {
            log.error("Unable to track event");
        }
    }

    function track(queContainer, queName, event) {
        queContainer[queName].push(event);
        safeYazinoTrackEvent(event);
    }

    function gaTrack(event) {
        log.info('Tracking event with Google Analytics', event);
        track(gaq.container, gaq.varName, event);
        safeYazinoTrackEvent(event[0], event[1] || {});
    }

    function trackVirtualUrl(virtualUrl) {
        gaTrack(['tracker2._trackPageview', '/virtual/' + virtualUrl ]);
    }

    function trackPlayerEvent(context, event, eventType) {
        var virtualUrl = eventType + '/events/' + context + '/' + event;     // TODO either hard-code eventType, apply default or throw error if undefined
        trackVirtualUrl(virtualUrl);
    }

    function setupNewQue(queConfig, queContainer,  queName) {
        if (!queContainer[queName]) {
            queContainer[queName] = [];
        }
        queContainer[queName] = moveOldEventsToNewQue(queConfig.container[queConfig.varName], queContainer[queName]);
        queConfig.container = queContainer;
        queConfig.varName = queName;
    }

    yazino.businessIntelligence = bi = {
        // Note: These functions that use a second, dedicated, tracker.  This is because our first free account does
        // not have sufficient remaining capacity to support WEB-1507.  This could be replaced with general support for
        // named trackers, a paid account or a layer of indirection that avoids google-specific code
        // (which is planned)
        //
        // Note: these methods are used to achieve consistent naming of virtual URLs
        trackEventForGameType: function (context, event, gameType) {
            var virtualUrl = gameType.toLowerCase().replace('_', '-') + '/events/' + context + '/' + event;
            log.warn('Misuse of tracking - should use wrapper (i.e. see YAZINO.businessIntelligence.track.*) not trackEventForGameType.');
            trackVirtualUrl(virtualUrl);
        },
        trackPlayerEvent: function (context, event, eventType) {
            log.warn('Misuse of tracking - should use wrapper (i.e. see YAZINO.businessIntelligence.track.*) not trackPlayerEvent.');
            trackPlayerEvent(context, event, eventType);
        },
        yazinoTrack: function (eventName, eventProperties) {
            log.warn('Misuse of tracking - should use wrapper (i.e. see YAZINO.businessIntelligence.track.*) not yazinoTrack.');
            yazinoTrack(eventName, eventProperties);
        },
        gaTrack: function (event) {
            log.warn('Misuse of tracking - should use wrapper (i.e. see YAZINO.businessIntelligence.track.*) not gaTrack.');
            gaTrack(event);
        },
        getGaQue: function () {
            return gaq.container[gaq.varName];
        },
        getKmQue: function () {
            return kmq.container[kmq.varName];
        },
        setup: {
            googleAnalytics: function (gaKey, gaqVarContainer, gaqVar) {
                if (!gaKey) {
                    return false;
                }
                setupNewQue(gaq, gaqVarContainer, gaqVar);
                (function () {
                    var ga = window.document.createElement('script'),
                        s;
                    ga.type = 'text/javascript';
                    ga.async = true;
                    ga.src = ('https:' === window.document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                    s = window.document.getElementsByTagName('script')[0];
                    s.parentNode.insertBefore(ga, s);
                }());
                gaTrack(['_setAccount', gaKey]);
                bi.track.pageView();
                return true;
            },
            googleAnalyticsTracker2: function (ga2Key) {
                if (!ga2Key) {
                    return false;
                }
                gaTrack(['tracker2._setAccount', ga2Key]);
                return true;
            }
        },
        reset: reset, //ONLY for testing
        track: {
            pageView: function () {
                gaTrack(['_trackPageview']);
            },
            playerId: function (playerId) {
            },
            yazinoAction: function (actionName, ctaRef, gameType) {
                var eventName = 'action_' + actionName, props = {};
                props[eventName + "_cta"] = ctaRef;
                props[eventName + "_gameType"] = gameType;
            },
            purchase: {
                startedProcess: function (ctaName, ctaContext) {
                    trackPlayerEvent("lobby", "buy-chips-clicked", "purchases");
                },
                cancelled: function () {
                },
                viewedMethod: function (methodName) {
                    trackPlayerEvent("lobby", methodName + "-selected", "purchases");
                },
                viewedOption: function () {
                    trackPlayerEvent("lobby", "payment-option-selected", "purchases");
                },
                submittedForm: function (methodName) {
                    trackPlayerEvent("lobby", methodName + "-submit-button-clicked", "purchases");
                },
                errorsDisplayed: function (methodName, errorArray) {
                    var eventProps = {"payment method": methodName};
                    if (typeof errorArray !== 'undefined') {
                        eventProps.errors = errorArray.join(',');
                    }
                    trackPlayerEvent("lobby", methodName + "-errors-displayed", "purchases");
                },
                selectedMethod: function (methodName) {
                    trackPlayerEvent("lobby", "buy-chips-continue-button-clicked", "purchases");
                },
                success: function (methodName) {
                    trackPlayerEvent("lobby", methodName + "-success-displayed", "purchases");
                },
                failure: function () {
                }
            },
            invite: {
                open: function (type) {
                },
                success: function (numOfInvites, type) {
                },
                statementOpened: function () {
                }
            },
            challenge: {
                open: function (type, source) {
                }
            },
            internalAdverts: {
                mobileApps: {
                    android: function (context, game) {
                    },
                    ios: function (context, game) {
                    }
                }
            }
        }
    };
}(YAZINO));

(function (yazino) {
    yazino.EventDispatcher = /** @constructor */function () {
        var listenersByEventType = {}, self = this, log = yazino.logger;
        this.addEventListener = function (eventType, listener) {
            log.info("EventDispatcher.addEventListener eventType = [%s]", eventType);
            if (!listenersByEventType[eventType]) {
                listenersByEventType[eventType] = [];
            }
            listenersByEventType[eventType].push(listener);
            self.dispatchEvent({
                eventType: "_ListenerAdded",
                listenerType: eventType
            });
        };
        this.removeEventListener = function (eventType, listener) {
            var listeners, i;
            log.info("EventDispatcher.removeEventListener eventType = [%s]", eventType);
            listeners = listenersByEventType[eventType];
            for (i = 0; i < listeners.length; i += 1) {
                if (listeners[i] === listener) {
                    listeners.splice(i, 1);
                }
            }
            self.dispatchEvent({
                eventType: "_ListenerRemoved",
                listenerType: eventType
            });
        };
        this.dispatchEvent = function (event) {
            var listeners, i, rethrowError;
            listeners = listenersByEventType[event.eventType] || [];
            log.info("EventDispatcher.dispatchEvent eventType = [%s]", event.eventType);
            rethrowError = function (error) {
                log.log("Logging error with message [%s]", error.message);
                log.error("Caught error from event listener", error);
            };
            for (i = 0; i < listeners.length; i += 1) {
                try {
                    listeners[i](event);
                } catch (error) {
                    rethrowError(error);
                }
            }
        };
        this.getListenersByEventType = function () {
            return listenersByEventType;
        };
    };
}(YAZINO));
