/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("LobbyInformationWidget", function () {

    var lobbyService,
        widget = $('<div class="lobbyInformationWidget"><p>Invitations: <span class="invitations">...</span></p> <span class="bold">Online Now:</span> <span class="onlineFriends">?</span>/<span class="totalFriends">?</span>&nbsp;<span class="font-friend-panel-2">Friends</span> <span>|</span> <span class="onlinePlayers">1618</span>&nbsp;<span class="font-friend-panel-2">Players</span> <span class="activeTables">64</span>&nbsp;<span class="font-friend-panel-2">Tables</span> </div>');

    beforeEach(function () {
        YAZINO.playerRelationshipsService = new YAZINO.PlayerRelationshipsService();
        YAZINO.playerService = new YAZINO.EventDispatcher();

        lobbyService = new YAZINO.LobbyService();
        widget.lobbyInformationWidget(lobbyService);

    });

    it("Should update online players and number of tables", function () {
        lobbyService.dispatchEvent({eventType: "LobbyInformationChanged_", details: {"onlinePlayers": 3, "activeTables": 1}});
        expect(widget.find(".onlinePlayers").text()).toBe("3");
        expect(widget.find(".activeTables").text()).toBe("1");
    });

    it("Should update online and total friends", function () {
        YAZINO.playerService.dispatchEvent({
            eventType: "FriendsSummaryChanged",
            summary: {
                friends: 5,
                online: 4
            }
        });
        expect(widget.find(".onlineFriends").text()).toBe("4");
        expect(widget.find(".totalFriends").text()).toBe("5");
    });

    it("Should update number of invites when no data", function () {
        $(".lobbyInformationWidget").lobbyInformationWidget(lobbyService);
        YAZINO.playerService.dispatchEvent({
            eventType: "TableInvitesChanged"
        });
        expect(widget.find("span.invitations:first").text()).toBe("0");
    });

    it("Should update number of invites when data empty", function () {
        $(".lobbyInformationWidget").lobbyInformationWidget(lobbyService);
        YAZINO.playerService.dispatchEvent({
            eventType: "TableInvitesChanged",
            data: []
        });
        expect(widget.find("span.invitations:first").text()).toBe("0");
    });

});
