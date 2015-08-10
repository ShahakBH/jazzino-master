/*global gapi, $*/
var YAZINO = YAZINO || {};

YAZINO.createGoogleApi = function () {

    var clientIsLoaded = false,
        queuedCalls = [],
        googleApi,
        configforAuthorisation = {"client_id": YAZINO.configuration.get('googleApi.client_id'), scope: YAZINO.configuration.get('googleApi.scope')},
        apiKey = YAZINO.configuration.get('googleApi.apiKey');

    function authenticate(success) {
        var authParams;

        function doAuth() {

            googleApi.client.setApiKey(apiKey);
            YAZINO.logger.log("authing");
            googleApi.auth.authorize(configforAuthorisation, function () {
                YAZINO.logger.log("authed");
                authParams = googleApi.auth.getToken();
                success(authParams);
            });
        }

        if (clientIsLoaded) {
            YAZINO.logger.log("not queueing");
            doAuth();
        } else {
            YAZINO.logger.log("queueing");
            queuedCalls.push(doAuth);
        }
    }

    return {
        getContacts: function (service, failureCallback) {
            authenticate(function (authParams) {
                YAZINO.logger.log("gettingContacts");
                authParams.alt = 'json';
                authParams["max-results"] = 99999;
                $.ajax({
                    url: 'https://www.google.com/m8/feeds/contacts/default/full',
                    dataType: 'jsonp',
                    data: authParams,
                    success: function (data) {
                        var friendsArray = [];
                        $.each(data.feed.entry, function (index, entry) {

                            if (typeof entry.gd$email !== "undefined" && typeof entry.gd$email[0] !== "undefined") {
                                var friend = {},
                                    address = entry.gd$email[0].address,
                                    title = entry.title.$t;

                                friend.id = address;
                                if (typeof title !== "undefined" && title !== "") {
                                    friend.displayName = title + ": " + address;
                                } else {
                                    friend.displayName = address;
                                }
                                friendsArray.push(friend);
                            }
                        });
                        service.dispatchEvent({
                            eventType: "PeopleLoaded",
                            friends: friendsArray
                        });
                    }
                });
            });

        },
        hasLoaded: function () {
            gapi.auth.init(function () {
                var i = 0;
                googleApi = gapi;
                clientIsLoaded = true;

                for (i in queuedCalls) {
                    YAZINO.logger.log("checking queued:", queuedCalls[i]);
                    if (queuedCalls.hasOwnProperty(i) && typeof queuedCalls[i] === 'function') {
                        YAZINO.logger.log("execing queued:", queuedCalls[i]);
                        queuedCalls[i]();
                    }
                }
            });
        }
    };

};
YAZINO.googleApi = YAZINO.createGoogleApi();
