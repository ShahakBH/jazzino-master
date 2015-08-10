/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("NewsEventService", function () {

    var listener, listenerHasBeenCalled, newsEventService, specificListenerLogic,
        ok = function (bool) {
            expect(bool).toBeTruthy();
        };

    beforeEach(function () {
        YAZINO.rpcService = new YAZINO.EventDispatcher();
        newsEventService = new YAZINO.createNewsEventService();
        listenerHasBeenCalled = false;
        specificListenerLogic = function () {};
        listener = jasmine.createSpy('listener').andCallFake(function (data) {
            specificListenerLogic(data);
            listenerHasBeenCalled = true;
        });
    });

    function waitForListener() {
        waitsFor(function () {
            return listenerHasBeenCalled === true;
        }, "waiting for event listener", 100);
    }

    it("dispatches NewsReceived event", function () {
        var newsEventDocument = {
                eventType: "NEWS_EVENT",
                document: {
                    "type": "ACHIEVEMENT",
                    "shortDescription": {
                        "message": "You stood on 13 and won!",
                        "parameters": []
                    },
                    "delay": 0,
                    "title": "Blackjack hero",
                    "image": "BLACKJACK_STAND_AND_WIN",
                    "gameType": "BLACKJACK",
                    "playerId": 2200,
                    "news": {
                        "message": "John Doe stood on 13 and won! Can you Stand and Deliver in Blackjack too?",
                        "parameters": []
                    }
                }
            };
        runs(function () {
            newsEventService.addEventListener("NewsReceived", listener);
            YAZINO.rpcService.dispatchEvent(newsEventDocument);
        });
        waitForListener();
        runs(function () {
            expect(listener).toHaveBeenCalledWith({
                eventType: "NewsReceived",
                newsType: "ACHIEVEMENT",
                title: "Blackjack hero",
                shortMessage: "You stood on 13 and won!",
                message: "John Doe stood on 13 and won! Can you Stand and Deliver in Blackjack too?",
                image: "BLACKJACK_STAND_AND_WIN",
                gameType: "BLACKJACK"
            });
        });
    });

    it("dispatches NewsReceived event for news lacking short description", function () {
        var newsEventService = YAZINO.createNewsEventService(YAZINO.rpcService),
            newsEventDocument = {
                eventType: "NEWS_EVENT",
                document: {
                    "type": "ACHIEVEMENT",
                    "delay": 0,
                    "image": "BLACKJACK_STAND_AND_WIN",
                    "playerId": 2200,
                    "news": {
                        "message": "John Doe stood on 13 and won! Can you Stand and Deliver in Blackjack too?",
                        "parameters": []
                    }
                }
            };
        specificListenerLogic = function (data) {
            expect(data.shortDescription).not.toBeDefined();
        };
        runs(function () {
            newsEventService.addEventListener("NewsReceived", listener);
            YAZINO.rpcService.dispatchEvent(newsEventDocument);
        });
        waitForListener();
        runs(function () {
            expect(listener).toHaveBeenCalled();
        });
    });

    it("dispatches NewsReceived event respecting delay", function () {
        var newsEventDocument = {
                eventType: "NEWS_EVENT",
                document: {
                    "type": "ACHIEVEMENT",
                    "delay": 5,
                    "image": "BLACKJACK_STAND_AND_WIN",
                    "playerId": 2200,
                    "news": {
                        "message": "John Doe stood on 13 and won! Can you Stand and Deliver in Blackjack too?",
                        "parameters": []
                    }
                }
            };
        runs(function () {
            newsEventService.addEventListener("NewsReceived", listener);
            YAZINO.rpcService.dispatchEvent(newsEventDocument);
            expect(listener).not.toHaveBeenCalled();
        });
        waitForListener();
        runs(function () {
            expect(listener).toHaveBeenCalled();
        });
    });
});
