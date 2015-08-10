/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("InviteFriendsService", function () {
    var storageArea = 'testInvitations',
        storage = YAZINO.getLocalStorageInstance(storageArea),
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService()),
        localStorageIsAvailable,
        fakeConfig;
    storage.isAvailable = function () {
        return localStorageIsAvailable;
    };
    beforeEach(function () {
        fakeConfig = YAZINO.configurationFactory({
            gameType: 'BLACKJACK'
        });
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);
        localStorageIsAvailable = true;
    });
    it("triggerPopup dispatches PopupVisibilityChanged event", function () {
        var onInviteFriendsPopupShown = jasmine.createSpy('onInviteFriendsPopupShown');
        underTest.addEventListener('PopupVisibilityChanged', onInviteFriendsPopupShown);

        underTest.triggerPopup();

        expect(onInviteFriendsPopupShown).toHaveBeenCalledWith({
            eventType: "PopupVisibilityChanged",
            isVisible: true,
            source: "BLACKJACK"
        });
    });
    it("hidePopup dispatches PopupVisibilityChanged event", function () {
        var onInviteFriendsPopupShown = jasmine.createSpy('onInviteFriendsPopupShown');
        underTest.addEventListener('PopupVisibilityChanged', onInviteFriendsPopupShown);

        underTest.hidePopup();

        expect(onInviteFriendsPopupShown).toHaveBeenCalledWith({
            eventType: "PopupVisibilityChanged",
            isVisible: false
        });
    });
    it("does not invoke triggerPopup when triggerPopupIfNotTriggeredRecently is invoked if player is not logged in", function () {
        var underTest, agesAgo = 0;
        spyOn(storage, "get").andReturn(new jQuery.Deferred().resolve(agesAgo));
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService());
        spyOn(underTest, "triggerPopup");
        fakeConfig.set('playerId', undefined);

        underTest.triggerPopupIfNotTriggeredRecently();

        expect(underTest.triggerPopup).not.toHaveBeenCalled();
    });
    it("invokes triggerPopup when triggerPopupIfNotTriggeredRecently is invoked if popup shown more than 3 days ago", function () {
        var underTest, fourDays = 4 * 24 * 60 * 60 * 1000;
        spyOn(storage, "get").andReturn(new Date().getTime() - fourDays);
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService());
        spyOn(YAZINO.action, "run");
        fakeConfig.set('playerId', "123");

        underTest.triggerPopupIfNotTriggeredRecently();

        expect(YAZINO.action.run).toHaveBeenCalledWith('inviteFriends', 'auto');
    });
    it("should not invoke if localStorage not available", function () {
        var underTest, fourDays = 4 * 24 * 60 * 60 * 1000;
        localStorageIsAvailable = false;
        spyOn(storage, "get").andReturn(new jQuery.Deferred().resolve(new Date().getTime() - fourDays));
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService());
        spyOn(underTest, "triggerPopup");
        fakeConfig.set('playerId', "123");

        underTest.triggerPopupIfNotTriggeredRecently();

        expect(underTest.triggerPopup).not.toHaveBeenCalled();
    });
    it("does not invoke triggerPopup when triggerPopupIfNotTriggeredRecently is invoked if popup shown less than 3 days ago", function () {
        var underTest, twoDays = 2 * 24 * 60 * 60 * 1000;
        spyOn(storage, "get").andReturn(new jQuery.Deferred().resolve(new Date().getTime() - twoDays));
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService());
        spyOn(underTest, "triggerPopup");
        fakeConfig.set('playerId', "123");

        underTest.triggerPopupIfNotTriggeredRecently();

        expect(underTest.triggerPopup).not.toHaveBeenCalled();
    });
    it("does not invoke triggerPopup when triggerPopupIfNotTriggeredRecently is invoked if players is newly registered", function () {
        var underTest;
        spyOn(jQuery, "cookie").andReturn("true");
        underTest = new YAZINO.InviteFriendsService(storage, YAZINO.createModalDialogueService());
        spyOn(underTest, "triggerPopup");

        underTest.triggerPopupIfNotTriggeredRecently();

        expect(underTest.triggerPopup).not.toHaveBeenCalled();
    });
});
