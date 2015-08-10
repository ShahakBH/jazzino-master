/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("facebookFriendsInviteWidget", function () {
    var sampleFriends,
        friendsProvider = {
            getPeople: function () {
            },
            checkAlreadyRegistered: function () {
            }
        },
        personSelector,
        personSelectorWidget = jQuery.fn.personSelector,
        preDrawFilter;

    YAZINO.EventDispatcher.apply(friendsProvider);

    beforeEach(function () {
        var continueCallback = jasmine.createSpy("callback");

        sampleFriends = [
            { id: "1", displayName: "Person A" },
            { id: "2", displayName: "Person B" },
            { id: "3", displayName: "Person C" }
        ];
        personSelector = jQuery('<div/>').personSelector(continueCallback);
        jQuery.fn.personSelector = function (continueCallback, config) {
            preDrawFilter = (config.hooks && config.hooks.preDrawEvent) || function () {};
            return personSelector;
        };

        spyOn(personSelector, 'addPeople');
        spyOn(personSelector, 'modelChanged');
        spyOn(personSelector, 'rebuildView');

        spyOn(friendsProvider, 'getPeople');
        spyOn(friendsProvider, 'checkAlreadyRegistered');
    });

    afterEach(function () {
        jQuery.fn.personSelector = personSelectorWidget;
    });

    it("Should make external calls when elements used", function () {
        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);

        expect(friendsProvider.getPeople).toHaveBeenCalled();
    });

    it("Should not make external calls when no elements used", function () {
        jQuery("<div/>").find('a').facebookFriendsInviteWidget(friendsProvider);

        expect(friendsProvider.getPeople).not.toHaveBeenCalled();

        friendsProvider.dispatchEvent({
            eventType: "PeopleLoaded",
            friends: sampleFriends
        });

        expect(personSelector.addPeople).not.toHaveBeenCalled();
    });

    it("Should populate the personSelector in response to a PeopleLoadedEvent with the data from the event", function () {
        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);
        friendsProvider.dispatchEvent({
            eventType: "PeopleLoaded",
            friends: sampleFriends
        });

        expect(personSelector.addPeople).toHaveBeenCalledWith(sampleFriends);
    });

    it("People should be disabled by default", function () {
        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);
        friendsProvider.dispatchEvent({
            eventType: "PeopleLoaded",
            friends: sampleFriends
        });

        expect(personSelector.addPeople).toHaveBeenCalledWith(sampleFriends);
    });

    it("Should populate the personSelector with loading status", function () {
        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);

        preDrawFilter(sampleFriends);

        expect(sampleFriends[0].disabled).toBeTruthy();
        expect(sampleFriends[0].comment).toBe('Loading');
        expect(sampleFriends[1].disabled).toBeTruthy();
        expect(sampleFriends[1].comment).toBe('Loading');
        expect(sampleFriends[2].disabled).toBeTruthy();
        expect(sampleFriends[2].comment).toBe('Loading');
    });

    it('Should disable friends already registered', function () {
        var callback;

        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);

        friendsProvider.checkAlreadyRegistered = function (idsToCheck, registeredCallback) {
            callback = registeredCallback;
        };

        preDrawFilter(sampleFriends);

        callback(sampleFriends[1].id);

        expect(sampleFriends[0].disabled).toBeFalsy();
        expect(sampleFriends[0].comment).toBe('');
        expect(sampleFriends[1].disabled).toBeTruthy();
        expect(sampleFriends[1].comment).toBe('Already Registered');
        expect(sampleFriends[2].disabled).toBeFalsy();
        expect(sampleFriends[2].comment).toBe('');

        expect(personSelector.rebuildView).toHaveBeenCalledWith(sampleFriends);
    });

    it('Should mark checked friends', function () {
        var callback;

        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);

        friendsProvider.checkAlreadyRegistered = function (idsToCheck, registeredCallback) {
            callback = registeredCallback;
        };

        preDrawFilter(sampleFriends);

        callback(sampleFriends[1].id);

        expect(sampleFriends[0].hasBeenChecked).toBeTruthy();
        expect(sampleFriends[1].hasBeenChecked).toBeTruthy();
        expect(sampleFriends[2].hasBeenChecked).toBeTruthy();
    });

    it('Should not recheck friends already checked', function () {
        var idsChecked = [];

        jQuery("<div/>").facebookFriendsInviteWidget(friendsProvider);

        sampleFriends[1].hasBeenChecked = true;

        friendsProvider.checkAlreadyRegistered = function (idsToCheck, registeredCallback) {
            idsChecked = idsToCheck;
        };

        preDrawFilter(sampleFriends);

        expect(idsChecked).toContain(sampleFriends[0].id);
        expect(idsChecked).not.toContain(sampleFriends[1].id);
        expect(idsChecked).toContain(sampleFriends[2].id);
    });

});

