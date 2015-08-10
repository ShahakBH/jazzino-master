/*global document, jQuery, setInterval, setTimeout, window, YAZINO, parent, StrataPartnerApi, alert */
var YAZINO = YAZINO || {};
//budServ
YAZINO.createBuddyService = function (playerRelationshipsService) {
    var service = {}, currentPage = 0, listOfBuddyIds,
        pageSize = parseInt(YAZINO.configuration.get('getBuddiesPageSize'), 0),
        hasNextPage = false,
        hasPrevPage = false,
        loadedBuddyList = [],
        filterText = "",
        lastSentBuddyId = -1,
        getBuddyDetailsForPage = function (pageNumber) {
            if (listOfBuddyIds === undefined || listOfBuddyIds.length === 0) {
                service.dispatchEvent({eventType: "BuddiesLoaded", hasPreviousPage: false, hasNextPage: false, buddies: listOfBuddyIds});
            } else {
                var paginatedBuddies = service.getPaginatedBuddies(listOfBuddyIds, pageNumber);
                YAZINO.logger.log("getting dets for buddies:", paginatedBuddies);
                jQuery.ajax({type: "get",
                    url: "/social/players",
                    data: {playerIds: paginatedBuddies.join(","), details: "name,picture,online,locations"}})
                    .done(function (data) {
                        service.dispatchEvent({eventType: "BuddiesLoaded", hasPreviousPage: hasPrevPage, hasNextPage: hasNextPage, buddies: data.players});
                    });
            }
        },
        yank = function (buddyId) {
            var i;
            if (loadedBuddyList && loadedBuddyList.buddies) {
                for (i = 0; i < loadedBuddyList.buddies.length; i += 1) {
                    if (loadedBuddyList.buddies[i][0] === buddyId) {
                        loadedBuddyList.buddies.splice(i, 1);
                        break;
                    }
                }
            }
        },
        filter = function (buddies, filterText) {
            var i, id, filtered = [];

            if (buddies !== undefined) {
                YAZINO.logger.debug("filtering " + buddies.length + " buddies for word" + filterText);
                for (i = 0; i < buddies.length; i += 1) {
                    if (buddies[i][1].toUpperCase().indexOf(filterText.toUpperCase()) !== -1) {
                        filtered.push(buddies[i][0]);
                    }
                }
            }
            return filtered;
        };

    YAZINO.EventDispatcher.apply(service);

    service.removeBuddy = function (buddyId) {
        if (!buddyId) {
            throw {
                message: "Missing Buddy Id"
            };
        }
        yank(buddyId);
        playerRelationshipsService.unfriendRequest(buddyId);
    };

    service.getPaginatedBuddies = function (buddies, pageNumber) {
        var pageStart = pageSize * pageNumber,
            pageEnd = (pageSize * pageNumber) + pageSize;

        hasNextPage = pageEnd < buddies.length;
        hasPrevPage = pageNumber !== 0;

        if (pageEnd > buddies.length) {
            pageEnd = buddies.length;
        }

        return buddies.slice(pageStart, pageEnd);
    };

    service.showNextPage = function () {
        currentPage = currentPage + 1;
        YAZINO.logger.log("showing next page:", currentPage);
        getBuddyDetailsForPage(currentPage);
    };

    service.showPreviousPage = function () {
        currentPage = currentPage - 1;
        YAZINO.logger.log("showing previous page:", currentPage);
        getBuddyDetailsForPage(currentPage);
    };

    service.addEventListener("BuddyIdsLoaded", function (buddies) {
        service.initialiseListOfBuddyIds(buddies.buddies, currentPage);
        getBuddyDetailsForPage(currentPage);
    });

    service.filterAndDispatch = function (filterText) {
        var filteredBuddies = filter(loadedBuddyList.buddies, filterText);
        currentPage = 0;
        if (loadedBuddyList.buddies.length > 0) {
            service.dispatchEvent({eventType: "BuddyIdsLoaded", buddies: filteredBuddies});
        } else {
            service.dispatchEvent({eventType: "BuddyIdsLoaded"});
        }

    };

    service.getBuddies = function () {
        jQuery.ajax({type: "get", url: "/api/1.0/social/buddiesNames", data: {'ie': Date.now()}}).done(function (buddies) {
            loadedBuddyList = buddies;
            service.filterAndDispatch(filterText);
        });
    };

    service.sendChallenge = function (buddyId, doneCallback) {
        if (buddyId !== lastSentBuddyId) {
            jQuery.ajax({type: "put",
                url: "/challenge",
                data: {buddyId: buddyId}})
                .done(doneCallback);
            lastSentBuddyId = buddyId;
        }
    };

    service.redirectParent = function (tableId) {
        window.parent.location.href = "/table/" + tableId;
    };

    //this is for testing...
    service.initialiseListOfBuddyIds = function (buddyIds, currPage) {
        listOfBuddyIds = buddyIds;
        currentPage = currPage;
    };
    service.hasNextPage = function () {
        return hasNextPage;
    };
    service.hasPrevPage = function () {
        return hasPrevPage;
    };


    return service;
};

