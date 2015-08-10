/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("InviteViaEmailService", function () {

    var invitationService = {
            invitationsSent: jasmine.createSpy()
        },
        originalRedirector = YAZINO.socialFlow.util.redirect,
        deferredAjax,
        underTest;

    beforeEach(function () {
        deferredAjax = jQuery.Deferred();
        jQuery.ajax = function () {
            return deferredAjax;
        };
        YAZINO.socialFlow.util.redirect = jasmine.createSpy('redirector');
        underTest = YAZINO.createInviteViaEmailService(invitationService);
    });

    afterEach(function () {
        YAZINO.socialFlow.util.redirect = originalRedirector;
    });

    it("should use ajax to verify emails", function () {
        spyOn(jQuery, "ajax").andCallThrough();
        underTest.sendInvites(["email1@example.org", "email2@example.org"]);
        expect(jQuery.ajax).toHaveBeenCalledWith({
            url: "/invitation/inviteViaEmail",
            type: "POST",
            accepts: { json: "application/json" },
            dataType: "json",
            data: {
                source: "MFS",
                emails: ["email1@example.org", "email2@example.org"]
            }
        });
    });

    it("should invoke invitation service when all emails are ok", function () {
        var callback = jasmine.createSpy(),
            serverResponse = {
                successful: 1,
                rejections: []
            };
        underTest.addEventListener("SendingInviteViaEmailFailed", callback);
        underTest.sendInvites(["valid@example.org"], callback);
        deferredAjax.resolve(serverResponse);
        expect(YAZINO.socialFlow.util.redirect).toHaveBeenCalledWith("/invitation/sent");
        expect(callback).not.toHaveBeenCalled();
    });

    it("should pass failure to callback", function () {
        var callback = jasmine.createSpy(),
            serverResponse = {
                successful: 0,
                rejections: [
                    {email: "valid@example.org", resultCode: "ALREADY_REGISTERED"},
                    {email: "invalid@example.org", resultCode: "INVALID_EMAIL"}
                ]
            };
        underTest.addEventListener("SendingInviteViaEmailFailed", callback);
        underTest.sendInvites(["valid@example.org", "invalid@example.org"], callback);
        deferredAjax.resolve(serverResponse);
        expect(YAZINO.socialFlow.util.redirect).not.toHaveBeenCalled();
        expect(callback).toHaveBeenCalledWith({
            successful: 0,
            invalid: ["invalid@example.org"],
            already_registered: ["valid@example.org"],
            limit_exceeded: [],
            eventType: "SendingInviteViaEmailFailed"
        });
    });

    it("should pass limit exceeded to callback", function () {
        var callback = jasmine.createSpy(),
            serverResponse = {
                successful: 0,
                rejections: [{email: "valid@example.org", resultCode: "LIMIT_EXCEEDED"}]
            };
        underTest.addEventListener("SendingInviteViaEmailFailed", callback);
        underTest.sendInvites(["valid@example.org", "invalid@example.org"], callback);
        deferredAjax.resolve(serverResponse);
        expect(YAZINO.socialFlow.util.redirect).not.toHaveBeenCalled();
        expect(callback).toHaveBeenCalledWith({
            successful: 0,
            invalid: [],
            already_registered: [],
            limit_exceeded: ["valid@example.org"],
            eventType: "SendingInviteViaEmailFailed"
        });
    });

    it("should not invoke callback if ajax fails", function () {
        spyOn(YAZINO.logger, "warn");
        var callback = jasmine.createSpy();
        underTest.sendInvites(["valid@example.org"], callback);
        deferredAjax.reject();
        expect(callback).not.toHaveBeenCalled();
        expect(YAZINO.logger.warn).toHaveBeenCalled();
    });

    it("should not invoke callback if server response is not valid", function () {
        spyOn(YAZINO.logger, "warn");
        var callback = jasmine.createSpy(),
            serverResponse = {};
        underTest.sendInvites(["valid@example.org"], callback);
        deferredAjax.resolve(serverResponse);
        expect(callback).not.toHaveBeenCalled();
        expect(YAZINO.logger.warn).toHaveBeenCalled();
    });

    it("should highlight first field when no email addresses entered", function () {
        spyOn(YAZINO.logger, "warn");
        var callback = jasmine.createSpy(),
            serverResponse = {};
        underTest.sendInvites(["valid@example.org"], callback);
        deferredAjax.resolve(serverResponse);
        expect(callback).not.toHaveBeenCalled();
        expect(YAZINO.logger.warn).toHaveBeenCalled();
    });
});
