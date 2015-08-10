/*global FB, jQuery */
var YAZINO = YAZINO || {};

YAZINO.fb = (function (fbApi) {

    var service = new YAZINO.EventDispatcher();

    function convertArrayToCsv(array) {
        return array.join(',');
    }

    /*
     Verifies that the given list of friends are in fact facebook friends.
     Returns a promise that will be resolve with and array of verified friends, or rejected with facebook response.
     */
    function verifyFriends(candidateFriendIds) {
        var deferred = jQuery.Deferred();
        fbApi.api(
            "/me/friends",
            function (response) {
                if (response && !response.error) {
                    var verifiedFriends = [];
                    jQuery.each(response.data, function (index, value) {
                        if (jQuery.inArray(value.id, candidateFriendIds) !== -1) {
                            verifiedFriends.push(value.id);
                        }
                    });
                    YAZINO.logger.log('Verified friends: ' + verifiedFriends + 'from candidates: ' + candidateFriendIds);
                    deferred.resolve(verifiedFriends);
                } else {
                    YAZINO.logger.log("Failed to verify Facebook friends: " + response);
                    deferred.reject(response);
                }
            }
        );
        return deferred.promise();
    }

    function loginToFacebook(doSomething) {
        fbApi.login(function (response) {
            if (response.authResponse || response.status === 'connected') {
                doSomething();
            }
            // else not much we can do
        }, {scope: YAZINO.configuration.get('facebookPermissions')});
    }

    function checkLoggindInAnd(doSomething) {
        fbApi.getLoginStatus(function (response) {
            if (response.authResponse || response.status === 'connected') {
                doSomething();
            } else {
                YAZINO.logger.log("Response not authorised", response);
                loginToFacebook(doSomething);
            }
        });
    }

    function userToUserRequest(friendIds, inviteMessage, inviteTitle, requestCallback) {
        checkLoggindInAnd(function () {
            YAZINO.fb.verifyFriends(friendIds)
                .done(function (verifiedFriends) {
                    YAZINO.logger.log('Sending app request to friends: ' + verifiedFriends.toString());
                    if (verifiedFriends.length >= 1) {
                        fbApi.ui({
                            method: 'apprequests',
                            filters: "['app_non_users']",
                            to: convertArrayToCsv(friendIds),
                            display: 'iframe',
                            message: inviteMessage || YAZINO.configuration.get('facebook.invitation.defaultMessage'),
                            title: inviteTitle
                        }, function (data) {
                            YAZINO.logger.log('response from fb: ' + data);
                            if (requestCallback) {
                                requestCallback(data);
                            }
                        });
                    }
                });
        });
    }

    function purchaseCash(productUrl, requestId) {
        function logFailedPurchase(requestId, error_code, error_message) {
            YAZINO.logger.log("Logging failed transaction");
            jQuery.post(YAZINO.configuration.get('baseUrl') + "/payment/facebook/fail",
                {
                    gameType: YAZINO.configuration.get('gameType'),
                    productUrl: productUrl,
                    requestId: requestId,
                    errorCode: error_code,
                    errorMessage: error_message
                },
                function (result) {
                    YAZINO.logger.log("Facebook purchase failure logged on the server.");
                });
        }

        function getStatusFromFacebookErrorCode(errorCode) {
            if (errorCode === 1383010) {
                return 'cancelled'; // user cancelled
            } else {
                return 'failed'; // fb failure
            }
        }

        fbApi.ui(
            {
                method: 'pay',
                action: 'purchaseitem',
                product: productUrl,
                quantity: 1,
                request_id: requestId
            },
            function (data) {
                if (data) {
                    var result = {};
                    // errors are reported as {error_code: xxxx, error_message: message} i.e. no status property
                    if (data.hasOwnProperty('error_code')) {
                        logFailedPurchase(requestId, data.error_code, data.error_message);
                        result.status = getStatusFromFacebookErrorCode(data.error_code);
                        result.error = data;
                    } else {
                        result.status = data.status;
                    }
                    service.dispatchEvent({
                        eventType: "FacebookPaymentComplete",
                        result: result
                    });
                }
                YAZINO.logger.log(data);
            }
        );
    }

    function earnCash(productUrl, success) {
        fbApi.ui(
            {
                method: 'pay',
                action: 'earn_currency',
                product: productUrl
            },
            function (data) {
                success(data);
            }
        );
    }

    service.login = loginToFacebook;
    service.checkLoggedInAnd = checkLoggindInAnd;
    service.userToUserRequest = userToUserRequest;
    service.earn = earnCash;
    service.purchase = purchaseCash;
    service.verifyFriends = verifyFriends;

    return service;
}(FB));
