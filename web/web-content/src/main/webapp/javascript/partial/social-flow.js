/*global alert, clearInterval, clearTimeout, location, document, FB, jQuery, setInterval, setTimeout, window, YAZINO, parent, StrataPartnerApi */

var YAZINO = YAZINO || {};
YAZINO.socialFlow = YAZINO.socialFlow || {};
YAZINO.socialFlow.util = YAZINO.socialFlow.util || {};

(function ($) { // this is used by the tests and should be moved to a jquery util somewhere.
    $.fn.extend({
        contains: function (selector) {
            return this.find(selector).length > 0;
        }
    });
    YAZINO.socialFlow.util = {
        resetHeight: function () {
            var totalHeight = $('body').height()
                + parseInt($('body').css('padding-top'), 10)
                + parseInt($('body').css('padding-bottom'), 10);
            parent.YAZINO.lightboxWidget.adjustHeights(totalHeight);
        },
        redirect: function (url) {
            window.location.href = url;
        },
        browserPost: function (data, url) {
            var form = jQuery("<form/>")
                .attr("action", url || location.href)
                .attr("method", "POST");

            function createField(key, value) {
                form.append(jQuery("<input/>").attr({type: "hidden", name: key, value: value}));
            }

            jQuery.each(data, function (key, value) {
                if (typeof value === 'object') {
                    $.each(value, function () {
                        createField(key + '[]', this);
                    });
                } else {
                    createField(key, value);
                }
            });
            YAZINO.logger.log(form.html());
            form.appendTo($('body'));
            form.submit();
        }
    };
}(jQuery));

YAZINO.createInviteViaEmailService = function () {
    var service = {};

    YAZINO.EventDispatcher.apply(service);

    function dispatchSendingCompleteEvent(result) {
        if (result.invalid.length === 0 && result.already_registered.length === 0 && result.limit_exceeded.length === 0) {
            YAZINO.socialFlow.util.redirect("/invitation/sent");
            return;
        }
        result.eventType = "SendingInviteViaEmailFailed";
        service.dispatchEvent(result);
    }

    function aggregateResult(serverResult) {
        var result = {
            successful: serverResult.successful,
            invalid: [],
            already_registered: [],
            limit_exceeded: []
        };
        jQuery.each(serverResult.rejections, function () {
            if (this.resultCode === "ALREADY_REGISTERED") {
                result.already_registered.push(this.email);
            } else if (this.resultCode === "LIMIT_EXCEEDED") {
                result.limit_exceeded.push(this.email);
            } else {
                result.invalid.push(this.email);
            }
        });
        dispatchSendingCompleteEvent(result);
    }

    service.sendInvites = function (emailAddresses, requireAllValidToSend) {
        jQuery.ajax({
            url: "/invitation/inviteViaEmail",
            type: "POST",
            accepts: { json: "application/json" },
            dataType: "json",
            data: {
                source: "MFS",
                emails: emailAddresses,
                requireAllValidToSend: requireAllValidToSend
            }
        }).done(function (serverResult) {
            if (!serverResult.hasOwnProperty('successful')) {
                YAZINO.logger.warn('Invalid server response when verifying emails: %o', serverResult);
                return;
            }
            aggregateResult(serverResult);

        }).fail(function (error) {
            YAZINO.logger.warn('Unable to verify emails', error);
        });
    };

    return service;
};

