/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("FacebookUserDetailsWidget", function () {
    var service;

    beforeEach(function () {
        service = {
            getFacebookUserDetails: function () {
            }
        };
        YAZINO.EventDispatcher.apply(service);
    });

    it("should use service to retrieve facebook user picture and name", function () {
        spyOn(service, 'getFacebookUserDetails');
        jQuery(".fbUserDetailsWidgetTest").facebookUserDetailsWidget(service);
        expect(service.getFacebookUserDetails).toHaveBeenCalledWith(["fbUser1", "fbUser2"]);
    });

    it("should update DOM when receive picture and name", function () {
        jQuery(".fbUserDetailsWidgetTest").facebookUserDetailsWidget(service);
        service.dispatchEvent({
            eventType: "FbUserDetailsRetrieved",
            data: {
                "fbUser1": {
                    "id": "fbUser1",
                    "name": "User 1",
                    "picture": "image_for_user1"
                },
                "fbUser2": {
                    "id": "fbUser2",
                    "name": "User 2",
                    "picture": "image_for_user2"
                }
            }
        });

        expect(jQuery("*[data-fb-user-id='fbUser1'] .fbName").text()).toEqual("User 1");
        expect(jQuery("*[data-fb-user-id='fbUser2'] .fbName").text()).toEqual("User 2");
        expect(jQuery("*[data-fb-user-id='fbUser1'] .fbPicture").attr("src")).toEqual("image_for_user1");
        expect(jQuery("*[data-fb-user-id='fbUser2'] .fbPicture").attr("src")).toEqual("image_for_user2");
    });

    it("should update DOM when receive picture and name after October 2012 breaking changes", function () {
        jQuery(".fbUserDetailsWidgetTest").facebookUserDetailsWidget(service);
        service.dispatchEvent({
            eventType: "FbUserDetailsRetrieved",
            data: {
                "fbUser1": {
                    "id": "fbUser1",
                    "name": "User 1",
                    "picture": {
                        'data': {
                            'is_silhouette': false,
                            'url': "image_for_user1"
                        }
                    }
                },
                "fbUser2": {
                    "id": "fbUser2",
                    "name": "User 2",
                    "picture": {
                        'data': {
                            'is_silhouette': false,
                            'url': "image_for_user2"
                        }
                    }
                }
            }
        });

        expect(jQuery("*[data-fb-user-id='fbUser1'] .fbName").text()).toEqual("User 1");
        expect(jQuery("*[data-fb-user-id='fbUser2'] .fbName").text()).toEqual("User 2");
        expect(jQuery("*[data-fb-user-id='fbUser1'] .fbPicture").attr("src")).toEqual("image_for_user1");
        expect(jQuery("*[data-fb-user-id='fbUser2'] .fbPicture").attr("src")).toEqual("image_for_user2");
    });
});

