/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("GameLoaderService", function () {

    beforeEach(function () {
        $('<div id="loader"/>').appendTo($('#testArea'));
    });

    afterEach(function () {
        $('#loader').remove();
    });

    it("Should delay invocation of setIsMuted until swf is loaded (flash movie clip should invoke YAZINO.onLoaderLoaded method to signal it's ready)", function () {
        var gameLoaderService = new YAZINO.GameLoaderService(),
            elem = document.getElementById("loader");

        elem.setIsMuted = jasmine.createSpy("isMuted");
        gameLoaderService.setIsMuted(true);
        expect(elem.setIsMuted).not.toHaveBeenCalled();
        YAZINO.onLoaderLoaded();
        expect(elem.setIsMuted).toHaveBeenCalled();
    });
});

describe("PlayerBalanceWidget", function () {

    var widget;

    beforeEach(function () {
        widget = $('<div><span class="balance"/></div>');
    });

    it("should update when event is received", function () {
        widget.playerBalanceWidget();
        YAZINO.playerService.dispatchEvent({
            eventType: "BalanceChanged",
            balance: 250
        });
        expect(widget.text()).toBe("250");
    });

    it("should avoid race condition where event is received before widget instansiation", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "BalanceChanged",
            balance: 250
        });
        widget.playerBalanceWidget();
        expect(widget.text()).toBe("250");
    });

    // can't test because of auto-instansiation.
//    it("should only register callback once", function () {
//        var listenersAdded = 0;
//        spyOn(YAZINO.playerService, 'addEventListener').andCallFake(function (listnerName) {
//            if (listnerName === "BalanceChanged") {
//                listenersAdded += 1;
//            }
//        });
//        widget.playerBalanceWidget();
//        $('<div/>').playerBalanceWidget();
//        expect(listenersAdded).toBe(1);
//    });
});

describe("FlashChecker", function () {
    var spyListener;

    beforeEach(function () {
        spyListener = jasmine.createSpy('eventListener');
        YAZINO.notificationService = YAZINO.createNotificationService(YAZINO.createNewsEventService(), YAZINO.createFacebookPostingService(new YAZINO.PlainStorageService()));
        YAZINO.notificationService.addEventListener("NotificationEvent", spyListener);
    });

    it("does nothing if flash is available", function () {
        spyOn(swfobject, 'getFlashPlayerVersion').andReturn({ major: 10 });
        YAZINO.checkFlash();
        expect(spyListener).not.toHaveBeenCalled();
    });

    it("dispatches NotificationEvent event when flash version not available", function () {
        spyOn(swfobject, 'getFlashPlayerVersion').andReturn(null);
        YAZINO.checkFlash();
        expect(spyListener).toHaveBeenCalled();
    });

    it("dispatches NotificationEvent event when flash major version is zero", function () {
        spyOn(swfobject, 'getFlashPlayerVersion').andReturn({ major: 0 });
        YAZINO.checkFlash();
        expect(spyListener).toHaveBeenCalled();
    });

});

describe("TableService", function () {
    var tableService,
        model,
        spyListener;

    beforeEach(function () {
        spyListener = jasmine.createSpy('spyListener');
        tableService = new YAZINO.TableService({
            gameName: "Wheel Deal",
            defaultTemplateName: "Slots",
            minimumStakes: [1, 100, 10000],
            minimumBalanceFactor: 2500,
            variationNames: "Low,Medium,High",
            variationDescriptions: ["Min 1", "Min 100", "Min 10K"],
            variants: {},
            inverseVariants: {}
        });
        model = tableService.getTableLauncherWidgetModel("SLOTS");
        model.addEventListener("StakeChanged", spyListener);
    });

    it("Stake should default to minimum just over minimum threshold but under medium", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "BalanceChanged",
            balanceAsNumber: 2501
        });
        expect(spyListener).toHaveBeenCalledWith(
            {
                eventType : 'StakeChanged',
                details : { stakeIndex : 0, stakesLength : 3, lowerLimit : 1, description : 'Min 1' }
            }
        );
    });

    it("Stake should default to minimum when balance very low", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "BalanceChanged",
            balanceAsNumber: 2
        });
        expect(spyListener).toHaveBeenCalledWith(
            {
                eventType : 'StakeChanged',
                details : { stakeIndex : 0, stakesLength : 3, lowerLimit : 1, description : 'Min 1' }
            }
        );
    });

    it("Stake should default to maximum", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "BalanceChanged",
            balanceAsNumber: 50000
        });
        expect(spyListener).toHaveBeenCalledWith(
            {
                eventType : 'StakeChanged',
                details : { stakeIndex : 0, stakesLength : 3, lowerLimit : 1, description : 'Min 1' }
            }
        );
    });

});

describe("ProfileBoxWidget", function () {

    var widget;

    beforeEach(function () {
        widget = $("<div/>")
            .append($('<span/>').addClass('medals'))
            .append($('<span/>').addClass('trophies'))
            .append($('<span/>').addClass('currentLevel'))
            .append($('<span/>').addClass('levelPoints'))
            .append($('<span/>').addClass('levelToNext'))
            .append($('<span/>').addClass('achievements'))
            .append($('<span/>').addClass('totalAchievements'));
        YAZINO.playerService = new YAZINO.PlayerService();
        widget.profileBoxWidget();
    });

    it("Number of medals should update when event is received", function () {
        YAZINO.playerService.dispatchEvent({eventType: "CollectibleChanged", collectibleType: "medal", amount: 3});
        expect(widget.find(".medals").text()).toBe("3");
    });

    it("Number of trophies should update when event is received", function () {
        YAZINO.playerService.dispatchEvent({eventType: "CollectibleChanged", collectibleType: "trophy", amount: 1});
        expect(widget.find(".trophies").text()).toBe("1");
    });

    it("Number of achievements should update when event is received", function () {
        $(".profileBoxTest").profileBoxWidget();
        YAZINO.playerService.dispatchEvent({eventType: "CollectibleChanged", collectibleType: "achievement", amount: 33, total: 100});
        expect(widget.find(".achievements").text()).toBe("33");
        expect(widget.find(".totalAchievements").text()).toBe("100");
    });

    it("Level and experience percentage should update when event is received", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "ExperienceChanged",
            gameType: YAZINO.configuration.get('gameType'),
            level: 3,
            points: 15,
            toNextLevel: 100
        });
        expect(widget.find(".currentLevel").text()).toBe("3");
        expect(widget.find(".levelPoints").text()).toBe("15");
        expect(widget.find(".levelToNext").text()).toBe("100");
    });

    it("Level information should not change if is for incorrect game type", function () {
        widget.find(".currentLevel").text("?");
        widget.find(".levelPoints").text("?");
        widget.find(".levelToNext").text("?");
        YAZINO.playerService.dispatchEvent({
            eventType: "ExperienceChanged",
            gameType: "SomeOtherGameType",
            level: 3,
            points: 15,
            toNextLevel: 100
        });
        expect(widget.find(".currentLevel").text()).toBe("?");
        expect(widget.find(".levelPoints").text()).toBe("?");
        expect(widget.find(".levelToNext").text()).toBe("?");
    });

});
