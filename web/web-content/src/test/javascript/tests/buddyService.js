/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, console, $ */

describe('Buddy Service', function () {
    var underTest, deferredAjax, deferredPost, deferredGet, friendsSummaryEventType, buddiesLoadedEventType, playerRelationshipService,
        buddiesList;

    beforeEach(function () {
        buddiesList = [
            [111, "aaa"],
            [222, "bbb"],
            [333, "ccc"],
            [444, "ddd"],
            [555, "eee"],
            [666, "fff"],
            [777, "ggg"],
            [123, "abc"],
            [456, "def"],
            [789, "bob geldof"]
        ];
        friendsSummaryEventType = "FRIENDS_SUMMARY";
        buddiesLoadedEventType = "BuddiesLoaded";
        var unfriendRequest = jasmine.createSpy("relationship service");
        playerRelationshipService = {unfriendRequest: unfriendRequest};

        deferredPost = jQuery.Deferred();
        spyOn(jQuery, "post").andReturn(deferredPost);

        deferredGet = jQuery.Deferred();
        spyOn(jQuery, "get").andReturn(deferredGet);

        deferredAjax = jQuery.Deferred();
        spyOn(jQuery, "ajax").andReturn(deferredAjax);

        YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 5});
    });

    function setupService() {
        underTest = YAZINO.createBuddyService(playerRelationshipService);
    }


    function getBuddyIds(buddies) {
        var i, buddyIds = [];
        for (i = 0; i < buddies.length; i += 1) {
            buddyIds.push(buddies[i][0]);
        }
        return buddyIds;
    }

    describe('removeBuddy', function () {
        it("should post id to be deleted to controller", function () {
            setupService();
            underTest.removeBuddy(123);
            expect(playerRelationshipService.unfriendRequest).toHaveBeenCalledWith(123);
        });

        it("should remove the id from the cached ids", function () {
            var buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener"),
//                buddyDetailsEventListener = jasmine.createSpy("buddyDetailsEventListener"),
                buddyIds = getBuddyIds(buddiesList),
                ayz = [111, 123],
                ay = [111];
            setupService();
            underTest.addEventListener("BuddyIdsLoaded", buddiesLoadedEventListener);
//            underTest.addEventListener("BuddyListLoaded", buddyDetailsEventListener);
//            alert(buddiesList);
            deferredAjax.resolve({"buddies": buddiesList});
            underTest.getBuddies();

            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: buddyIds});
            underTest.filterAndDispatch("a");

            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: ayz});

            underTest.removeBuddy(123);
            underTest.filterAndDispatch("a");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: ay});

        });
    });

    describe('send challenge', function () {
        it("should send a challenge using ajax", function () {
            var done = jasmine.createSpy("done");
            underTest.sendChallenge(123, done);
            deferredAjax.resolve();
            expect(done.callCount).toBe(1);
            underTest.sendChallenge(123, done);
            deferredAjax.resolve();
            expect(done.callCount).toBe(1);
            underTest.sendChallenge(321, done);
            deferredAjax.resolve();
            expect(done.callCount).toBe(2);
        });

    });

    describe('getBuddies', function () {

        it('should return Pages of Buddies', function () {
            var seven = [111, 222, 333, 444, 555, 666, 777],
                five = [111, 222, 333, 444, 555],
                three = [111, 222, 333];
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 5});

            setupService();

            expect(underTest.getPaginatedBuddies(three, 0)).toEqual([111, 222, 333]);
            expect(underTest.getPaginatedBuddies(seven, 0)).toEqual([111, 222, 333, 444, 555]);
            expect(underTest.getPaginatedBuddies(seven, 1)).toEqual([666, 777]);
            expect(underTest.getPaginatedBuddies(seven, 2)).toEqual([]);
            expect(underTest.getPaginatedBuddies(five, 0)).toEqual([111, 222, 333, 444, 555]);
            expect(underTest.getPaginatedBuddies(five, 1)).toEqual([]);
            expect(underTest.getPaginatedBuddies([], 0)).toEqual([]);
            expect(underTest.getPaginatedBuddies([], 10)).toEqual([]);
        });

        it('should show contain whether page has a next or previous page', function () {
            var buddyIds = [111, 222, 333, 444, 555, 666];
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 2});

            setupService();

            underTest.getPaginatedBuddies(buddyIds, 0);
            expect(underTest.hasNextPage()).toBeTruthy();
            expect(underTest.hasPrevPage()).toBeFalsy();
            underTest.getPaginatedBuddies(buddyIds, 1);
            expect(underTest.hasNextPage()).toBeTruthy();
            expect(underTest.hasPrevPage()).toBeTruthy();
            underTest.getPaginatedBuddies(buddyIds, 2);
            expect(underTest.hasNextPage()).toBeFalsy();
            expect(underTest.hasPrevPage()).toBeTruthy();

        });

        it('should move between pages properly', function () {
            var buddies = [111, 222, 333, 444, 555, 666];
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 2});

            setupService();

            underTest.getPaginatedBuddies(buddies, 0);
            expect(underTest.hasNextPage()).toBeTruthy();
            expect(underTest.hasPrevPage()).toBeFalsy();
            underTest.getPaginatedBuddies(buddies, 1);
            expect(underTest.hasNextPage()).toBeTruthy();
            expect(underTest.hasPrevPage()).toBeTruthy();
            underTest.getPaginatedBuddies(buddies, 2);
            expect(underTest.hasNextPage()).toBeFalsy();
            expect(underTest.hasPrevPage()).toBeTruthy();

        });

        it("should exist", function () {
            setupService();
            expect(underTest.getBuddies).toBeDefined();
        });

        it("should look up page size", function () {
            spyOn(YAZINO.configuration, "get");
            setupService();
            expect(YAZINO.configuration.get).toHaveBeenCalledWith("getBuddiesPageSize");
        });

        it("should call social api to get list of buddies", function () {
            setupService();
            underTest.getBuddies();
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/api/1.0/social/buddiesNames", data: {'ie': jasmine.any(Number)}});

        });

        it("should despatch loaded buddyList ids", function () {

            var buddies = buddiesList.slice(0, 5),
                buddyIds = getBuddyIds(buddies),
                buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");

            setupService();
            underTest.addEventListener("BuddyIdsLoaded", buddiesLoadedEventListener);

            deferredAjax.resolve({"buddies": buddies});

            underTest.getBuddies();
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/api/1.0/social/buddiesNames", data: {'ie': jasmine.any(Number)}});

            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({ eventType: 'BuddyIdsLoaded', buddies: buddyIds});
        });

        it("should get buddy details and post event with paginated buddy details", function () {
            var buddies = [123, 456, 789, 666, 777],
                buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 3});
            spyOn(YAZINO.configuration, "get").andCallThrough();
            setupService();
            underTest.addEventListener("BuddiesLoaded", buddiesLoadedEventListener);

            underTest.dispatchEvent({eventType: "BuddyIdsLoaded", buddies: buddies});

            expect(YAZINO.configuration.get).toHaveBeenCalledWith("getBuddiesPageSize");
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/social/players", data: {playerIds: "123,456,789", details: "name,picture,online,locations"}});
            deferredAjax.resolve({result: "ok", players: [
                {playerId: 123, name: "your mum", online: "false", url: 'a'},
                {playerId: 456, name: "my mum", online: "false", url: 'b'},
                {playerId: 789, name: "our mum", online: "true", url: 'c'}
            ]});
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddiesLoaded", hasNextPage: true, hasPreviousPage: false, buddies: [
                {playerId: 123, name: "your mum", online: "false", url: 'a'},
                {playerId: 456, name: "my mum", online: "false", url: 'b'},
                {playerId: 789, name: "our mum", online: "true", url: 'c'}
            ]});
        });

        it("should dispatch event with second page of data", function () {
            var buddies = [123, 456, 789, 666, 777],
                buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 3});

            setupService();
            underTest.addEventListener("BuddiesLoaded", buddiesLoadedEventListener);

            underTest.initialiseListOfBuddyIds(buddies, 0);

            underTest.showNextPage();
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/social/players", data: {playerIds: "666,777", details: "name,picture,online,locations"}});
            deferredAjax.resolve({result: "ok", players: [
                {playerId: 666, name: "your mum", online: "false", url: 'a'},
                {playerId: 777, name: "our mum", online: "true", url: 'c'}
            ]});
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddiesLoaded", hasNextPage: false, hasPreviousPage: true, buddies: [
                {playerId: 666, name: "your mum", online: "false", url: 'a'},
                {playerId: 777, name: "our mum", online: "true", url: 'c'}
            ]});
        });

        it("should dispatch event with previous page of data", function () {
            var buddies = [123, 456, 789, 666, 777],
                buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");
            YAZINO.configuration = YAZINO.configurationFactory({getBuddiesPageSize: 3});

            setupService();
            underTest.addEventListener("BuddiesLoaded", buddiesLoadedEventListener);
            underTest.initialiseListOfBuddyIds(buddies, 1);

            underTest.showPreviousPage();
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/social/players", data: {playerIds: "123,456,789", details: "name,picture,online,locations"}});
            deferredAjax.resolve({result: "ok", players: [
                {playerId: 123, name: "your mum", online: "false", url: 'a'},
                {playerId: 456, name: "my mum", online: "false", url: 'b'},
                {playerId: 789, name: "our mum", online: "true", url: 'c'}
            ]});
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddiesLoaded", hasNextPage: true, hasPreviousPage: false, buddies: [
                {playerId: 123, name: "your mum", online: "false", url: 'a'},
                {playerId: 456, name: "my mum", online: "false", url: 'b'},
                {playerId: 789, name: "our mum", online: "true", url: 'c'}
            ]});
        });

        it("shouldFilterOnNameAndRepublishIds", function () {
            var buddies = buddiesList,
                buddyIds = getBuddyIds(buddies),
                buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");

            setupService();
            underTest.addEventListener("BuddyIdsLoaded", buddiesLoadedEventListener);

            deferredAjax.resolve({"buddies": buddies});

            underTest.getBuddies();
            expect(jQuery.ajax).toHaveBeenCalledWith({type: "get", url: "/api/1.0/social/buddiesNames", data: {'ie': jasmine.any(Number)}});
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({ eventType: 'BuddyIdsLoaded', buddies: buddyIds});

            underTest.filterAndDispatch("a");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: [111, 123]});

            underTest.filterAndDispatch("bb");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: [222]});

            underTest.filterAndDispatch("ccc");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: [333]});

            underTest.filterAndDispatch("b");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: [222, 123, 789]});

            underTest.filterAndDispatch("b g");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: [789]});

            underTest.filterAndDispatch("q");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: []});

            underTest.filterAndDispatch("");
            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddyIdsLoaded", buddies: buddyIds});


        });

        it("should not lookup buddies data when player is a loser", function () {
            var buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");
            setupService();

            underTest.addEventListener("BuddiesLoaded", buddiesLoadedEventListener);

            underTest.dispatchEvent({eventType: "BuddyIdsLoaded"});

            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddiesLoaded", hasPreviousPage: false, hasNextPage: false, buddies: undefined});
        });

        it("should not lookup buddies data when player is a loser", function () {
            var buddiesLoadedEventListener = jasmine.createSpy("buddiesLoadedListener");
            setupService();

            underTest.addEventListener("BuddiesLoaded", buddiesLoadedEventListener);

            underTest.dispatchEvent({eventType: "BuddyIdsLoaded", buddies: []});

            expect(buddiesLoadedEventListener).toHaveBeenCalledWith({eventType: "BuddiesLoaded", hasPreviousPage: false, hasNextPage: false, buddies: []});
        });

    });
});
