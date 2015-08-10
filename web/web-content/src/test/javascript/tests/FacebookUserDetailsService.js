/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

var FB = FB || {};

describe("FacebookUserDetailsService", function () {

    var service = YAZINO.FacebookUserDetailsService(),
        fakeFBResponse = function (response) {
            var fbApiCallArgs;
            expect(FB.api).toHaveBeenCalled();
            fbApiCallArgs = FB.api.mostRecentCall.args;
            expect(fbApiCallArgs.length).toEqual(3);
            fbApiCallArgs[2](response);
        };


    it("should invoke FB.api", function () {
        var fbApiCallArgs;
        spyOn(FB, "api");
        service.getFacebookUserDetails(["u1", "u2", "u3"]);
        expect(FB.api).toHaveBeenCalled();
        fbApiCallArgs = FB.api.mostRecentCall.args;
        expect(fbApiCallArgs.length).toEqual(3);
        expect(fbApiCallArgs[0]).toEqual("/");
        expect(fbApiCallArgs[1]).toEqual({ ids: ["u1", "u2", "u3"], fields: ['name', 'picture'] });
    });

    it("should not invoke FB.api if zero facebook IDs sent", function () {
        var fbApiCallArgs;
        spyOn(FB, "api");
        service.getFacebookUserDetails(undefined);
        expect(FB.api).not.toHaveBeenCalled();
    });

    it("should not invoke FB.api if no facebook IDs sent", function () {
        var fbApiCallArgs;
        spyOn(FB, "api");
        service.getFacebookUserDetails([]);
        expect(FB.api).not.toHaveBeenCalled();
    });

    it("should dispatch events based on FB.api response", function () {
        var listener = jasmine.createSpy(),
            facebookResponse = {
                "u1": { id: "u1", name: "User 1", picture: "Picture 1 "},
                "u2": { id: "u2", name: "User 2", picture: "Picture 2 "},
                "u3": { id: "u3", name: "User 3", picture: "Picture 3 "}
            };
        spyOn(FB, "api");
        service.getFacebookUserDetails(["u1", "u2", "u3"]);
        service.addEventListener("FbUserDetailsRetrieved", listener);
        fakeFBResponse(facebookResponse);
        expect(listener).toHaveBeenCalledWith({eventType: "FbUserDetailsRetrieved", data: facebookResponse});
    });

    it("should handle empty FB api response", function () {
        var listener = jasmine.createSpy();
        spyOn(FB, "api");
        service.getFacebookUserDetails(["u1", "u2", "u3"]);
        service.addEventListener("FbUserDetailsRetrieved", listener);
        fakeFBResponse(undefined);
        expect(listener).not.toHaveBeenCalled();
    });

    it("should handle FB.api errors", function () {
        var fbApiCallArgs,
            listener = jasmine.createSpy(),
            facebookResponse = {
                "error": {"message": "Unsupported get request.", "type": "GraphMethodException", "code": 100}
            };
        spyOn(FB, "api");
        service.getFacebookUserDetails(["u1", "u2", "u3"]);
        service.addEventListener("FbUserDetailsRetrieved", listener);
        fakeFBResponse(facebookResponse);
        expect(listener).not.toHaveBeenCalled();
    });
});