(function ($) {
    $.fn.extend({
        personSelector: function (continueCallback, configIn) {
            var config = $.extend(true, {
                    copy: {
                        title: 'Select some people',
                        continueButton: 'Continue',
                        searchLabel: 'Search',
                        searchPlaceholder: 'Start typing a name',
                        peopleSelectedLabel: 'People Selected'
                    },
                    hooks: {
                        preDrawEvent: function (personList) {
                        }
                    },
                    pageSize: 700,
                    maxSelectable: null,
                    maxSendLimitHitAction: function () {
                        alert("You have reached the maximum number of people.");
                    }
                }, configIn || {}),
                elements = {
                    root: this,
                    header: $('<header/>').append($('<h1/>').text(config.copy.title)),
                    friendsSelectedHeading: $('<h3/>').text(config.copy.peopleSelectedLabel),
                    availablePeople: $('<div class="availablePeople personList"/>'),
                    selectedPeople: $('<div class="selectedPeople personList"/>'),
                    searchBar: $('<input/>'),
                    continueControl: $('<a href="#" class="continueControl control featureLink disabled"><span class="button">' + config.copy.continueButton + '</span></a>'),
                    nextPageButton: $('<a class="nextPageButton">Next</a>'),
                    previousPageButton: $('<a class="previousPageButton">Previous</a>')
                },

                peopleByIndex = [],
                isSubmittable = false,
                PERSON_INDEX_ATTRIBUTE_NAME = 'data-person-id',
                currentPage = 0,
                paginatedInclusionFilter = function (person, index) {
                    var bottom = currentPage * parseInt(config.pageSize, 10),
                        top = bottom + parseInt(config.pageSize, 10);
                    return parseInt(index, 10) >= bottom && parseInt(index, 10) < top;
                },
                defaultSort = function (a, b) {
                    return YAZINO.util.comparator.alphabetical.ascending(a.displayName, b.displayName);
                },
                defaultInclusionFilter = paginatedInclusionFilter,
                activeInclusionFilter = defaultInclusionFilter,
                activeSort = defaultSort,
                disabled = false;

            if (elements.root.length !== 1) {
                throw 'Requires exactly 1 element.';
            }

            continueCallback = continueCallback || function () {
            };

            function getSelectedPeople() {
                return elements.selectedPeople.find('.person');
            }

            function enablePersonSelector(element) {
                element.removeClass('disabled');
            }

            function enabledIf(element, isEnabled) {
                if (isEnabled) {
                    enablePersonSelector(element);
                } else {
                    element.addClass('disabled');
                }
            }

            function updateControls() {
                isSubmittable = getSelectedPeople().length > 0 && !disabled;
                enabledIf(elements.continueControl, isSubmittable);
            }

            function searchBarChanged() {
                var currentValue = elements.searchBar.val();
                if (searchBarChanged.previousValue === currentValue) {
                    return;
                }
                if ($.trim(currentValue).length === 0) {
                    elements.root.clearFilter();
                    return;
                }
                searchBarChanged.previousValue = currentValue;
                elements.root.filterByName(currentValue);
            }

            function getPersonFromList(personElement, list) {
                var personId = personElement.attr(PERSON_INDEX_ATTRIBUTE_NAME);
                return list.find('*[' + PERSON_INDEX_ATTRIBUTE_NAME + '="' + personId + '"]');
            }

            function deselectPerson(personElement) {
                getPersonFromList(personElement, elements.availablePeople).find('input').removeAttr('checked');
                personElement.remove();
            }

            function availablePeopleClickHandler(personElement) {
                var selectedPersonElement = getPersonFromList(personElement, elements.selectedPeople);
                if (personElement.hasClass('disabled')) {
                    return false;
                }
                function quotaIsFull() {
                    return config.maxSelectable !== null
                        && elements.selectedPeople.find('.person').length >= config.maxSelectable;
                }

                if (selectedPersonElement.length > 0) {
                    deselectPerson(selectedPersonElement);
                } else if (quotaIsFull()) {
                    config.maxSendLimitHitAction();
                } else {
                    personElement.find('input').attr('checked', 'checked');
                    personElement.clone().insertAfter(elements.friendsSelectedHeading);
                }
            }

            function generatePersonClickHandler(handler) {
                return function (e) {
                    var personElement = $(e.target).closest('.person');
                    handler(personElement);
                    updateControls();
                    return true;
                };
            }

            function throwIfPersonInvalid(person) {
                if (!person.displayName) {
                    throw 'invalid person: missing or empty displayName';
                }
            }

            function generatePersonElementFromPersonObject(personObject) {
                var checkBox = $("<input type='checkbox'/>"),
                    element = $('<div class="person"/>')
                        .attr(PERSON_INDEX_ATTRIBUTE_NAME, peopleByIndex.indexOf(personObject))
                        .text(personObject.displayName)
                        .prepend(checkBox);
                if (personObject.disabled) {
                    element.addClass('disabled');
                }
                if (getPersonFromList(element, elements.selectedPeople).length > 0) {
                    checkBox.attr("checked", "checked");
                }
                if (personObject.comment) {
                    element.append($('<p/>').addClass('comment').text(personObject.comment));
                }
                return element;
            }

            function rebuildView(availablePeopleModel) {
                elements.availablePeople.scrollTop(0).find(".person").remove();

                $.each(availablePeopleModel, function () {
                    generatePersonElementFromPersonObject(this).insertBefore(elements.nextPageButton);
                });

            }

            function isLastPage() {
                return currentPage >= (peopleByIndex.length / config.pageSize) - 1;
            }

            function resetPrevPageButton() {
                if (currentPage === 0) {
                    elements.previousPageButton.hide();
                } else {
                    elements.previousPageButton.show();
                }
            }

            function resetNextPageButton() {
                if (isLastPage()) {
                    elements.nextPageButton.hide();
                } else {
                    elements.nextPageButton.show();
                }
            }

            function resetButtons() {
                resetPrevPageButton();
                resetNextPageButton();
            }

            function modelChanged() {
                var i,
                    availablePeopleModel = [];
                for (i in peopleByIndex) {
                    if (peopleByIndex.hasOwnProperty(i) && activeInclusionFilter(peopleByIndex[i], i)) {
                        availablePeopleModel.push(peopleByIndex[i]);
                    }
                }
                config.hooks.preDrawEvent(availablePeopleModel);
                resetButtons();
                rebuildView(availablePeopleModel);
            }


            function nextPage() {
                if (!isLastPage()) {
                    currentPage += 1;
                    modelChanged();
                }
            }

            function prevPage() {
                if (currentPage > 0) {
                    currentPage -= 1;
                    modelChanged();
                }
            }

            elements.searchBar.attr('placeholder', config.copy.searchPlaceholder);

            elements.root
                .addClass('personSelector')
                .prepend(elements.header)
                .append($('<label/>').addClass('search')
                    .text(config.copy.searchLabel)
                    .append(elements.searchBar))
                .append($('<div/>').addClass('personLists')
                    .append(elements.availablePeople
                        .append(elements.previousPageButton)
                        .append(elements.nextPageButton))
                    .append(elements.selectedPeople
                        .append(elements.friendsSelectedHeading))
                    .append(elements.continueControl));

            elements.searchBar.blur(searchBarChanged);
            elements.searchBar.change(searchBarChanged);
            elements.searchBar.keyup(searchBarChanged);
            elements.availablePeople.click(generatePersonClickHandler(availablePeopleClickHandler));
            elements.selectedPeople.click(generatePersonClickHandler(deselectPerson));
            elements.nextPageButton.click(nextPage);
            elements.previousPageButton.click(prevPage);
            elements.continueControl.click(function () {
                if (isSubmittable && !disabled) {
                    var outputPeople = [];

                    getSelectedPeople().each(function () {
                        outputPeople.push(peopleByIndex[$(this).attr(PERSON_INDEX_ATTRIBUTE_NAME)]);
                    });

                    continueCallback(outputPeople);
                }
                return false;
            });
            resetButtons();

            $.extend(elements.root, {

                addPeople: function (people) {
                    $.each(people, function () {
                        var thisPersonIndex = peopleByIndex.length;
                        throwIfPersonInvalid(this);
                        peopleByIndex[thisPersonIndex] = this;
                    });
                    peopleByIndex.sort(activeSort);

                    modelChanged();
                },
                filterByName: function (name) {
                    activeInclusionFilter = function (person, id) {
                        var matched = true;
                        $.each(name.toLowerCase().split(' '), function () {
                            if (person.displayName.toLowerCase().indexOf(this) === -1) {
                                matched = false;
                            }
                        });
                        return matched;
                    };
                    modelChanged();
                },
                clearFilter: function () {
                    activeInclusionFilter = defaultInclusionFilter;
                    modelChanged();
                },
                setSort: function (sort) {
                    activeSort = sort;
                },
                disableSelector: function () {
                    disabled = true;
                    updateControls();
                },
                enableSelector: function () {
                    disabled = false;
                    updateControls();
                },

                modelChanged: modelChanged,
                rebuildView: rebuildView,
                nextPage: nextPage,
                prevPage: prevPage,
                getSelectedPeople: function () {
                    var outputPeople = [], selectedPeopleWidgets = elements.selectedPeople.find('.person');

                    selectedPeopleWidgets.each(function () {
                        outputPeople.push(peopleByIndex[$(this).attr(PERSON_INDEX_ATTRIBUTE_NAME)]);
                    });

                    return outputPeople;
                }
            });

            return elements.root;
        }
    });
}(jQuery));

