/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("ModalDialogueService", function () {
    var underTest, dailyPopupShownListener, dailyPopupHiddenListener, inviteFriendsPopupShownListener;
    beforeEach(function () {
        underTest = YAZINO.createModalDialogueService();
        underTest.addEventListener("DailyPopupShown", dailyPopupShownListener = jasmine.createSpy());
        underTest.addEventListener("DailyPopupHidden", dailyPopupHiddenListener = jasmine.createSpy());
        underTest.addEventListener("InviteFriendsPopupShown", inviteFriendsPopupShownListener = jasmine.createSpy());
    });

    it("should dispatch PopupShown event when popup is requested for the first time", function () {
        underTest.requestDialogue("DailyPopup");

        expect(dailyPopupShownListener).toHaveBeenCalledWith({ eventType: "DailyPopupShown" });
    });

    it("should dispatch PopupHidden event when dialogue dismissed", function () {
        underTest.requestDialogue("DailyPopup");

        underTest.dismissDialogue();

        expect(dailyPopupHiddenListener).toHaveBeenCalledWith({ eventType: "DailyPopupHidden" });
    });

    it("should not dispatch second PopupShown event", function () {
        underTest.requestDialogue("DailyPopup");

        underTest.requestDialogue("InviteFriendsPopup");

        expect(inviteFriendsPopupShownListener).not.toHaveBeenCalled();
    });

    it("should dispatch second PopupShown event once after the first one is dismissed", function () {
        underTest.requestDialogue("DailyPopup");
        underTest.requestDialogue("InviteFriendsPopup");

        underTest.dismissDialogue();

        expect(inviteFriendsPopupShownListener).toHaveBeenCalled();
    });
});
