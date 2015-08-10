/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('TwitterService', function () {
    var underTest,
        fakeWindowScreen = {
            height: 720,
            width: 1000
        };

    beforeEach(function () {
        underTest = YAZINO.generateTwitterService(fakeWindowScreen);
    });

    it('should create correct link to Twitter with correct URL to share', function () {
        var urlToShare = 'http://something.something/something/dark?=side',
            twitterShareUrl = underTest.shareUrl(urlToShare);
        expect(twitterShareUrl).toBe('https://twitter.com/share?url=http%3A%2F%2Fsomething.something%2Fsomething%2Fdark%3F%3Dside');
    });

    it('should create correct link to Twitter with correct message to share', function () {
        var textToShare = 'You\'re about to send a message to twitter, will it work?',
            twitterShareUrl = underTest.shareText(textToShare);
        expect(twitterShareUrl).toBe('https://twitter.com/share?url=&text=You\'re%20about%20to%20send%20a%20message%20to%20twitter%2C%20will%20it%20work%3F');
    });

    it('should create correct link to Twitter with correct message and url to share', function () {
        var textToShare = 'You\'re about to send a message to twitter, will it work?',
            urlToShare = 'http://something.something/something/dark?=side',
            twitterShareUrl = underTest.shareUrlAndText(urlToShare, textToShare);
        expect(twitterShareUrl).toBe('https://twitter.com/share?url=http%3A%2F%2Fsomething.something%2Fsomething%2Fdark%3F%3Dside&' +
            'text=You\'re%20about%20to%20send%20a%20message%20to%20twitter%2C%20will%20it%20work%3F');
    });

    it('should call shareUrl and open new window with correct params', function () {
        var urlToShare = 'http://something.something/something/dark?=side',
            twitterUrl = 'https://twitter.com/share?url=http%3A%2F%2Fsomething.something%2Fsomething%2Fdark%3F%3Dside',
            count = 0;

        spyOn(window, 'open').andCallFake(function (url, name, specs) {
            expect(url).toBe(twitterUrl);
            expect(name).toBe('twitterWindow');
            expect(specs).toBe('scrollbars=yes,resizable=yes,toolbar=no,location=yes,width=550,height=420,left=225,top=150');
            count += 1;
        });
        underTest.shareUrlAndOpenWindow(urlToShare);
        expect(count).toBe(1);
    });

    it('should call shareText and open new window with correct params', function () {
        var textToShare = 'You\'re about to send a message to twitter, will it work?',
            twitterUrl = 'https://twitter.com/share?url=&text=You\'re%20about%20to%20send%20a%20message%20to%20twitter%2C%20will%20it%20work%3F',
            count = 0;

        spyOn(window, 'open').andCallFake(function (url, name, specs) {
            expect(url).toBe(twitterUrl);
            expect(name).toBe('twitterWindow');
            expect(specs).toBe('scrollbars=yes,resizable=yes,toolbar=no,location=yes,width=550,height=420,left=225,top=150');
            count += 1;
        });
        underTest.shareTextAndOpenWindow(textToShare);
        expect(count).toBe(1);
    });

    it('should call shareUrlAndText and open new window with correct params', function () {
        var textToShare = 'You\'re about to send a message to twitter, will it work?',
            urlToShare = 'http://something.something/something/dark?=side',
            twitterUrl = 'https://twitter.com/share?url=http%3A%2F%2Fsomething.something%2Fsomething%2Fdark%3F%3Dside&' +
                'text=You\'re%20about%20to%20send%20a%20message%20to%20twitter%2C%20will%20it%20work%3F',
            count = 0;

        spyOn(window, 'open').andCallFake(function (url, name, specs) {
            expect(url).toBe(twitterUrl);
            expect(name).toBe('twitterWindow');
            expect(specs).toBe('scrollbars=yes,resizable=yes,toolbar=no,location=yes,width=550,height=420,left=225,top=150');
            count += 1;
        });
        underTest.shareUrlAndTextAndOpenWindow(urlToShare, textToShare);
        expect(count).toBe(1);
    });
});