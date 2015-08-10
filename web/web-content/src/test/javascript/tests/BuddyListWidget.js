/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $ */

describe('BuddyWidget', function () {
    var buddyService, backupWidget, lightbox, widget;

    beforeEach(function () {
        buddyService = {
            removeBuddy: jasmine.createSpy('removeBuddy'),
            getBuddies: jasmine.createSpy('getBuddies'),
            filterAndDispatch: jasmine.createSpy('filter'),
            sendChallenge: function (id, callback) { callback(); },
            redirectParent: jasmine.createSpy('redirect')
        };
        lightbox = {confirmationBox: function (title, body, success) {
            success();
        }};
        backupWidget = YAZINO.lightboxWidget;

        YAZINO.lightboxWidget = lightbox;

        YAZINO.EventDispatcher.apply(buddyService);
        widget = jQuery(".buddyListStatementTest");
    });

    afterEach(function () {
        YAZINO.lightboxWidget = backupWidget;
    });

    describe('buddy list widget ', function () {

        it("should load up buddies", function () {

            widget.buddyListWidget(buddyService);

            expect(buddyService.getBuddies).toHaveBeenCalled();
        });

        it("should repopulate buddiesList on BuddiesLoaded event", function () {
            var buddiesEvent = { eventType: 'BuddiesLoaded', hasNextPage: false, hasPreviousPage: false, buddies: [
                    { playerId: 123, name: 'your mum', online: 'false', picture: 'a/pic' },
                    { playerId: 456, name: 'my mum', online: 'false', picture: 'b/pic' },
                    { playerId: 789, name: 'our mum', online: 'true', picture: 'c/pic' }
                ] };

            widget.buddyListWidget(buddyService);

            buddyService.dispatchEvent(buddiesEvent);
            expect(widget.find("li.buddyListItem").length).toBe(3);
            expect(widget.find(".buddyListItem span.removeBuddy").length).toBe(3);

            expect(jQuery(widget.find(".playerName")[0]).text()).toBe("your mum");
            expect(jQuery(widget.find("img.avatar")[0]).attr('src')).toBe("a/pic");

            expect(jQuery(widget.find(".playerName")[1]).text()).toBe("my mum");
            expect(jQuery(widget.find("img.avatar")[1]).attr('src')).toBe("b/pic");

            expect(jQuery(widget.find(".playerName")[2]).text()).toBe("our mum");
            expect(jQuery(widget.find("img.avatar")[2]).attr('src')).toBe("c/pic");

        });

        it("should show next button when there is a further page", function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: true, hasPreviousPage: false, buddies: [
                {playerId: 111, name: "your mum", online: "false", url: 'a'},
                {playerId: 222, name: "our mum", online: "true", url: 'c'}
            ]};

            widget.buddyListWidget(buddyService);
            buddyService.dispatchEvent(event);
            expect(widget.find("li.buddyListItem").length).toBe(3);

            expect(jQuery(widget.find("li.buddyListItem")[2]).text()).toBe("Next Page");

        });

        it("should show previous button when there is a previous page", function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: false, hasPreviousPage: true, buddies: [
                {playerId: 111, name: "your mum", online: "false", url: 'a'},
                {playerId: 222, name: "our mum", online: "true", url: 'c'}
            ]};

            widget.buddyListWidget(buddyService);
            buddyService.dispatchEvent(event);
            expect(widget.find("li.buddyListItem").length).toBe(3);

            expect(jQuery(widget.find("li.buddyListItem")[0]).text()).toBe("Previous Page");

        });

        it("should show both next and prev button when there is a next and a previous page", function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: true, hasPreviousPage: true, buddies: [
                {playerId: 111, name: "your mum", online: "false", url: 'a'},
                {playerId: 222, name: "our mum", online: "true", url: 'c'}
            ]};

            widget.buddyListWidget(buddyService);
            buddyService.dispatchEvent(event);
            expect(widget.find("li.buddyListItem").length).toBe(4);

            expect(jQuery(widget.find("li.buddyListItem")[0]).text()).toBe("Previous Page");
            expect(jQuery(widget.find("li.buddyListItem")[3]).text()).toBe("Next Page");

        });

        it('it should show "you have no friends" when you are a loser', function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: false, hasPreviousPage: false};

            widget.buddyListWidget(buddyService);
            buddyService.dispatchEvent(event);
            expect(widget.find("li.buddyListItem").length).toBe(0);
            expect(jQuery('.haveBuddies').is(':visible')).toBe(false);
            expect(jQuery('.haveNoBuddies').is(':visible')).toBe(true);
            expect(jQuery('.noBuddiesMatchingSelection').is(':visible')).toBe(false);
        });

        it('it should not show "you have no friends" when you have friends', function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: false, hasPreviousPage: false, buddies: [
                    {playerId: 111, name: "your mum", online: "false", url: 'a'}
                ]};
            widget.buddyListWidget(buddyService);

            buddyService.dispatchEvent(event);

            expect(widget.find("li.buddyListItem").length).toBe(1);
            expect(jQuery('.haveBuddies').is(':visible')).toBe(true);
            expect(jQuery('.haveNoBuddies').is(':visible')).toBe(false);
            expect(jQuery('.noBuddiesMatchingSelection').is(':visible')).toBe(false);
        });

        it('it should not show "you have no friends" when you have friends but have filtered them all out', function () {
            var event = {eventType: "BuddiesLoaded", hasNextPage: false, hasPreviousPage: false, buddies: []};
            widget.buddyListWidget(buddyService);

            buddyService.dispatchEvent(event);

            expect(widget.find("li.buddyListItem").length).toBe(0);
            expect(jQuery('.haveBuddies').is(':visible')).toBe(true);
            expect(jQuery('.haveNoBuddies').is(':visible')).toBe(false);
            expect(jQuery('.noBuddiesMatchingSelection').is(':visible')).toBe(true);
        });

        it('should update filter on blur of search', function () {
            widget.buddyListWidget(buddyService);
            widget.find('.search input').val('a').blur();
            expect(buddyService.filterAndDispatch).toHaveBeenCalledWith("a");
        });

        it('should update filter on change of search', function () {
            widget.buddyListWidget(buddyService);
            widget.find('.search input').val('a').change();
            expect(buddyService.filterAndDispatch).toHaveBeenCalledWith("a");
        });

        it('should update filter on keyup of search', function () {
            widget.buddyListWidget(buddyService);
            widget.find('.search input').val('a').keyup();
            expect(buddyService.filterAndDispatch).toHaveBeenCalledWith("a");
        });
    });

    describe('joinOrChallengeBuddyWidget', function () {
        it('should show join table when table id is present', function () {
            var widget = $('#challengeWidget').clone().joinOrChallengeButton(buddyService, 123, 456);
            expect($(widget).find('a').text()).toBe('Join');
            expect(widget.hasClass('joinBuddy')).toBeTruthy();
            expect(widget.hasClass('challengeBuddy')).toBeFalsy();
        });

        it('should show challenge Buddy when table id is present', function () {
            var widget = $('#challengeWidget').clone().joinOrChallengeButton(buddyService, undefined, 456);
            expect($(widget).find('a').text()).toBe('Send Challenge');
            expect(widget.hasClass('joinBuddy')).toBeFalsy();
            expect(widget.hasClass('challengeBuddy')).toBeTruthy();
        });

        it('should change challenge text on click of the challenge button', function () {
            var widget = $('#challengeWidget').clone().joinOrChallengeButton(buddyService, undefined, 456);
            $(widget).find('a').click();
            expect($(widget).find('a').text()).toBe("Challenge Sent");

        });

        it('should fire challenge request on click of the challenge button', function () {
            var widget = $('#challengeWidget').clone().joinOrChallengeButton(buddyService, undefined, 456);
            buddyService.sendChallenge = jasmine.createSpy();
            $(widget).find('a').click();
            expect(buddyService.sendChallenge).toHaveBeenCalledWith(456, jasmine.any(Function));

        });

        it('should redirect parent on click of the join button', function () {
            var widget = $('#challengeWidget').clone().joinOrChallengeButton(buddyService, 123, undefined);
            $(widget).find('a').click();
            expect(buddyService.redirectParent).toHaveBeenCalledWith(123);
        });
    });

    describe('removeBuddyWidget', function () {

        it("should call removeBuddy on the service on click", function () {
            var widget = $("#removeBuddyWidget");
            widget.removeBuddyWidget(buddyService, "123", "Jim", jQuery("<li/>"));
            widget.click();
            expect(buddyService.removeBuddy).toHaveBeenCalledWith("123");
        });

        it("should remove player from dom", function () {
            var widget = $(".removeBuddyWidget"),
                listItem = jQuery("<li/>");
            spyOn(listItem, "remove");

            widget.removeBuddyWidget(buddyService, "123", "Name", listItem);
            widget.click();
            expect(listItem.remove).toHaveBeenCalled();
        });
    });
});