(function () {
    function setUpPersonSelector(personProvider, personSelector, widget) {
        personProvider.addEventListener("PeopleLoaded", function (event) {
            personSelector.addPeople(event.friends);
        });
        personProvider.getPeople();
        return widget.append(personSelector);
    }

    function convertPersonArrayToIdArray(people) {
        var ids = [];
        jQuery.each(people, function () {
            ids.push(this.id);
        });
        return ids;
    }

    jQuery.fn.extend({
        challengeViaEmailWidget: /** @this {!Object} */function (inviteViaEmailService) {
            return this.each(
                function () {
                    var widget = jQuery(this),
                        invalidEmailKeyWidget = widget.find(".emailValidationErrors .invalidEmail"),
                        emailFields = jQuery(this).find("input[type='email'],input[type='text']"),
                        lastSentEmails = [];

                    widget.find("form.addresses").submit(function () {
                        var emails = [];
                        emailFields.each(function () {
                            var value = jQuery(this).val();
                            if (value.length > 0 && value !== jQuery(this).attr('placeholder')) {
                                emails.push(value);
                            }
                        });
                        if (emails.length === 0) {
                            emailFields.first().addClass("invalidEmail");
                            invalidEmailKeyWidget.show();
                        } else {
                            if (!(jQuery(emails).not(lastSentEmails).length === 0 && jQuery(lastSentEmails).not(emails).length === 0)) {
//                                inviteViaEmailService.sendInvites(emails, true);
                                lastSentEmails = emails;
                                return true;
                            }
                        }
                        return false;
                    });
                }
            );
        },
        inviteViaEmailWidget: /** @this {!Object} */function (inviteViaEmailService) {
            return this.each(
                function () {
                    var widget = jQuery(this),
                        invalidEmailKeyWidget = widget.find(".emailValidationErrors .invalidEmail"),
                        emailAlreadyRegisteredKeyWidget = widget.find(".emailValidationErrors .emailAlreadyRegistered"),
                        limitExceededWidget = widget.find(".emailValidationErrors .limitExceeded"),
                        emailFields = jQuery(this).find("input[type='email'],input[type='text']"),
                        lastSentEmails = [];

                    inviteViaEmailService.addEventListener("SendingInviteViaEmailFailed", function (event) {
                        invalidEmailKeyWidget.hide();
                        emailAlreadyRegisteredKeyWidget.hide();
                        limitExceededWidget.hide();
                        emailFields.each(function (index, el) {
                            var element = jQuery(el),
                                emailAddress = element.val();
                            element.removeClass("invalidEmail").removeClass("emailAlreadyRegistered").removeClass("limitExceeded");
                            if (event.invalid.indexOf(emailAddress) >= 0) {
                                element.addClass("invalidEmail");
                            }
                            if (event.already_registered.indexOf(emailAddress) >= 0) {
                                element.addClass("emailAlreadyRegistered");
                            }
                            if (event.limit_exceeded.indexOf(emailAddress) >= 0) {
                                element.addClass("limitExceeded");
                            }
                        });
                        if (event.invalid.length > 0) {
                            invalidEmailKeyWidget.show();
                        }
                        if (event.already_registered.length > 0) {
                            emailAlreadyRegisteredKeyWidget.show();
                        }
                        if (event.limit_exceeded.length > 0) {
                            limitExceededWidget.show();
                        }
                    });
                    widget.find("form.addresses").submit(function () {
                        var emails = [];
                        emailFields.each(function () {
                            var value = jQuery(this).val();
                            if (value.length > 0 && value !== jQuery(this).attr('placeholder')) {
                                emails.push(value);
                            }
                        });
                        if (emails.length === 0) {
                            emailFields.first().addClass("invalidEmail");
                            invalidEmailKeyWidget.show();
                        } else {
                            if (!(jQuery(emails).not(lastSentEmails).length === 0 && jQuery(lastSentEmails).not(emails).length === 0)) {
                                inviteViaEmailService.sendInvites(emails, true);
                                lastSentEmails = emails;
                            }
                        }
                        return false;
                    });
                }
            );
        },



        googleContactsInviteWidget: function (personProvider) {
            return this.each(function () {
                var widget = jQuery(this),
                    maxSelectable = personProvider.maxInvitations || null,
                    personSelector = jQuery('<div/>').personSelector(function (people) {
                        personProvider.invitePeople(convertPersonArrayToIdArray(people));
                    }, {
                        copy: {
                            title: 'Your Google Contacts',
                            continueButton: 'Send Invites',
                            searchLabel: 'Search Friends',
                            peopleSelectedLabel: 'Contacts Selected'
                        },

                        hooks: {
                            preDrawEvent: function (peopleToDisplay) {
                                var idsToCheck = [],
                                    peopleToCheck = [];

                                function updateStatusAndDisplay(alreadyRegisteredPlayers) {

                                    jQuery.each(peopleToCheck, function () {
                                        if (alreadyRegisteredPlayers.indexOf(this.id) > -1) {
                                            this.disabled = true;
                                            this.comment = "Already Registered";
                                        } else {
                                            this.disabled = false;
                                            this.comment = "";
                                        }
                                        this.hasBeenChecked = true;
                                    });
                                    personSelector.rebuildView(peopleToDisplay);
                                }

                                jQuery.each(peopleToDisplay, function () {
                                    if (!this.hasBeenChecked) {
                                        idsToCheck.push(this.id);
                                        peopleToCheck.push(this);
                                        this.disabled = true;
                                        this.comment = 'Loading';
                                    }
                                });

                                if (idsToCheck.length > 0) {
                                    personProvider.checkAlreadyRegistered(idsToCheck, updateStatusAndDisplay);
                                }
                            }
                        },
                        pageSize: YAZINO.configuration.get('socialFlow.batchSize'),
                        maxSelectable: maxSelectable,
                        maxSendLimitHitAction: function () {
                            alert("You can only send " + maxSelectable + " invites at a time! Invite more friends after sending these invites.");
                        }
                    });

                personSelector.setSort(function (a, b) {
                    return YAZINO.util.comparator.alphabetical.ascending(a.id.toLowerCase(), b.id.toLowerCase());
                });
                personProvider.addEventListener("PeopleLoaded", function (event) {
                    personSelector.addPeople(event.friends);
                    widget.html('').append(personSelector);
                    YAZINO.socialFlow.util.resetHeight();
                });

                widget.append(jQuery("<span/>").addClass("button").addClass("featureLink").addClass("load-contacts")
                    .append(jQuery("<span/>").text("Access Google Contacts").addClass("button")
                        .click(function () {
                            personProvider.getPeople();
                        })));

            });
        },
        googleChallengeWidget: function (personProvider) {
            return this.each(function () {
                var widget = jQuery(this),
                    maxSelectable = personProvider.maxInvitations || null,
                    callback = function (people) {
                        personProvider.challengePeople(convertPersonArrayToIdArray(people), function () {
                            YAZINO.socialFlow.util.redirect('/challenge');
                        });
                    },
                    personSelector = jQuery('<div/>').personSelector(callback, {
                        copy: {
                            title: 'Your Google Contacts',
                            continueButton: 'Send Challenge',
                            searchLabel: 'Search Contacts',
                            peopleSelectedLabel: 'Contacts Selected'
                        },
                        pageSize: YAZINO.configuration.get('socialFlow.batchSize'),
                        maxSelectable: maxSelectable,
                        maxSendLimitHitAction: function () {
                            alert("You can only send " + maxSelectable + " challenges at a time! Challenge more friends after sending these challenges.");
                        }
                    });

                widget.append(
                    jQuery("<span/>").addClass("button").addClass("featureLink").addClass("load-contacts")
                        .append(jQuery("<span/>").text("Access Google Contacts").addClass("button")
                            .click(function () {
                                personProvider.getPeople();

                            }))
                );

                personProvider.addEventListener("PeopleLoaded", function (event) {
                    personSelector.addPeople(event.friends);
                    widget.html('').append(personSelector);
                });

            });
        },

        facebookFriendsInviteWidget: function (personProvider) {
            return this.each(function () {
                var widget = jQuery(this),
                    maxSelectable = personProvider.maxInvitations || null,
                    personSelector = jQuery('<div/>').personSelector(function (people) {
                        personProvider.invitePeople(convertPersonArrayToIdArray(people));
                    }, {
                        copy: {
                            title: 'Your Facebook Friends',
                            continueButton: 'Send Invites',
                            searchLabel: 'Search Friends',
                            peopleSelectedLabel: 'Friends Selected'
                        },

                        hooks: {
                            preDrawEvent: function (peopleToDisplay) {
                                var idsToCheck = [],
                                    peopleToCheck = [];

                                function updateStatusAndDisplay(alreadyRegisteredPlayers) {

                                    jQuery.each(peopleToCheck, function () {
                                        if (alreadyRegisteredPlayers.indexOf(this.id) > -1) {
                                            this.disabled = true;
                                            this.comment = "Already Registered";
                                        } else {
                                            this.disabled = false;
                                            this.comment = "";
                                        }
                                        this.hasBeenChecked = true;
                                    });
                                    personSelector.rebuildView(peopleToDisplay);
                                }

                                jQuery.each(peopleToDisplay, function () {
                                    if (!this.hasBeenChecked) {
                                        idsToCheck.push(this.id);
                                        peopleToCheck.push(this);
                                        this.disabled = true;
                                        this.comment = 'Loading';
                                    }
                                });

                                if (idsToCheck.length > 0) {
                                    personProvider.checkAlreadyRegistered(idsToCheck, updateStatusAndDisplay);
                                }
                            }
                        },
                        pageSize: YAZINO.configuration.get('socialFlow.batchSize'),
                        maxSelectable: maxSelectable,
                        maxSendLimitHitAction: function () {
                            alert("You can only send " + maxSelectable + " invites at a time! Invite more friends after sending these invites.");
                        }
                    });
                return setUpPersonSelector(personProvider, personSelector, widget);
            });
        },
        facebookChallengeWidget: function (personProvider) {
            return this.each(function () {
                var widget = jQuery(this),
                    maxSelectable = personProvider.maxInvitations || null,
                    callback = function (people) {
                        personProvider.challengePeople(convertPersonArrayToIdArray(people), function () {
                            YAZINO.socialFlow.util.redirect('/challenge');
                        });
                    },
                    personSelector = jQuery('<div/>').personSelector(callback, {
                        copy: {
                            title: 'Your Facebook Friends',
                            continueButton: 'Send Challenge',
                            searchLabel: 'Search Friends',
                            peopleSelectedLabel: 'Friends Selected'
                        },
                        pageSize: YAZINO.configuration.get('socialFlow.batchSize'),
                        maxSelectable: maxSelectable,
                        maxSendLimitHitAction: function () {
                            alert("You can only send " + maxSelectable + " challenges at a time! Challenge more friends after sending these challenges.");
                        }
                    });

                return setUpPersonSelector(personProvider, personSelector, widget);
            });
        }
    });
}());

