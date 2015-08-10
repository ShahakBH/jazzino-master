/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("Yazino Buddies Challenge Widget", function () {

    var widget,
        any = jasmine.any,
        deferredAjax,
        knownUserMap = {
            '123': {
                name: 'Mat Carey',
                provider: 'facebook'
            },
            '456': {
                name: '',
                provider: 'facebook'
            },
            '789': {
                name: 'Mr. Bob',
                provider: 'YAZINO'
            }
        },
        ajaxUserIdsRequested = [],
        facebookService,
        assume = expect,
        facebookCanvasActionsAllowed,
        facebookBuddyChallenge,
        facebookFriendsRequested,
        peopleLoadedHandler;

    function lookupUser(id) {
        return {
            name: knownUserMap[id] === undefined ? id + "'s name" : knownUserMap[id].name,
            provider: 'yazino',
            playerId: id
        };
    }

    function getFormattedPersonWithComment(id) {
        var user = lookupUser(id);
        return {
            id: user.playerId,
            displayName: user.name,
            provider: user.provider
        };
    }

    function getFormattedPerson(id) {
        return getFormattedPersonWithComment(id);
    }

    function setFacebookFriends(friends) {
        function dispatchEvent() {

            peopleLoadedHandler({
                eventType: "PeopleLoaded",
                friends: friends
            });
        }

        $.each(friends, function () {
            if (!this.id || !this.name) {
                throw 'invalid friend format - to mock facebook friends we should use .id and .name';
            }
        });

        if (facebookFriendsRequested) {
            dispatchEvent();
        } else {
            window.alert('not loaded yet');
            facebookService.getPeople.andCallFake(function () {
                facebookFriendsRequested = true;
                dispatchEvent();
            });
        }
    }

    function setupWidget() {
        widget = $('<div/>');
        facebookService = YAZINO.facebookFriendsService();
        spyOn(YAZINO.configuration, 'get').andCallFake(function (key) {
            if (key === 'facebookCanvasActionsAllowed' || key === 'onCanvas') {
                return facebookCanvasActionsAllowed;
            }
            if (key === 'socialFlow.challenge.allowFacebookBuddyChallenge') {
                return facebookBuddyChallenge;
            }
        });
        facebookBuddyChallenge = true;
        spyOn(facebookService, 'challengePeople');
        spyOn(facebookService, 'getPeople').andCallFake(function () {facebookFriendsRequested = true; });
        spyOn(facebookService, 'addEventListener').andCallFake(function (event, handler) {
            if (event === 'PeopleLoaded') {
                peopleLoadedHandler = handler;
            }
        });
        spyOn($.fn, "personSelector").andCallThrough();
        widget = widget.buddiesChallengeWidget(facebookService);
        spyOn(widget, "addPeople");
    }

    function sendSuccessCallbackWithData(callbackData) {
        $.fn.personSelector.mostRecentCall.args[0](callbackData);
    }

    beforeEach(function () {
        deferredAjax = [];
        facebookCanvasActionsAllowed = true;
        facebookFriendsRequested = false;
        spyOn(YAZINO.socialFlow.util, 'browserPost');
        spyOn(YAZINO.socialFlow.util, 'redirect');
        peopleLoadedHandler = null;
        spyOn($, "ajax").andCallFake(function (url, config) {
            if (url === '/social/players') {
                ajaxUserIdsRequested = config.data.playerIds.split(',');
            }
            deferredAjax[url] = $.Deferred();
            return deferredAjax[url];
        });
    });

    it('should exist', function () {
        expect(typeof $('body').buddiesChallengeWidget).toBe('function');
    });

    it('should create person selector with config', function () {
        setupWidget();
        expect(widget.personSelector).toHaveBeenCalledWith(any(Function), {
            copy: {
                title: 'Your Yazino Buddies',
                continueButton: 'Send Challenge',
                searchLabel: 'Search',
                peopleSelectedLabel: 'Buddies Selected'
            }
        });
    });

    it('should register event listener for buddies', function () {
        setupWidget();
        expect($.ajax).toHaveBeenCalledWith("/api/1.0/social/buddiesNames");
    });

    it('should add buddies from event to selector', function () {
        setupWidget();
        deferredAjax["/api/1.0/social/buddiesNames"].resolve({buddies: [[1, "1's name"], [2, "2's name"], [4, "4's name"]]});
        expect(widget.addPeople).toHaveBeenCalledWith([
            getFormattedPerson(1),
            getFormattedPerson(2),
            getFormattedPerson(4)
        ]);
    });

    it('should send challenges via ajax on success callback for Yazino users', function () {
        var callbackData = [{id: 123, displayName: "Mat Carey", provider: "YAZINO"}, {id: 456, displayName: "Jae Rae", provider: "YAZINO"}];
        setupWidget();
        sendSuccessCallbackWithData(callbackData);
        expect(YAZINO.socialFlow.util.browserPost).toHaveBeenCalledWith({
            buddyIds: [123, 456]
        }, '/challenge');
    });
});
