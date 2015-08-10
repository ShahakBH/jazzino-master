/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("FriendSelectorService", function () {

    var friendSelectorService, availableFriendsChangedSpy;

    beforeEach(function () {
        spyOn(jQuery, 'ajax');
        friendSelectorService = new YAZINO.FriendSelectorService();
        YAZINO.playerRelationshipsService = {
            getAllOnlineFriendDetails: function () {
                return {
                    "1": "1",
                    "2": "2"
                };
            },
            getOfflineFriendsDetails: function () {
                return {
                    "5": "5",
                    "6": "6"
                };
            }
        };
        availableFriendsChangedSpy = jasmine.createSpy("availableFriendsChangedSpy");
        friendSelectorService.addEventListener("AvailableFriendsChanged", availableFriendsChangedSpy);
    });

    it("dispatches event when friends loaded", function () {
        friendSelectorService.loadFriends();
        expect(availableFriendsChangedSpy).toHaveBeenCalled();
    });

    it("dispatched event contains correct number of friends", function () {
        availableFriendsChangedSpy.andCallThrough(function (friends) {
            expect(friends.length).toBe(4);
        });
        friendSelectorService.loadFriends();
        expect(availableFriendsChangedSpy).toHaveBeenCalled();
    });

    it("dispatched event contains friends ordered by those online first", function () {
        friendSelectorService.loadFriends();
        expect(availableFriendsChangedSpy).toHaveBeenCalledWith({
            eventType: 'AvailableFriendsChanged',
            friends: ["1", "2", "5", "6"]
        });
    });

    it("doesnt send invites when no selected friends", function () {
        friendSelectorService.sendInvites(1, "foo");
        expect(jQuery.ajax).not.toHaveBeenCalled();
    });

    it("sending invites makes call with correct tableId", function () {
        jQuery.ajax.andCallFake(function (options) {
            expect(options.data.tableId).toBe(123);
        });

        friendSelectorService.selectFriend(1);
        friendSelectorService.sendInvites(123, "foo");

        expect(jQuery.ajax).toHaveBeenCalled();
    });

    it("sending invites makes call with correct message", function () {
        jQuery.ajax.andCallFake(function (options) {
            expect(options.data.message).toBe("foo");
        });

        friendSelectorService.selectFriend(1);
        friendSelectorService.sendInvites(123, "foo");

        expect(jQuery.ajax).toHaveBeenCalled();
    });

    it("sending invites makes call with correct friendIds", function () {
        jQuery.ajax.andCallFake(function (options) {
            expect(options.data.friendIds).toBe("1,3");
        });

        friendSelectorService.selectFriend(1);
        friendSelectorService.selectFriend(2);
        friendSelectorService.selectFriend(3);
        friendSelectorService.deselectFriend(2);
        friendSelectorService.sendInvites(123, "foo");

        expect(jQuery.ajax).toHaveBeenCalled();
    });

});