YAZINO.facebookFriendsService = function () {
    var service = new YAZINO.EventDispatcher();

    function sendUserToUserRequest(facebookIdArray, message, title, successCallback, failureCallback) {
        YAZINO.fb.userToUserRequest(
            facebookIdArray,
            message,
            title,
            function (response) {
                if (response) {
                    if (typeof successCallback === 'function') {
                        successCallback(response.to);
                    }
                } else {
                    if (typeof failureCallback === 'function') {
                        failureCallback();
                    }
                }
            }
        );
    }

    function reportInvitationsToServer(recipientIds) {
        YAZINO.socialFlow.util.browserPost({
            requestIds: recipientIds.join(','),
            source: "FACEBOOK_POPUP"
        }, '/invitation/facebook');
    }

    service.sendUserToUserRequest = sendUserToUserRequest;

    service.invitePeople = function (peopleToInvite, messageOverride, titleOverride, successCallbackOverride) {
        var message = messageOverride || YAZINO.configuration.get('socialFlow.invitation.gameSpecificInviteText'),
            title = titleOverride || YAZINO.configuration.get('socialFlow.invitation.defaultTitle'),
            successCallback = successCallbackOverride || reportInvitationsToServer;

        sendUserToUserRequest(
            peopleToInvite,
            message,
            title,
            successCallback
        );
    };

    service.challengePeople = function (peopleToChallenge, successCallback) {
        YAZINO.logger.log('sending challenge');
        YAZINO.logger.log(peopleToChallenge);
        sendUserToUserRequest(
            peopleToChallenge,
            YAZINO.configuration.get('socialFlow.challenge.message'),
            YAZINO.configuration.get('socialFlow.challenge.title'),
            successCallback
        );
    };

    service.maxInvitations = 50;

    service.getPeople = function () {
        YAZINO.fb.checkLoggedInAnd(
            function () {
                FB.api('/me/friends', function (response) {
                    var friendsArray = [];
                    if (!response || response.error) {
                        YAZINO.logger.error(response.error);
                    } else {
                        jQuery.each(response.data, function () {
                            var friend = {};
                            friend.id = this.id;
                            friend.displayName = this.name;
                            friendsArray.push(friend);
                        });
                        service.dispatchEvent({
                            eventType: "PeopleLoaded",
                            friends: friendsArray
                        });
                    }
                });
            }
        );
    };

    service.checkAlreadyRegistered = function (friendIds, successCallback) {

        function errorCallback(error) {
            YAZINO.logger.warn("Unable to verify registered facebook: %s", error);
        }

        jQuery.ajax({
            url: "/invitation/registered/facebook/",
            type: "GET",
            accepts: { json: "application/json" },
            dataType: "json",
            data: {
                ids: friendIds.join(",")
            }
        }).done(successCallback).fail(errorCallback);
    };
    return service;
};

