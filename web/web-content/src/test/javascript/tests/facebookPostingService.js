/*global YAZINO, it, spyOn, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

var FB = FB || {
    ui: function () {}
};

describe("FacebookPostingService", function () {

    var facebookPlayerData, newsEvent, postingService, expectedRequest, fakeConfig;

    beforeEach(function () {
        document.loader = {};
        fakeConfig = YAZINO.configurationFactory({});
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);

        fakeConfig.set('permanentContentUrl', "publicurl");
        fakeConfig.set('externalLobbyUrl', "apps.facebook");
        fakeConfig.set('facebookLoginUrl', "apps.facebook");

        facebookPlayerData = {name: 'Mr Biggles'};
        newsEvent = {
            message: 'a message',
            image: 'picture',
            title: 'Yazino - Let\'s play!',
            gameType: 'blackjack',
            postedAchievementTitleText: 'Yazino - Let\'s play!',
            postedAchievementTitleLink: 'blackjack',
            postedAchievementActionText: 'Play black jack at Yazino',
            postedAchievementActionLink: 'blackjack'
        };
        postingService = YAZINO.createFacebookPostingService();
        if (!jasmine.isSpy(FB.ui)) {
            spyOn(FB, 'ui');
        }
        document.loader.fbPostDialogClosed = jasmine.createSpy('fbPostDialogClosed');
        expectedRequest = {
            method: 'feed',
            name: "Yazino - Let's play!",
            description: "a message",
            link: YAZINO.configuration.get('facebookLoginUrl'),
            display: "popup",
            actions: {
                "name": "Play black jack at Yazino",
                "link": YAZINO.configuration.get('facebookLoginUrl')
            },
            picture: "publicurl/images/news/picture.png"
        };
    });

    it("Should publish using FB.ui", function () {
        postingService.post(newsEvent, false, facebookPlayerData);
        expect(FB.ui).toHaveBeenCalled();
    });

    it("Should create full request", function () {
        postingService.post(newsEvent, false, facebookPlayerData);
        expect(FB.ui).toHaveBeenCalledWith(expectedRequest, jasmine.any(Function));
    });

    it("Should create full request with correct name", function () {
        newsEvent.message = 'a message for ${name}, please play with ${name}';
        newsEvent.postedAchievementTitleText = 'Yazino - Let\'s play with ${name}!';
        expectedRequest.name = "Yazino - Let's play with Mr Biggles!";
        expectedRequest.description = "a message for Mr Biggles, please play with Mr Biggles";

        postingService.post(newsEvent, false, facebookPlayerData);

        expect(FB.ui).toHaveBeenCalledWith(expectedRequest, jasmine.any(Function));
    });

    it("Should support url with trailing slash", function () {
        newsEvent.postedAchievementActionLink = YAZINO.configuration.get('facebookLoginUrl') + '/blackjack/';

        expectedRequest.link = YAZINO.configuration.get('facebookLoginUrl');

        postingService.post(newsEvent, false, facebookPlayerData);

        expect(FB.ui).toHaveBeenCalledWith(expectedRequest, jasmine.any(Function));
    });

    it("Should create request without picture", function () {
        delete newsEvent.image;
        delete expectedRequest.picture;

        postingService.post(newsEvent, false, facebookPlayerData);

        expect(FB.ui).toHaveBeenCalledWith(expectedRequest, jasmine.any(Function));
    });

    it("Should create request without url", function () {
        delete newsEvent.postedAchievementActionLink;

        postingService.post(newsEvent, false, facebookPlayerData);

        expect(FB.ui).toHaveBeenCalledWith(expectedRequest, jasmine.any(Function));
    });

    it("should invoke loader callback", function () {
        var newsEvent = {
            message: 'a message',
            title: 'Yazino - Let\'s play!',
            gameType: 'blackjack',
            postedAchievementTitleText: 'Yazino - Let\'s play!',
            postedAchievementTitleLink: 'blackjack',
            postedAchievementActionText: 'Play black jack at Yazino'
        };
        FB.ui.andCallFake(function (data, callback) {
            callback();
        });
        postingService.post(newsEvent, false, facebookPlayerData);
        expect(document.loader.fbPostDialogClosed).toHaveBeenCalled();
    });
});