//budWid
jQuery.fn.extend({
    buddyListWidget: /** @this {!Object} */function (buddyService) {
        return this.each(function () {
            var widget = jQuery('.buddyListRoot'),
                buddiesList = jQuery("<ul/>"),
                repopulateBuddies,
                previousButton,
                nextButton,
                searchBar;

            function searchBarChanged() {
                var currentValue = searchBar.val();
                if (searchBarChanged.previousValue === currentValue) {
                    return;
                }
                searchBarChanged.previousValue = currentValue;
                if (jQuery.trim(currentValue).length === 0) {
                    buddyService.filterAndDispatch("");
                    return;
                }

                buddyService.filterAndDispatch(currentValue);
            }


            function setupStaticButtons() {
                searchBar = jQuery('.search input')
                    .blur(searchBarChanged)
                    .change(searchBarChanged)
                    .keyup(searchBarChanged);
            }
            setupStaticButtons();

            repopulateBuddies = function (event) {
                function initWidget() {
                    widget.html('');
                    widget.append(buddiesList);
                    buddiesList.html("");
                }

                function addPreviousPageButton() {
                    if (event.hasPreviousPage === true) {
                        previousButton = jQuery("<li/>").addClass("buddyListItem pageTurn").append(jQuery("<span/>").text("Previous Page"))
                            .click(buddyService.showPreviousPage);
                        buddiesList.append(previousButton);
                    }
                }

                function addNextPageButton() {
                    if (event.hasNextPage === true) {
                        nextButton = jQuery("<li/>").addClass("buddyListItem pageTurn").append(jQuery("<span/>").text("Next Page"))
                            .click(buddyService.showNextPage);
                        buddiesList.append(nextButton);
                    }
                }

                function showBuddiesList() {
                    jQuery('.haveNoBuddies').hide();
                    jQuery('.haveBuddies').show();
                    widget.removeClass('noBuddiesMatchingSelection');
                }

                function showLoserView() {
                    jQuery('.haveNoBuddies').show();
                    jQuery('.haveBuddies').hide();
                }

                function showNoBuddiesMatching() {
                    widget.append('<div/>').addClass('noBuddiesMatchingSelection').text('None of your buddies match your search.');
                }

                function addBuddies() {

                    jQuery.each(event.buddies, function (index, buddy) {
                        var online, buddyListItem, tableId;
                        YAZINO.logger.log(buddy.status);
                        if (buddy.online === true) {
                            if (buddy.locations.length > 0) {
                                online = "playing " + buddy.locations[0].gameType.toLowerCase().split("_").join(" ");
                                tableId = buddy.locations[0].locationId;
                            } else {
                                online = "online";
                            }
                        } else {
                            online = "offline";
                        }
                        buddyListItem = jQuery('.template .buddyListItemTemplate')
                            .clone();
                        buddyListItem
                            .removeClass('buddyListItemTemplate').addClass('buddyListItem')
                            .find('.avatar').attr("src", buddy.picture).attr("alt", buddy.name).end()
                            .find('.playerName').text(buddy.name).end()
                            .find('.playerStatus').text(online).end()
                            .find('.featureLink.removeBuddy').removeBuddyWidget(buddyService, buddy.playerId, buddy.name, buddyListItem).end()
                            .find('.featureLink.challengeBuddy').joinOrChallengeButton(buddyService, tableId, buddy.playerId).end()
                            .appendTo(buddiesList);
                    });
                }

                initWidget();
                YAZINO.logger.log("adding buddyWidget");
                if (event.buddies !== undefined) {
                    if (event.buddies.length > 0) {
                        showBuddiesList();
                        addPreviousPageButton();
                        addBuddies();
                        addNextPageButton();
                        setupStaticButtons();
                    } else {
                        showNoBuddiesMatching();
                    }
                } else {
                    showLoserView();
                }
                YAZINO.util.resizeIframeLightbox();
            };
            buddyService.addEventListener("BuddiesLoaded", repopulateBuddies); //gets ready to rec'v buddies

            buddyService.getBuddies();//kicks off the loading
        });
    }
});

jQuery.fn.extend({
    joinOrChallengeButton: function (buddyService, tableId, buddyId) {
        return this.each(
            function () {
                var widget = jQuery(this), button = widget.find('a');
                if (tableId !== undefined) {
                    widget.addClass("joinBuddy").removeClass("challengeBuddy");

                    button.attr('href', '#')
                        .text('Join')
                        .click(function () {
                            buddyService.redirectParent(tableId);
                        });

                } else {

                    widget.removeClass("joinBuddy").addClass("challengeBuddy");

                    button.attr('href', '#')
                        .text('Send Challenge')
                        .click(function () {
                            if (button.attr("disabled") !== 'disabled') {
                                buddyService.sendChallenge(buddyId, function () {
                                    button.text("Challenge Sent").attr('disabled', 'disabled');
                                });
                            }
                        });
                }
                return widget;

            }
        );
    }
});

jQuery.fn.extend({
    removeBuddyWidget: /** @this {!Object} */function (buddyService, buddyId, buddyName, buddyListItem) {
        return this.each(
            function () {
                var widget = jQuery(this);

                if (!buddyId) {
                    throw {
                        message: "Missing Buddy Id"
                    };
                }

                widget.click(function () {
                    YAZINO.lightboxWidget.confirmationBox("Remove Buddy?",
                        "Are you sure you want to remove '" + buddyName + "' from your Buddy List?",
                        function () {
                            buddyService.removeBuddy(buddyId);
                            buddyListItem.remove();
                        });
                });
            }
        );
    }
});

jQuery(document).ready(
    function () {
        var buddyService = YAZINO.createBuddyService(YAZINO.playerRelationshipsService);
        jQuery('article.buddyListStatement').buddyListWidget(buddyService);
    }
);