YAZINO.googleContactsService = function (emailService) {
    var service = new YAZINO.EventDispatcher(), lastSentEmails = [],

        quietlySendEmailsAndForwardOntoSuccessPage = function (emailAddresses) {
            jQuery.ajax({
                url: "/invitation/asyncInviteViaEmail",
                type: "POST",
                accepts: { json: "application/json" },
                dataType: "json",
                data: {
                    source: "MFS",
                    emails: emailAddresses
                }
            }).done(function (serverResult) {
                if (!serverResult) {
                    YAZINO.logger.warn('Invalid server response when verifying emails: %o', serverResult);
                    return;
                }
                YAZINO.socialFlow.util.redirect("/invitation/sent");
            }).fail(function (error) {
                YAZINO.logger.warn('Unable to verify emails', error);
            });


        };

    service.addEmailServiceListener = function (eventToListenTo, callback) {
        emailService.addEventListener(eventToListenTo, callback);
    };

    service.invitePeople = function (peopleToInvite) {
        if (!(jQuery(peopleToInvite).not(lastSentEmails).length === 0 && jQuery(lastSentEmails).not(peopleToInvite).length === 0)) {
            quietlySendEmailsAndForwardOntoSuccessPage(peopleToInvite);
            lastSentEmails = peopleToInvite;
        }
    };

    service.challengePeople = function (peopleToChallenge) {
        if (!(jQuery(peopleToChallenge).not(lastSentEmails).length === 0 && jQuery(lastSentEmails).not(peopleToChallenge).length === 0)) {
            YAZINO.logger.log('sending challenge');
            YAZINO.logger.log(peopleToChallenge);
            YAZINO.socialFlow.util.browserPost({
                emails: peopleToChallenge
            }, "/challenge");
            lastSentEmails = peopleToChallenge;
        }
    };

    service.maxInvitations = 50;

    service.getPeople = function (failureAuthCallback) {
        YAZINO.googleApi.getContacts(service, failureAuthCallback || function () {});
    };


    service.checkAlreadyRegistered = function (friendIds, successCallback) {

        function errorCallback(error) {
            YAZINO.logger.warn("Unable to verify registered email:");
            YAZINO.logger.warn(error);
        }

        jQuery.ajax({
            url: "/invitation/registered/email/",
            type: "GET",
            accepts: { json: "application/json" },
            dataType: "json",
            data: {
                addresses: friendIds.join(",")
            }
        }).done(successCallback).fail(errorCallback);
    };

    return service;
};

