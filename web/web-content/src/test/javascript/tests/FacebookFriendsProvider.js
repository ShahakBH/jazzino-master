/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("FacebookFriendsService", function () {

    var service, expectedUserData, listener, deferredAjax, fakeConfig,
        defaultTitle = "Invite your friends to Yazino, get 5,000 chips!";

    beforeEach(function () {
        deferredAjax = jQuery.Deferred();
        jQuery.ajax = function () {
            return deferredAjax;
        };

        listener = jasmine.createSpy();
        expectedUserData = [
            {name: 'Milton Waddams', id: 123456},
            {name: 'Jimbob McFlurry', id: 654321}
        ];

        service = YAZINO.facebookFriendsService();
        service.addEventListener("PeopleLoaded", listener);
        service.addEventListener("VerifiedRegisteredFriends", listener);

        fakeConfig = YAZINO.configurationFactory({
            socialFlow: {
                invitation: {
                    batchSize: 5,
                    gameSpecificInviteText: "game specific text",
                    facebookPermissions: "email,user_birthday",
                    defaultTitle: defaultTitle
                }
            }
        });
        fakeConfig.set('socialFlow.invitation.gameSpecificInviteText', "game specific text");
        fakeConfig.set('socialFlow.invitation.facebookPermissions', "email,user_birthday");

        spyOn(YAZINO.configuration, "get").andCallFake(fakeConfig.get);
    });

    it('should filter out non facebook friends', function () {
        var deferred, filteredFriends;
        spyOn(FB, 'api').andCallFake(function (path, callback) {
            callback({data: expectedUserData});
        });

        deferred = YAZINO.fb.verifyFriends([expectedUserData[0].id, 'not a fb friend id', expectedUserData[1].id]);
        deferred.done(function (friends) {
            filteredFriends = friends;
        });

        waitsFor(function () {
            return deferred.state() !== 'pending';
        }, "the promise to complete", 10000);

        expect(filteredFriends).toEqual([expectedUserData[0].id, expectedUserData[1].id]);
    });

    it("should fire friends loaded event when friends have been loaded", function () {
        spyOn(FB, 'getLoginStatus').andCallFake(function (callback) {
            callback({authResponse: true});
        });
        spyOn(FB, 'api').andCallFake(function (path, callback) {
            callback({data: expectedUserData});
        });
        service.getPeople();
        expect(listener).toHaveBeenCalledWith({
            eventType: "PeopleLoaded",
            friends: [
                {id: 123456, displayName: "Milton Waddams"},
                {id: 654321, displayName: 'Jimbob McFlurry'}
            ]
        });
    });

    function expectFacebookToBeCalled() {
        expect(FB.ui).toHaveBeenCalled();
        expect(FB.ui.mostRecentCall.args[0]).toEqual({
            method: 'apprequests',
            filters: "['app_non_users']",
            to: '123456,654321',
            message: "game specific text",
            title: defaultTitle,
            display: 'iframe'
        });
    }

    it("should call login when not logged in and trying to send requests", function () {
        var friendIds = [123456, 654321],
            deferredVerify = jQuery.Deferred();

        spyOn(FB, 'getLoginStatus').andCallFake(function (callback) {
            callback({authResponse: "", status: "not_authorized"});
        });
        spyOn(FB, 'login').andCallFake(function (callback) {
            callback({authResponse: "", status: "connected"}, {scope: "email,user_birthday"});
        });
        spyOn(YAZINO.fb, 'verifyFriends').andReturn(deferredVerify.promise());

        FB.ui = jasmine.createSpy("FBui");
        fakeConfig.set('playerId', 555);

        service.invitePeople(friendIds);
        deferredVerify.resolve(['123456', '654321']);

        expect(FB.login).toHaveBeenCalled();
        expect(YAZINO.fb.verifyFriends).toHaveBeenCalled();
        expectFacebookToBeCalled();

    });

    it("should send request to facebook", function () {
        var friendIds = [123456, 654321],
            deferredVerify = jQuery.Deferred();

        spyOn(FB, 'getLoginStatus').andCallFake(function (callback) {
            callback({authResponse: "", status: "connected"});
        });
        spyOn(YAZINO.fb, 'verifyFriends').andReturn(deferredVerify.promise());

        FB.ui = jasmine.createSpy("FBui");
        fakeConfig.set('playerId', 555);

        service.invitePeople(friendIds);
        deferredVerify.resolve(['123456', '654321']);

        expectFacebookToBeCalled();

    });

    it("should verify registered friends", function () {
        spyOn(jQuery, "ajax").andCallThrough();
        service.checkAlreadyRegistered(["1", "2", "3"]);
        expect(jQuery.ajax).toHaveBeenCalledWith({
            url: "/invitation/registered/facebook/",
            type: "GET",
            accepts: { json: "application/json" },
            dataType: "json",
            data: {
                ids: "1,2,3"
            }
        });
    });

    it("should dispatch event after verifying registered friends", function () {
        var callback = jasmine.createSpy('registered friends callback');
        spyOn(jQuery, "ajax").andCallThrough();
        service.checkAlreadyRegistered(["1", "2", "3"], callback);
        deferredAjax.resolve(["1", "3"]);
        expect(callback).toHaveBeenCalledWith(["1", "3"]);
    });

    it("should not dispatch event if verification failed", function () {
        spyOn(YAZINO.logger, "warn");
        spyOn(jQuery, "ajax").andCallThrough();
        service.checkAlreadyRegistered(["1", "2", "3"]);
        deferredAjax.reject();
        expect(listener).not.toHaveBeenCalled();
        expect(YAZINO.logger.warn).toHaveBeenCalled();
    });


});
