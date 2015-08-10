/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('invite friend reminder widget', function () {

    var RECIPIENT_ID = "recipientId",
        inviteFriendsReminderService;

    beforeEach(function () {
        inviteFriendsReminderService = {
            sendReminder: jasmine.createSpy('inviteFriendsReminderService')
        };
        YAZINO.EventDispatcher.apply(inviteFriendsReminderService);
    });

    it("should throw an error if container does not specify recipient id", function () {
        var widget = $("#inviteFriendReminderWidget4");

        widget.removeAttr('data-invite-recipient-id');

        expect(function () {
            widget.inviteFriendReminderWidget(inviteFriendsReminderService);
        }).toThrow({message: "Missing recipient id."});
    });

    it("should throw an error if container does not specify source", function () {
        var widget = $("#inviteFriendReminderWidget5");

        widget.removeAttr('data-invite-source');

        expect(function () {
            widget.inviteFriendReminderWidget(inviteFriendsReminderService);
        }).toThrow({message: "Missing source."});
    });

    it("should attach launcher to all matching links", function () {
        $('reminderLinkClass').inviteFriendReminderWidget(inviteFriendsReminderService);

    });

    it("should invoke service when clicked with correct recipient identifier/source", function () {
        var widget = $("#inviteFriendReminderWidget1");

        widget.inviteFriendReminderWidget(inviteFriendsReminderService);

        widget.find("a.sendReminder").click();

        expect(inviteFriendsReminderService.sendReminder).toHaveBeenCalledWith("recipient1", "source1");
    });

    it("should not invoke service when clicked in a non-pending state", function () {
        var widget = $("#inviteFriendReminderWidget3");

        widget.inviteFriendReminderWidget(inviteFriendsReminderService);

        widget.removeClass('pending');

        widget.find("a.sendReminder").click();

        expect(inviteFriendsReminderService.sendReminder).not.toHaveBeenCalled();
    });

    it("should flag the widget as reminded in response to the reminder sent event", function () {
        var widget = $("#inviteFriendReminderWidget2");

        widget.inviteFriendReminderWidget(inviteFriendsReminderService);

        inviteFriendsReminderService.dispatchEvent({
            eventType: 'reminderSentEvent.recipient1.source1'
        });

        expect(widget.hasClass('pending')).toBeFalsy();
        expect(widget.hasClass('reminded')).toBeTruthy();
    });

});