(function ($) {
    $.fn.extend({
        buddiesChallengeWidget: function () {
            var widget, lastSentEmails = [];

            if ($(this).length === 0) {
                return;
            }
            if ($(this).length !== 1) {
                throw "one element required for initialisation.";
            }

            function completedCallback(data) {
                var nonFbIds = [];

                function sendResultsToServer() {
                    if (nonFbIds.length > 0) {
                        if (!(jQuery(nonFbIds).not(lastSentEmails).length === 0 && jQuery(lastSentEmails).not(nonFbIds).length === 0)) {
                            YAZINO.socialFlow.util.browserPost({
                                buddyIds: nonFbIds
                            }, '/challenge');
                            lastSentEmails = nonFbIds;
                        }
                    } else {
                        YAZINO.socialFlow.util.redirect('/challenge/sent');
                    }
                }

                $.each(data, function () {
                    nonFbIds.push(this.id);
                });

                sendResultsToServer();
            }

            widget = $(this).personSelector(completedCallback, {
                copy: {
                    title: 'Your Yazino Buddies',
                    continueButton: 'Send Challenge',
                    searchLabel: 'Search',
                    peopleSelectedLabel: 'Buddies Selected'
                }
            });

            function addPeopleFromIds(buddies) {
                var peopleToAdd = [], i;
                for (i = 0; i < buddies.length; i += 1) {
                    peopleToAdd.push({
                        displayName: buddies[i][1],
                        provider: "yazino",
                        id: parseInt(buddies[i][0], 10)
                    });
                }
                widget.addPeople(peopleToAdd);
            }

            $.ajax("/api/1.0/social/buddiesNames").done(function (result) {
                addPeopleFromIds(result.buddies);
            });

            return widget;
        }
    });
}(jQuery));

