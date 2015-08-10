/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("facebookChallengeWidget", function () {
    var sampleFriends,
        friendsProvider = {
            getPeople: function () {},
            checkAlreadyRegistered: function () {},
            challengePeople: function () {},
            maxInvitations: 50
        },
        personSelector,
        eventListeners = {},
        assume = expect,
        anyFunction = jasmine.any(Function),
        continueListener,
        personSelectorConfig;

    YAZINO.EventDispatcher.apply(friendsProvider);

    beforeEach(function () {
        sampleFriends = [
            { id: "1", displayName: "Person A" },
            { id: "2", displayName: "Person B" },
            { id: "3", displayName: "Person C" }
        ];
        personSelector = jQuery('<div/>').personSelector(function () {});

        spyOn(jQuery.fn, "personSelector").andCallFake(function (continueCallback, config) {
            continueListener = continueCallback;
            personSelectorConfig = config;
            return personSelector;
        });
        spyOn(personSelector, 'addPeople');
        spyOn(personSelector, 'modelChanged');
        spyOn(personSelector, 'rebuildView');

        spyOn(friendsProvider, "addEventListener").andCallFake(function (key, fn) {
            eventListeners[key] = fn;
        });
        spyOn(friendsProvider, 'getPeople').andCallThrough();
        spyOn(friendsProvider, 'checkAlreadyRegistered');
        spyOn(friendsProvider, 'challengePeople');
    });

    it('should exist', function () {
        expect($('body').facebookChallengeWidget).toBeDefined();
    });

    function setUpWidget() {
        $('<div/>').facebookChallengeWidget(friendsProvider);
    }

    it('should lookup facebook friends on init', function () {
        setUpWidget();
        expect(friendsProvider.getPeople).toHaveBeenCalled();
    });

    it('should register event listener for people loaded event', function () {
        setUpWidget();
        expect(friendsProvider.addEventListener).toHaveBeenCalledWith("PeopleLoaded", anyFunction);
    });

    it('should add friends when they are loaded', function () {
        setUpWidget();
        assume(personSelector.addPeople).not.toHaveBeenCalled();
        assume(friendsProvider.addEventListener).toHaveBeenCalledWith("PeopleLoaded", anyFunction);
        eventListeners.PeopleLoaded({friends: sampleFriends});
        expect(personSelector.addPeople).toHaveBeenCalledWith(sampleFriends);
    });

    it('should pass selected people to challengePeople on success callback', function () {
        setUpWidget();
        continueListener(sampleFriends);
        expect(friendsProvider.challengePeople).toHaveBeenCalledWith(['1', '2', '3'], jasmine.any(Function));
    });

    it('should give correct config', function () {
        YAZINO.configuration.set('socialFlow.batchSize', 500);
        setUpWidget();
        expect(personSelectorConfig.copy.title).toBe('Your Facebook Friends');
        expect(personSelectorConfig.copy.continueButton).toBe('Send Challenge');
        expect(personSelectorConfig.copy.searchLabel).toBe('Search Friends');
        expect(personSelectorConfig.copy.peopleSelectedLabel).toBe('Friends Selected');
        expect(personSelectorConfig.pageSize).toBe(500);
        expect(personSelectorConfig.maxSelectable).toBe(50);

        spyOn(window, 'alert');
        personSelectorConfig.maxSendLimitHitAction();
        expect(window.alert).toHaveBeenCalledWith("You can only send 50 challenges at a time! Challenge more friends after sending these challenges.");
    });

});
