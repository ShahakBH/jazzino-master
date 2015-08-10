/*global gapi, YAZINO, jasmine, expect, $, spyOn, describe, beforeEach, it*/

describe("googleapi", function () {
    var authResultHandler, googleApi;

    function initAndGetContacts() {
        googleApi.hasLoaded();
        googleApi.getContacts();
    }


    beforeEach(function () {
        var fakeConfig = YAZINO.configurationFactory(
            {googleApi: {
                apiKey: "[apiKey]",
                client_id: "[client_id]",
                scope: "[scope]"

            }}
        );
        authResultHandler = null;
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);
        spyOn(gapi.auth, "getToken").andReturn({id: "token"});
        spyOn(gapi.auth, "authorize").andCallFake(function (config, callback) {
            authResultHandler = callback;
        });
        googleApi = YAZINO.createGoogleApi(gapi);
    });

    it("should exist", function () {
        expect(googleApi).toBeDefined();
    });

    it("should set Api key on getContacts call", function () {
        spyOn(gapi.client, 'setApiKey');
        initAndGetContacts();
        expect(YAZINO.configuration.get).toHaveBeenCalledWith('googleApi.apiKey');
        expect(gapi.client.setApiKey).toHaveBeenCalledWith("[apiKey]");

    });

    it("should Authorize on first getContacts call", function () {
        initAndGetContacts();
        expect(gapi.auth.authorize).toHaveBeenCalledWith({client_id: "[client_id]", scope: "[scope]"}, jasmine.any(Function));

    });

    it("should handle Auth Result", function () {
        initAndGetContacts();
        expect(gapi.auth.getToken).not.toHaveBeenCalled();
        authResultHandler("true");
        expect(gapi.auth.getToken).toHaveBeenCalled();

    });

    it("should make ajax call with token", function () {
        spyOn($, "ajax");

        initAndGetContacts();
        expect($.ajax).not.toHaveBeenCalled();
        authResultHandler("true");
        expect($.ajax).toHaveBeenCalledWith(
            {
                url: 'https://www.google.com/m8/feeds/contacts/default/full',
                dataType: 'jsonp',
                data: {
                    id: "token",
                    alt: 'json',
                    "max-results": 99999
                },
                success: jasmine.any(Function)
            }
        );
    });

    it("should not Authenticate until after gapi.client loaded", function () {
        googleApi.getContacts();
        expect(gapi.auth.authorize).not.toHaveBeenCalled();
        googleApi.hasLoaded();
        expect(gapi.auth.authorize).toHaveBeenCalled();
    });

    it("should log a warning if auth fails", function () {

    });
});