(function ($) {
    $.fn.extend({
        latchingButtonWidget: function () {
            $(this).click(function () {
                $(this).attr('disabled', 'disabled');
            });
        }
    });
}(jQuery));

(function ($) {
    $(document).ready(
        function () {
            var facebookFriendsService = YAZINO.facebookFriendsService(),
                emailService = YAZINO.createInviteViaEmailService(),
                googleContactsService = YAZINO.googleContactsService(emailService);

            if (window && window.ClientSdkSupport && typeof (window.ClientSdkSupport.provideFacebookFriendsService) === "function") {
                window.ClientSdkSupport.provideFacebookFriendsService(facebookFriendsService);
            } else {
                YAZINO.logger.warn("FacebookFriendsService cannot be integrated with (missing) Client SDK.");
            }

            $(".invitation.email .sendEmails").inviteViaEmailWidget(emailService);
            $(".challenge.email .sendEmails").challengeViaEmailWidget(emailService);


            $(".invitation.facebook .personSelector").facebookFriendsInviteWidget(facebookFriendsService);

            $(".invitation.gmail .personSelector").googleContactsInviteWidget(googleContactsService);

            $(".challenge.gmail .personSelector").googleChallengeWidget(googleContactsService);


            $(".challenge.buddies .personSelector").buddiesChallengeWidget(facebookFriendsService);

            $(".challenge.facebook .personSelector").facebookChallengeWidget(facebookFriendsService);
        }
    );
}(jQuery));


