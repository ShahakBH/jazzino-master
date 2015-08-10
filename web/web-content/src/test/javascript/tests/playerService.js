/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("PlayerService", function () {

    var playerService,
        listener;

    beforeEach(function () {
        YAZINO.rpcService = new YAZINO.RpcService();
        playerService = new YAZINO.PlayerService();
        listener = jasmine.createSpy("listener");
    });

    it("Should dispatch BalanceChanged", function () {
        var balanceDocument = {
                eventType: "PLAYER_BALANCE",
                document: {
                    balance: 2500
                }
            };
        playerService.addEventListener("BalanceChanged", listener);
        YAZINO.rpcService.dispatchEvent(balanceDocument);
        expect(listener).toHaveBeenCalledWith({
            eventType: "BalanceChanged",
            balance: "2,500",
            balanceAsNumber: 2500
        });
    });

    it("Should round off thousands after 11 chars", function () {
        var balanceDocument = {
                eventType: "PLAYER_BALANCE",
                document: {
                    balance: 12345678901234
                }
            };
        playerService.addEventListener("BalanceChanged", listener);
        YAZINO.rpcService.dispatchEvent(balanceDocument);
        expect(listener).toHaveBeenCalledWith({
            eventType: "BalanceChanged",
            balance: "12,345,678,901k",
            balanceAsNumber: 12345678901234
        });
    });

    it("Should dispatch PlayerExperienceChanged", function () {
        var xpEvent = {
                eventType: "PLAYER_XP",
                document: {
                    gameType: "BLACKJACK",
                    level: 3,
                    points: 32323,
                    toNextLevel: 3333
                }
            };
        playerService.addEventListener("ExperienceChanged", listener);
        YAZINO.rpcService.dispatchEvent(xpEvent);
        expect(listener).toHaveBeenCalledWith({
            eventType: "ExperienceChanged",
            gameType: "BLACKJACK",
            level: 3,
            points: 32323,
            toNextLevel: 3333
        });
    });

    it("Should dispatch CollectibleChanged for trophy", function () {
        var docTrophy = {
                eventType: "TROPHY_STATUS",
                document: 8
            };
        playerService.addEventListener("CollectibleChanged", listener);
        YAZINO.rpcService.dispatchEvent(docTrophy);
        expect(listener).toHaveBeenCalledWith({
            eventType: "CollectibleChanged",
            collectibleType: "trophy",
            amount: 8
        });
    });

    it("Should dispatch CollectibleChanged for medal", function () {
        var docMedal = {
                eventType: "MEDAL_STATUS",
                document: 18
            };
        playerService.addEventListener("CollectibleChanged", listener);
        YAZINO.rpcService.dispatchEvent(docMedal);
        expect(listener).toHaveBeenCalledWith({
            eventType: "CollectibleChanged",
            collectibleType: "medal",
            amount: 18
        });
    });

    it("Should dispatch CollectibleChanged for achievements", function () {
        var docAchievements = {
                eventType: "ACHIEVEMENT_STATUS",
                document: {
                    "achievements": 38,
                    "totalAchievements": 192
                }
            };
        playerService.addEventListener("CollectibleChanged", listener);
        YAZINO.rpcService.dispatchEvent(docAchievements);
        expect(listener).toHaveBeenCalledWith({
            eventType: "CollectibleChanged",
            collectibleType: "achievement",
            amount: 38,
            total: 192
        });
    });

});
