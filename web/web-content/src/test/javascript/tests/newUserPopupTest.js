/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('New User Welcome Message Widget', function () {
    var lightboxWidget = {createFromElem: function () {}},
        config,
        localStorage;

    function setupWidget() {
        YAZINO.newUserWelcomeMessage(config, lightboxWidget, localStorage);
    }

    function showLightbox() {
        config.set('userDetails.isNewPlayer', true);
        setupWidget();
    }

    beforeEach(function () {
        config = YAZINO.configurationFactory({
            newPlayerPopup: {
                image: '/abc',
                alternativeText: 'Welcome to Yazino'
            },
            playerId: 1234
        });
        localStorage = YAZINO.getLocalStorageInstance('test-newUserLightboxWidget');
        localStorage.clear();
        spyOn(config, 'get').andCallThrough();
        spyOn(lightboxWidget, 'createFromElem');
        spyOn(localStorage, 'setItem').andCallThrough();
        spyOn(localStorage, 'getItem').andCallThrough();
    });

    describe('setup', function () {
        it('should exist', function () {
            expect(YAZINO.newUserWelcomeMessage).toBeDefined();
        });

        it('should check config', function () {
            setupWidget();
            expect(config.get).toHaveBeenCalledWith('userDetails.isNewPlayer');
        });

        it('should show lightbox for new users', function () {
            showLightbox();
            expect(lightboxWidget.createFromElem).toHaveBeenCalled();
        });

        it('should check displayed flag in local storage', function () {
            showLightbox();
            expect(localStorage.getItem).toHaveBeenCalledWith('displayedTo_1234');
        });

        it('should not bother checking displayed flag in local storage if config not setup', function () {
            setupWidget();
            expect(localStorage.getItem).not.toHaveBeenCalledWith('displayedTo_1234');
        });

        it('should not show lightbox for new users who have already seen it', function () {
            localStorage.set('displayedTo_1234', true);
            showLightbox();
            expect(lightboxWidget.createFromElem).not.toHaveBeenCalled();
        });

        it('should store displayed flag in local storage', function () {
            showLightbox();
            expect(localStorage.setItem).toHaveBeenCalledWith('displayedTo_1234', true);
        });

        it('should use user-id specific key', function () {
            config.set('playerId', 5678);
            showLightbox();
            expect(config.get).toHaveBeenCalledWith('playerId');
            expect(localStorage.getItem).toHaveBeenCalledWith('displayedTo_5678');
            expect(localStorage.setItem).toHaveBeenCalledWith('displayedTo_5678', true);
        });

        it('should not store displayed flag if config not new user', function () {
            setupWidget();
            expect(localStorage.setItem).not.toHaveBeenCalledWith('displayedTo_1234', true);
        });

        it('should not store displayed flag if already exists', function () {
            localStorage.set('displayedTo_1234', true);
            showLightbox();
            expect(localStorage.setItem).not.toHaveBeenCalledWith('displayedTo_1234', true);
        });
    });

    describe('Lightbox content', function () {
        var lightboxContent,
            lightboxClass;

        beforeEach(function () {
            lightboxContent = null;
            lightboxWidget.createFromElem.andCallFake(function (content, cssClass) {
                lightboxContent = content;
                lightboxClass = cssClass;
            });
        });

        it('should contain an image', function () {
            showLightbox();
            expect(lightboxContent.contains('img')).toBeTruthy();
        });

        it('should contain default image source', function () {
            showLightbox();
            expect(lightboxContent.find('img').attr('src')).toBe('/abc');
        });

        it('should contain image source provided', function () {
            config.set('newPlayerPopup.image', '/def');
            showLightbox();
            expect(lightboxContent.find('img.main').attr('src')).toBe('/def');
            expect(config.get).toHaveBeenCalledWith('newPlayerPopup.image');
        });

        it('should contain default alt text', function () {
            showLightbox();
            expect(lightboxContent.find('img').attr('alt')).toBe('Welcome to Yazino');
        });

        it('should contain alt text provided', function () {
            config.set('newPlayerPopup.alternativeText', 'mat');
            showLightbox();
            expect(lightboxContent.find('img').attr('alt')).toBe('mat');
            expect(config.get).toHaveBeenCalledWith('newPlayerPopup.alternativeText');
        });

        it('should contain major featureLink close button', function () {
            var closeButton;
            showLightbox();
            closeButton = lightboxContent.find('a.featureLink.major');
            expect(closeButton.length).toBe(1);
            expect(closeButton.attr('href')).toBe('yazino:closeLightbox');
            expect(closeButton.find('.button').text()).toBe('Collect your free chips');
        });

        it('should have been scanned for internal links', function () {
            showLightbox();
            expect(lightboxContent.find('.featureLink').attr('data-mapinternalurls-has-been-initialized')).toBe('true');
        });

        it('should show lightbox with appropriate css class', function () {
            showLightbox();
            expect(lightboxClass).toBe('newUser');
        });
    });

});