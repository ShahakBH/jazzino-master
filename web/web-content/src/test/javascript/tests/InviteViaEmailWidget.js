/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("InviteViaEmailWidget", function () {
    var invitationService, $ = jQuery;
    beforeEach(function () {
        invitationService = {
            selectProvider: jasmine.createSpy('invitationService'),
            sendInvites: jasmine.createSpy('sendInvites')
        };

        YAZINO.EventDispatcher.apply(invitationService);

        $(".inviteViaEmailWidgetTest").inviteViaEmailWidget(invitationService);
    });


    it("should mark problematic fields", function () {
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 1,
            invalid: ["email1@example.org"],
            limit_exceeded: ["limited@example.org"],
            already_registered: ["email3@example.org"]
        });
//        $(".testSubmit").click();

        expect($(".inviteViaEmailWidgetTest input[value='email1@example.org']").hasClass("invalidEmail")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest input[value='email3@example.org']").hasClass("emailAlreadyRegistered")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest input[value='limited@example.org']").hasClass("limitExceeded")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .invalidEmail").is(":visible")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .emailAlreadyRegistered").is(":visible")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .limitExceeded").is(":visible")).toBeTruthy();
    });

    it("should clear validation from all fields before applying new ones", function () {
        $(".inviteViaEmailWidgetTest input[value='email2@example.org']").addClass("invalidEmail");
        $(".inviteViaEmailWidgetTest input[value='limited@example.org']").addClass("limitExceeded");
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 2,
            invalid: [],
            limit_exceeded: [],
            already_registered: ["email2@example.org"]
        });
//        $(".testSubmit").click();
        expect($(".inviteViaEmailWidgetTest input[value='email2@example.org']").hasClass("invalidEmail")).toBeFalsy();
        expect($(".inviteViaEmailWidgetTest input[value='limited@example.org']").hasClass("limitExceeded")).toBeFalsy();
        expect($(".inviteViaEmailWidgetTest input[value='email2@example.org']").hasClass("emailAlreadyRegistered")).toBeTruthy();
    });

    it("should reset invalid email key", function () {
        $(".inviteViaEmailWidgetTest .emailValidationErrors .invalidEmail").show();
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 1,
            invalid: [],
            limit_exceeded: [],
            already_registered: ["email1@example.org"]
        });
//        $(".testSubmit").click();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .invalidEmail").is(":visible")).toBeFalsy();
    });

    it("should reset email already registered key", function () {
        $(".inviteViaEmailWidgetTest .emailValidationErrors .emailAlreadyRegistered").show();
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 1,
            invalid: [],
            limit_exceeded: [],
            already_registered: []
        });
//        $(".testSubmit").click();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .emailAlreadyRegistered").is(":visible")).toBeFalsy();
    });

    it("should display invalid email key", function () {
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 1,
            invalid: ["email1@example.org"],
            limit_exceeded: [],
            already_registered: []
        });
//        $(".testSubmit").click();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .invalidEmail").is(":visible")).toBeTruthy();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .emailAlreadyRegistered").is(":visible")).toBeFalsy();
    });

    it("should display email already registered key", function () {
        invitationService.dispatchEvent({
            eventType: "SendingInviteViaEmailFailed",
            successful: 1,
            invalid: [],
            limit_exceeded: [],
            already_registered: ["email1@example.org"]
        });
//        $(".testSubmit").click();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .invalidEmail").is(":visible")).toBeFalsy();
        expect($(".inviteViaEmailWidgetTest .emailValidationErrors .emailAlreadyRegistered").is(":visible")).toBeTruthy();
    });

    it("should show when receives invitation provider selected event with method not facebook", function () {
        invitationService.dispatchEvent({
            eventType: "InvitationProviderSelected",
            provider: "email"
        });

        expect($(".inviteViaEmailWidgetTest").is(":visible")).toBeTruthy();
    });


    it("should not send email addresses that it has just sent", function () {
        var addresses = $("form.addresses");
        addresses.submit();
        expect(invitationService.sendInvites.callCount).toBe(1);
        addresses.submit();
        expect(invitationService.sendInvites.callCount).toBe(1);
        $(".inviteViaEmailWidgetTest input[value='email1@example.org']").val('different@example.org'); //this can break subsequent tests!
        addresses.submit();
        expect(invitationService.sendInvites.callCount).toBe(2);
    });

});