describe('Global Invites Setup', function () {

    var service,
        lightboxControlSpy;

    function dispatchVisibilityChangedEvent(options) {
        service.dispatchEvent(jQuery.extend({}, {
            eventType: "PopupVisibilityChanged",
            isVisible: true,
            source: "AUTOMATIC"
        }, options));
    }

    function showLightbox(options) {
        dispatchVisibilityChangedEvent(jQuery.extend({}, options, {isVisible: true}));
    }

    function hideLightbox(options) {
        dispatchVisibilityChangedEvent(jQuery.extend({}, options, {isVisible: false}));
    }

    function setIsFacebook(value) {
        YAZINO.configuration.set('facebookConnect', !!value);
    }

    function setIsOnCanvas(value) {
        YAZINO.configuration.set('onCanvas', !!value);
    }

    beforeEach(function () {
        setIsFacebook(false);
        setIsOnCanvas(false);
        service = new YAZINO.EventDispatcher();

        spyOn(service, 'addEventListener').andCallThrough();
        service.triggerPopupIfNotTriggeredRecently = jasmine.createSpy('triggerPopupIfNotTriggeredRecently');
        service.hidePopup = jasmine.createSpy('hidePopup');
        lightboxControlSpy = jasmine.createSpy('lightboxControl');
        spyOn(YAZINO.lightboxWidget, 'createIframe').andReturn({
            setCloseCallback: lightboxControlSpy
        });
        spyOn(YAZINO.lightboxWidget, 'kill');

        YAZINO.setupInvitations(service);
    });

    it('should exist', function () {
        expect(typeof YAZINO.setupInvitations).toBe('function');
    });

    it('should add legacy event listener', function () {
        expect(service.addEventListener).toHaveBeenCalled();
        expect(service.addEventListener.mostRecentCall.args[0]).toBe('PopupVisibilityChanged');
        expect(typeof service.addEventListener.mostRecentCall.args[1]).toBe('function');
    });

    it('should close lightbox to run fire visibility changed event', function () {
        showLightbox();
        expect(lightboxControlSpy).toHaveBeenCalledWith(service.hidePopup);
    });

    it('should create lightbox on popup show event', function () {
        showLightbox();
        expect(YAZINO.lightboxWidget.createIframe).toHaveBeenCalledWith('/invitation', 'invitationDialog', false);
    });

    it('should not create lightbox without popup show event', function () {
        expect(YAZINO.lightboxWidget.createIframe).not.toHaveBeenCalled();
    });

    it('should remove lightbox on popup hide event', function () {
        hideLightbox();
        expect(YAZINO.lightboxWidget.kill).toHaveBeenCalled();
    });
    it("triggerPopup tracks open event for yazino", function () {
        spyOn(YAZINO.businessIntelligence.track.invite, "open");

        showLightbox();

        expect(YAZINO.businessIntelligence.track.invite.open).toHaveBeenCalledWith('EMAIL');
    });
    it("triggerPopup tracks open event for yazino when canvas actions are not available", function () {
        spyOn(YAZINO.businessIntelligence.track.invite, "open");
        setIsFacebook(true);
        setIsOnCanvas(false);

        showLightbox();

        expect(YAZINO.businessIntelligence.track.invite.open).toHaveBeenCalledWith('EMAIL');
    });
    it("triggerPopup tracks open event for facebook when on canvas", function () {
        spyOn(YAZINO.businessIntelligence.track.invite, "open");
        setIsFacebook(true);
        setIsOnCanvas(true);

        showLightbox();

        expect(YAZINO.businessIntelligence.track.invite.open).toHaveBeenCalledWith('FACEBOOK');
    });

    it("triggerPopup tracks open event for yazino with custom ctaRef", function () {
        spyOn(YAZINO.businessIntelligence.track.invite, "open");

        showLightbox({
            ctaRef: 'cta2'
        });

        expect(YAZINO.businessIntelligence.track.invite.open).toHaveBeenCalledWith('EMAIL');
    });

    it("should call legacy inviteFriendsService.triggerPopupIfNotTriggeredRecently", function () {
        expect(service.triggerPopupIfNotTriggeredRecently).toHaveBeenCalled();
    });

});
