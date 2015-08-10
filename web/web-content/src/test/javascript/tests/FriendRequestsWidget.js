/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('FriendRequestWidget', function () {

    var friendRequestsWidget, dispatchFriendRequestChangeEvent, backupRelationshipService, assume = expect;

    function generateRequest(nickname, picture) {
        return {
            "nickname": nickname,
            "status": {
                "pictureUrl": picture
            }
        };
    }

    function setFriends(friendsList) {
        dispatchFriendRequestChangeEvent({
            friendRequests: friendsList
        });
    }

    beforeEach(function () {
        backupRelationshipService = YAZINO.playerRelationshipsService;
        YAZINO.playerRelationshipsService = {
            addEventListener: function (eventType, callback) { // stub
                if (eventType === "FriendRequestsChanged") {
                    dispatchFriendRequestChangeEvent = callback;
                } else {
                    throw 'stub can\'t handle event of type [' + eventType + ']';
                }
            }
        };
        friendRequestsWidget = $('<div><div class="friendRequestArea"></div></div>');
        friendRequestsWidget.friendRequestsWidget();
        assume(typeof dispatchFriendRequestChangeEvent).toBe('function');
    });

    afterEach(function () {
        YAZINO.playerRelationshipService = backupRelationshipService;
    });

    it('should show correct number of invitations after multiple events', function () {
        setFriends([generateRequest('a'), generateRequest('b'), generateRequest('c')]);
        var requestCountElement = friendRequestsWidget.find('.requestCount');
        assume(requestCountElement.length).toBe(1);
        expect(requestCountElement.text()).toBe('3');

        setFriends([generateRequest('d')]);
        assume(requestCountElement.length).toBe(1);
        expect(requestCountElement.text()).toBe('1');
    });

    it('should show correct names of invitations after multiple events', function () {
        setFriends([generateRequest('person1'), generateRequest('person2'), generateRequest('person3')]);
        console.log('friendRequestsWidget', friendRequestsWidget);
        var requestListElement = friendRequestsWidget.find('ul');
        assume(requestListElement.length).toBe(1);
        expect(requestListElement.children().length).toBe(3);
        expect(requestListElement.children().eq(0).text()).toContain('person1');
        expect(requestListElement.children().eq(1).text()).toContain('person2');
        expect(requestListElement.children().eq(2).text()).toContain('person3');
    });

    it('should have all correct info for each request', function () {
        setFriends([generateRequest('a', 'urla'), generateRequest('b', 'urlb')]);
        var requestListElement = friendRequestsWidget.find('ul');
        assume(requestListElement.length).toBe(1);
        expect(requestListElement.children().length).toBe(2);
        console.dir(friendRequestsWidget);

        expect(requestListElement.children().eq(0).text()).toContain('a');
        expect(requestListElement.children().eq(0).find('.picture').attr("src")).toContain('urla');
        expect(friendRequestsWidget.find('.requestCount').text()).toContain('2');
        expect(requestListElement.children().eq(0).find('.picture').attr("src")).toContain('urla');
    });

});
