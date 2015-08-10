/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('invite friend reminder service', function () {

    var RECIPIENT_ID = "recipientId",
        inviteFriendsService = {
            triggerPopup: jasmine.createSpy('triggerPopup')
        },
        reminderSentEventListener,
        mostRecentAjax,
        deferredAjax,
        underTest = YAZINO.createInviteFriendsReminderService(inviteFriendsService);

    beforeEach(function () {
        reminderSentEventListener = jasmine.createSpy('reminderSentEventListener');
        jasmine.Ajax.useMock();
        deferredAjax = jQuery.Deferred();
        jQuery.ajax = function () {
            return deferredAjax;
        };
        FB.ui = jasmine.createSpy("FB.ui");
    });

    it("should send Facebook app request when invoked for Facebook", function () {
        var facebookArgs;

        underTest.sendReminder(RECIPIENT_ID, "Facebook");

        facebookArgs = FB.ui.mostRecentCall.args[0];
        expect(FB.ui).toHaveBeenCalled();
        expect(facebookArgs.method).toEqual("apprequests");
        expect(facebookArgs.display).toEqual("iframe");
        expect(facebookArgs.filters).toEqual("['app_non_users']");
        expect(facebookArgs.to).toEqual(RECIPIENT_ID);
    });       // TODO test callback function

    it("should trigger popup with call to action parameter when invoked for Yazino", function () {
        spyOn(jQuery, "ajax").andCallThrough();

        underTest.sendReminder(RECIPIENT_ID, "Email");

        expect(jQuery.ajax).toHaveBeenCalledWith({
            url: "/friends/sendInvitationReminder",
            type: "POST",
            accepts: {
                json: "application/json"
            },
            dataType: "json",
            data: {
                recipientId: RECIPIENT_ID
            }
        });
    });

    it("should dispatch reminderSent event when email POST succeeds", function () {
        underTest.addEventListener("reminderSentEvent." + RECIPIENT_ID + ".Email", reminderSentEventListener);

        underTest.sendReminder(RECIPIENT_ID, "Email");

        deferredAjax.resolve();

        expect(reminderSentEventListener).toHaveBeenCalledWith({
            eventType: 'reminderSentEvent.' + RECIPIENT_ID + ".Email"
        });
    });

    it("should not dispatch reminderSent event when email POST fails", function () {
        underTest.addEventListener("reminderSentEvent." + RECIPIENT_ID + ".Email", reminderSentEventListener);

        underTest.sendReminder(RECIPIENT_ID, "Email");

        deferredAjax.reject();

        expect(reminderSentEventListener).not.toHaveBeenCalled();
    });

    it("dispatch ReminderSent event when reminderSent invoked", function () {
        underTest.addEventListener("reminderSentEvent." + RECIPIENT_ID + ".Facebook", reminderSentEventListener);

        underTest.reminderSent(RECIPIENT_ID, "Facebook");

        expect(reminderSentEventListener).toHaveBeenCalledWith({
            eventType: 'reminderSentEvent.' + RECIPIENT_ID + ".Facebook"
        });
    });

    it("should expose reminderSent via YAZINO.inviteFriendsReminderSent", function () {
        expect(typeof YAZINO.inviteFriendsReminderSent).toBe("function");
    });

});

