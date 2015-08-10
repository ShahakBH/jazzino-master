/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("PlayerRelationshipsService", function () {

    var playerId,
        underTest,
        strictEqualCount,
        strictEqual = function (a, b) {
            strictEqualCount += 1;
            expect(a).toBe(b);
        };

    beforeEach(function () {
        YAZINO.rpcService = new YAZINO.RpcService();
        spyOn(YAZINO.rpcService, "send");
        spyOn(YAZINO.rpcService, "addEventListener");
        underTest = new YAZINO.PlayerRelationshipsService();
        playerId = 33;
        strictEqualCount = 0;
    });

    it("Should accept friend request", function () {
        underTest.acceptFriendRequest(playerId);
        expect(YAZINO.rpcService.send).toHaveBeenCalledWith("community", ["request", playerId, "ACCEPT_FRIEND"]);
    });

    it("Should reject friend request", function () {
        underTest.rejectFriendRequest(playerId);
        expect(YAZINO.rpcService.send).toHaveBeenCalledWith("community", ["request", playerId, "REJECT_FRIEND"]);
    });

    it("Should dispatch FriendsRelationshipsChanged event when PLAYER_RELATIONSHIP is received", function () {
        var levelingService = {},
            underTest,
            getPlayerRelationshipDocumentFragment,
            getPlayerStatusDocumentFragment,
            onlineFriendsDetails,
            onlineFriendsDetailsSlots,
            onlineFriendsDetailsBlackjack;
        YAZINO.rpcService = new YAZINO.RpcService();
        underTest = new YAZINO.PlayerRelationshipsService(levelingService);
        levelingService.fetchLevels = function (data, callback) {
            callback(data);
        };
        underTest.addEventListener("FriendsRelationshipsChanged", function (event) {
            strictEqual(event.eventType, event.eventType);
        });
        underTest.addEventListener("FriendsRelationshipsChanged_SLOTS", function (event) {
            strictEqual(event.eventType, event.eventType);
        });
        underTest.addEventListener("FriendsRelationshipsChanged_BLACKJACK", function (event) {
            strictEqual(event.eventType, event.eventType);
        });
        getPlayerRelationshipDocumentFragment = function (nickname, hasSlotsLocation, hasBlackjackLocation) {
            var result = {
                "status": {
                    "nickname": nickname,
                    "pictureUrl": "http://profile.ak.fbcdn.net/v227/1825/43/q603290906_8031.jpg",
                    "levels": {
                        "SLOTS": 6,
                        "BLACKJACK": 5,
                        "ROULETTE": 3,
                        "TEXAS_HOLDEM": 6
                    },
                    "online": true,
                    "locations": [],
                    "balanceSnapshot": 15775901.0600
                },
                "nickname": nickname,
                "allowedActions": [
                    "IGNORE",
                    "REMOVE_FRIEND",
                    "PRIVATE_CHAT"
                ],
                "relationshipType": "FRIEND"
            };
            if (hasSlotsLocation) {
                result.status.locations.push({
                    "locationName": "Slots Low 74266",
                    "gameType": "SLOTS",
                    "ownerId": null,
                    "locationId": "74266",
                    "privateLocation": false
                });
            }
            if (hasBlackjackLocation) {
                result.status.locations.push({
                    "locationName": "Las Vegas Low 74265",
                    "gameType": "BLACKJACK",
                    "ownerId": null,
                    "locationId": "74265",
                    "privateLocation": false
                });
            }
            return result;
        };
        getPlayerStatusDocumentFragment = function (nickname, hasSlotsLocation, hasBlackjackLocation) {
            var result = {
                "nickname": nickname,
                "pictureUrl": "http://profile.ak.fbcdn.net/v227/1825/43/q603290906_8031.jpg",
                "levels": {
                    "SLOTS": 6,
                    "BLACKJACK": 5,
                    "ROULETTE": 3,
                    "TEXAS_HOLDEM": 6
                },
                "online": true,
                "locations": [],
                "balanceSnapshot": 15775901.0600
            };
            if (hasSlotsLocation) {
                result.locations.push({
                    "locationName": "Slots Low 74266",
                    "gameType": "SLOTS",
                    "ownerId": null,
                    "locationId": "74266",
                    "privateLocation": false
                });
            }
            if (hasBlackjackLocation) {
                result.locations.push({
                    "locationName": "Las Vegas Low 74265",
                    "gameType": "BLACKJACK",
                    "ownerId": null,
                    "locationId": "74265",
                    "privateLocation": false
                });
            }
            return result;
        };
        YAZINO.rpcService.dispatchEvent({
            eventType: "PLAYER_RELATIONSHIP",
            document: {
                "691": getPlayerRelationshipDocumentFragment("Invisible Man", false, false),
                "692": getPlayerRelationshipDocumentFragment("Sameer Malik", true, false),
                "693": getPlayerRelationshipDocumentFragment("Attila Miklosi", false, true),
                "694": getPlayerRelationshipDocumentFragment("Damjan Vujnovic", true, true)
            }
        });
        onlineFriendsDetails = underTest.getOnlineFriendsDetails();
        strictEqual(onlineFriendsDetails.length, 3);
        strictEqual(onlineFriendsDetails[0].nickname, "Attila Miklosi");
        strictEqual(onlineFriendsDetails[1].nickname, "Damjan Vujnovic");
        strictEqual(onlineFriendsDetails[2].nickname, "Sameer Malik");
        onlineFriendsDetailsSlots = underTest.getOnlineFriendsDetailsByGameType("SLOTS");
        strictEqual(onlineFriendsDetailsSlots.length, 2);
        strictEqual(onlineFriendsDetailsSlots[0].nickname, "Damjan Vujnovic");
        strictEqual(onlineFriendsDetailsSlots[1].nickname, "Sameer Malik");
        onlineFriendsDetailsBlackjack = underTest.getOnlineFriendsDetailsByGameType("BLACKJACK");
        strictEqual(onlineFriendsDetailsBlackjack.length, 2);
        strictEqual(onlineFriendsDetailsBlackjack[0].nickname, "Attila Miklosi");
        strictEqual(onlineFriendsDetailsBlackjack[1].nickname, "Damjan Vujnovic");
        YAZINO.rpcService.dispatchEvent({
            eventType: "PLAYER_STATUS",
            document: {
                "692": getPlayerStatusDocumentFragment("Sameer Malik", true, true)
            }
        });
        strictEqual(underTest.getOnlineFriendsDetails().length, 3);
        strictEqual(underTest.getOnlineFriendsDetailsByGameType("SLOTS").length, 2);
        onlineFriendsDetailsBlackjack = underTest.getOnlineFriendsDetailsByGameType("BLACKJACK");
        strictEqual(onlineFriendsDetailsBlackjack.length, 3);
        strictEqual(onlineFriendsDetailsBlackjack[0].nickname, "Attila Miklosi");
        strictEqual(onlineFriendsDetailsBlackjack[1].nickname, "Damjan Vujnovic");
        strictEqual(onlineFriendsDetailsBlackjack[2].nickname, "Sameer Malik");
        expect(strictEqualCount).toBe(22);
    });

    it("should handle globalPlayers", function () {
        YAZINO.rpcService = new YAZINO.EventDispatcher();
        YAZINO.playerService = new YAZINO.EventDispatcher();
        var levelingService = {},
            underTest = new YAZINO.PlayerRelationshipsService(levelingService),
            document;
        document = {
            "21567": {
                "pictureUrl": "https://fbcdn-profile-a.akamaihd.net/hprofile-ak-snc4/275570_728801082_3266504_q.jpg",
                "nickname": "Andy Hayes",
                "levels": { "ROULETTE" : 2 },
                "balanceSnapshot": 266220.79,
                "online": true,
                "locations": [
                    {
                        "gameType": "ROULETTE",
                        "locationId": 6890113,
                        "locationName": "Roulette Low 6890113"
                    }
                ]
            },
            "82365": {
                "pictureUrl": "http://profile.ak.fbcdn.net/hprofile-ak-snc4/275099_100000834430121_2662385_q.jpg",
                "nickname": "???",
                "levels": { "ROULETTE" : 5 },
                "balanceSnapshot": 2504,
                "online": true,
                "locations": [
                    {
                        "gameType": "ROULETTE",
                        "locationId": 6890007,
                        "locationName": "ROULETTE Low 6890007"
                    }
                ]
            },
            "123091": {
                "pictureUrl": "http://profile.ak.fbcdn.net/hprofile-ak-snc4/275185_100001546770797_59261_q.jpg",
                "nickname": "Hassan Saad",
                "levels": { "BLACKJACK" : 7 },
                "balanceSnapshot": 889187980.58,
                "online": true,
                "locations": [
                    {
                        "gameType": "BLACKJACK",
                        "locationId": 6891099,
                        "locationName": "Blackjack Medium 6891099"
                    }
                ]
            }
        };
        jQuery.ajax = function (request) {
            request.success(document);
        };
        underTest.addEventListener("PlayersOnlineChanged", function (event) {
            strictEqual(event.data.length, 3);
            strictEqual(event.data[0].playerId, "21567");
            strictEqual(event.data[0].levels.ROULETTE, 2);
            strictEqual(event.data[1].playerId, "82365");
            strictEqual(event.data[1].levels.ROULETTE, 5);
            strictEqual(event.data[2].playerId, "123091");
            strictEqual(event.data[2].levels.BLACKJACK, 7);
        });
        expect(strictEqualCount).toBe(7);
    });
});
