///*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, jQuery, window */
//describe("FacebookFriendsService", function () {
//
//    var service, expectedUserData, listener, expectFacebookToBeCalled, deferredAjax, emailService, gapi, authResult;
//
//    beforeEach(function () {
//        deferredAjax = jQuery.Deferred();
//        jQuery.ajax = function () {
//            return deferredAjax;
//        };
//
//        listener = jasmine.createSpy();
//        authResult = {
//            authorize: jasmine.createSpy(),
//            getToken: function () {
//                return "your mum again";
//            }
//        };
//
//        window.gapi = gapi = {
//            auth: authResult,
//            api: jasmine.createSpy(),
//            client: {
//                setApiKey: jasmine.createSpy()
//            }
//
//        };
//
//        YAZINO.googleApi.hasLoaded();
//
//        expectedUserData = {
//            feed: {
//                entry: [
//                    {gd$email: "Jae@Rae.com", title: "Mr JaeRae Esq."},
//                    {gd$email: "Marey@Carey.com"}
//                ]
//            }
//        };
//
//        emailService = YAZINO.createInviteViaEmailService(),
//            service = YAZINO.googleContactsService(emailService);
//        service.addEventListener("FriendsLoaded", listener);
//        service.addEventListener("VerifiedRegisteredFriends", listener);
//    });
//
//    it("should fire friends loaded event when contacts have been loaded", function () {
//        gapi.api.andCallFake(function (path, callback) {
//            callback({data: expectedUserData});
//        });
//        service.getPeople();
//        deferredAjax.resolve(expectedUserData);
//
//        expect(gapi.client.setApiKey).toHaveBeenCalledWith("AIzaSyDX0xHecw9egrvI6d1GkxSX6RWCH0U9Gv4");
//
//        expect(listener).toHaveBeenCalledWith({
//            eventType: "PeopleLoaded",
//            friends: [
//                {id: "Jae@Rae.com", displayName: "Mr JaeRae Esq.: Jae@Rae.com"},
//                {id: "Marey@Carey.com", displayName: 'Marey@Carey.com'}
//            ]
//        });
//    });
//
//    expectEmailsToHaveBeenSent = function () {
//
//    }
//
//    it("should verify registered friends", function () {
//        spyOn(jQuery, "ajax").andCallThrough();
//        service.checkAlreadyRegistered(["1", "2", "3"]);
//        expect(jQuery.ajax).toHaveBeenCalledWith({
//            url: "/invitation/registered/email/",
//            type: "GET",
//            accepts: { json: "application/json" },
//            dataType: "json",
//            data: {
//                addresses: "1,2,3"
//            }
//        });
//    });
//
//    it("should dispatch event after verifying registered friends", function () {
//        var callback = jasmine.createSpy('registered friends callback');
//        spyOn(jQuery, "ajax").andCallThrough();
//        service.checkAlreadyRegistered(["1", "2", "3"], callback);
//        deferredAjax.resolve(["1", "3"]);
//        expect(callback).toHaveBeenCalledWith(["1", "3"]);
//    });
//
//    it("should not dispatch event if verification failed", function () {
//        spyOn(YAZINO.logger, "warn");
//        spyOn(jQuery, "ajax").andCallThrough();
//        service.checkAlreadyRegistered(["1", "2", "3"]);
//        deferredAjax.reject();
//        expect(listener).not.toHaveBeenCalled();
//        expect(YAZINO.logger.warn).toHaveBeenCalled();
//    });
//
//
//});
