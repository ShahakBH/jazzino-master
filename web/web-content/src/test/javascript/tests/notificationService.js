/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("NotificationService", function () {
    var newsEventService,
        postingService,
        notificationService,
        achievementEvent,
        listener;

    function dispatchEventAndExpectData(event, expected) {
        newsEventService.dispatchEvent(event);
        expect(listener).toHaveBeenCalledWith(expected);
    }

    beforeEach(function () {
        newsEventService = new YAZINO.EventDispatcher();
        postingService = {};
        notificationService = YAZINO.createNotificationService(newsEventService, postingService);
        listener = jasmine.createSpy('listener');

        notificationService.addEventListener("NotificationEvent", listener);
    });

    it("dispatches NotificationEvent for achievement", function () {
        var shortMessage = "You stood on 13 and won!";
        dispatchEventAndExpectData(
            {
                eventType: "NewsReceived",
                newsType: "ACHIEVEMENT",
                title: "Stand and Deliver",
                shortMessage: shortMessage,
                message: "John Doe stood on 13 and won! Can you Stand and Deliver in Blackjack too?",
                image: "BLACKJACK_STAND_AND_WIN",
                gameType: "BLACKJACK"
            },
            {
                eventType: "NotificationEvent",
                title: "Stand and Deliver",
                displayMessage: shortMessage,
                duration: 30,
                action: {
                    name: "Click to post.",
                    handler: jasmine.any(Function)
                }
            }
        );
    });

    it("dispatches NotificationEvent for new level", function () {
        var shortMessage = "You've reached level 5 and been awarded a bonus of 5000 chips";
        dispatchEventAndExpectData(
            {
                eventType: "NewsReceived",
                newsType: "LEVEL",
                shortMessage: shortMessage,
                message: "John Doe has reached level 5 in BLACKJACK! Join in the play and see how high you can reach!",
                image: "new_level_5_BLACKJACK",
                gameType: "BLACKJACK"
            },
            {
                eventType: "NotificationEvent",
                displayMessage: shortMessage,
                title: undefined,
                duration: 30,
                action: {
                    name: "Click to post.",
                    handler: jasmine.any(Function)
                }
            }
        );
    });

    it("dispatches NotificationEvent for table invite", function () {
        var newsEvent = {
            eventType: "NewsReceived",
            newsType: "TABLE_INVITE",
            shortMessage: "You have been invited to play at a %s table",
            message: "123",
            image: "",
            gameType: "BLACKJACK"
        },
            expected = {
                eventType: "NotificationEvent",
                displayMessage: newsEvent.shortMessage,
                duration: 30,
                title: undefined,
                action: {
                    name: "Click to play.",
                    handler: jasmine.any(Function)
                }
            };

        dispatchEventAndExpectData(newsEvent, expected);
    });

    it("dispatches NotificationEvent for tournament final position", function () {
        var newsEvent = {
            eventType: "NewsReceived",
            newsType: "NEWS",
            shortMessage: "You ranked 3 at Jack's Table!",
            message: "John Doe ranked 3 in a Jack's Table competition with 40 players. Challenge them in a competition now!",
            image: "BLACKJACK-tournament-position-3",
            gameType: "BLACKJACK"
        },
            expected = {
                eventType: "NotificationEvent",
                displayMessage: newsEvent.shortMessage,
                duration: 30,
                title: undefined,
                action: {
                    name: "Click to post.",
                    handler: jasmine.any(Function)
                }
            };

        dispatchEventAndExpectData(newsEvent, expected);
    });

    it("dispatches NotificationEvent for tournament registration", function () {
        var newsEvent = {
            eventType: "NewsReceived",
            newsType: "NEWS",
            message: "John Doe has joined the \"t1\" tournament",
            image: "BLACKJACK-tournament-position-3",
            gameType: "BLACKJACK"
        },
            expected = {
                eventType: "NotificationEvent",
                displayMessage: newsEvent.message,
                duration: 30,
                title: undefined,
                action: {
                    name: "Click to post.",
                    handler: jasmine.any(Function)
                }
            };

        dispatchEventAndExpectData(newsEvent, expected);
    });

    it("dispatches NotificationEvent for trophy", function () {
        var newsEvent = {
            eventType: "NewsReceived",
            newsType: "TROPHY",
            shortMessage: "You are the weekly Round Up Champ!",
            message: "John Doe is the weekly Round Up Champ! Challenge them to a game of Texas Hold'em at Yazino.",
            image: "trophy_weeklyChamp_TEXAS_HOLDEM",
            gameType: "TEXAS_HOLDEM"
        },
            expected = {
                eventType: "NotificationEvent",
                displayMessage: newsEvent.shortMessage,
                duration: 0,
                title: undefined,
                action: {
                    name: "Click to post.",
                    handler: jasmine.any(Function)
                }
            };

        dispatchEventAndExpectData(newsEvent, expected);
    });

});
