/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('FacebookService', function () {
    var underTest,
        facebookOpenGraphService = {
            updateLoginStatus: jasmine.createSpy('FacebookOpenGraphService')
        };

    beforeEach(function () {
        underTest = YAZINO.generateFacebookService(facebookOpenGraphService);
    });

    it('should send out event ready call on init', function () {
        var eventListener = jasmine.createSpy();
        underTest.addEventListener('FacebookServiceReady', eventListener);
        underTest.init();

        expect(eventListener).toHaveBeenCalledWith(
            {
                eventType: 'FacebookServiceReady',
                ready: true
            }
        );
    });

    it('should call FB.api once', function () {
        var promises = [], expectedMe = {name: 'expectedName'}, count = 0, i;

        spyOn(FB, 'api').andCallFake(function (path, callback) {
            count += 1;
            callback(expectedMe);
        });
        function expectCountToBe1() {
            expect(count).toBe(1);
        }

        for (i = 0; i < 10; i += 1) {
            promises[i] = underTest.getPlayerFacebookData();
            promises[i].then(expectCountToBe1);
        }

        expect(count).toBe(0);

        underTest.init();

        expect(count).toBe(1);
    });

    it('should return correct value for me each time', function () {
        var promises = [], expectedMe = {name: 'expectedName'}, i;

        spyOn(FB, 'api').andCallFake(function (path, callback) {
            callback(expectedMe);
        });

        underTest.init();

        function expectValuesToMatch(value) {
            expect(value).toBe(expectedMe);
        }

        for (i = 0; i < 10; i += 1) {
            promises[i] = underTest.getPlayerFacebookData();
            promises[i].then(expectValuesToMatch);
        }
    });
});



