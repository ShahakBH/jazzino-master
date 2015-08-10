/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("TournamentInfoWidget", function () {
    var tournamentService, playerService, widget;
    beforeEach(function () {
        tournamentService = new YAZINO.EventDispatcher();
        playerService = {
            getBalanceAsNumber: function () { return 0; }
        };
        widget = jQuery(".tournamentInfoWidget").clone();
        widget.tournamentInfoWidget(tournamentService, playerService);
    });

    it("should update basic elements of widget", function () {
        var event = {
            eventType: "NextTournamentInfoChanged_BLACKJACK",
            details: {
                name: "myTournament",
                registrationFee: 3000,
                prizePool: 10000,
                registeredPlayers: 34,
                registeredFriends: 15,
                registered: false
            }
        };
        tournamentService.dispatchEvent(event);
        expect(widget.find(".tournamentName").text()).toEqual(event.details.name);
        expect(widget.find(".registrationFee").text()).toEqual("3,000");
        expect(widget.find(".prizePool").text()).toEqual("10,000");
        expect(widget.find(".registeredPlayers").text()).toEqual("34");
        expect(widget.find(".registeredFriends").text()).toEqual("15");
        expect(widget.find(".registered").is(":visible")).toBeFalsy();
    });

});
