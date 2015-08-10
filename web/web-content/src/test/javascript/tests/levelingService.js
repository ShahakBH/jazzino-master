/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("LevelingService", function () {

    var underTest, data;

    beforeEach(function () {
        underTest = YAZINO.createLevelingService();
        data = {
            "1" : { "locations": [ {"gameType": "ROULETTE"} ] },
            "2": { "locations": [ {"gameType": "SLOTS"} ] }
        };
        spyOn(jQuery, 'ajax');
    });

    it("Should fetch levels for players on particular game type", function () {
        var listener = jasmine.createSpy('listener').andCallFake(function (dataWithLevels) {
            expect(dataWithLevels["1"].levels.ROULETTE).toBe(5);
            expect(dataWithLevels["2"].levels.SLOTS).toBe(7);
        });
        jQuery.ajax.andCallFake(function (request) {
            request.success([{level: request.data.gameType === "ROULETTE" ? 5 : 7}]);
        });
        underTest.fetchLevels(data, listener);
        expect(listener).toHaveBeenCalled();
    });

});
