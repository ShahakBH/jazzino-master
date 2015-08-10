/*global window, document, YAZINO */
(function (yazino) {

    var sdkSupport = {},
        invokeFlashFunction,
        unprocessedInvocations = [];

    function getSdk() {
        return document.getElementById("loader") || document.getElementById("slotsFlashLoader");
    }

    function createFlashMethodInvoker(methodName, argsTransform) {
        var transform = argsTransform || function (x) { return null; };
        return function (args) {
            invokeFlashFunction(methodName, transform(args));
        };
    }

    function isTwoWayBridgeCommunicationSupported() { // this is workaround for IE9/10
        var testFunction = getSdk().testExternalInterfaceInvocationsViaJavaScript;
        return typeof testFunction === 'function' && testFunction(); // for IE 9 & IE 10
    }

    function cacheInvocation(functionName, arg) {
        yazino.logger.info("invoking (via cache) " + functionName);
        if (arg) {
            unprocessedInvocations.push({ functionName: functionName, args: [ arg ] });
        } else {
            unprocessedInvocations.push({ functionName: functionName });
        }
    }

    function invokeImmediately(functionName, arg) {
        yazino.logger.info("invoking (immediately) " + functionName);
        var sdk = getSdk();

        if (!sdk) {
            return;
        }

        if (sdk[functionName]) {
            try {
                sdk[functionName].apply(null, [ arg ]);
            } catch (error) {
                yazino.logger.error("An error occurred when invoking " + functionName + " callback: " + error);
            }
        } else {
            yazino.logger.error("No callback registered for  " + functionName);
        }
    }

    function startCachingFlashInvocations() {
        yazino.logger.info("Entering Flash invocation caching mode");
        invokeFlashFunction = cacheInvocation;
    }

    function getUnprocessedInvocations() {
        yazino.logger.debug("Returning " + unprocessedInvocations.length + " invocations to Flash");
        var result = unprocessedInvocations;
        unprocessedInvocations = [];
        return result;
    }

    invokeFlashFunction = invokeImmediately;

    sdkSupport.provideFacebookFriendsService = function (facebookFriendsService) {
        sdkSupport.loadFacebookFriends = facebookFriendsService.getPeople;
        sdkSupport.sendFacebookUserToUserRequest = function (facebookUserIds, title, message) {
            facebookFriendsService.sendUserToUserRequest(facebookUserIds, message, title, createFlashMethodInvoker("onFacebookUserToUserRequestSuccess"), createFlashMethodInvoker("onFacebookUserToUserRequestFailure"));
        };
        sdkSupport.inviteFacebookFriendsViaUserToUserRequest = function (requestIds, title, message) {
            facebookFriendsService.invitePeople(requestIds, message, title, createFlashMethodInvoker("onFacebookFriendsInvitedViaUserToUserRequest"));
        };
        facebookFriendsService.addEventListener("PeopleLoaded", createFlashMethodInvoker("onFacebookFriendsLoaded", function (event) { return event.friends; }));
    };

    /**
     * See fb-payment.js
     * onFacebookPaymentStartError
     * facebook purchase starts with a call to {baseUrl}/api/1.0/payments/FACEBOOK_CANVAS/purchase/start. This returns the requestId
     * and product url. If this remote call fails we end with this callback.
     */
    sdkSupport.provideFacebookPaymentService = function (facebookPaymentService) {
        sdkSupport.purchaseFacebookProduct = facebookPaymentService.purchaseProduct;
        facebookPaymentService.addEventListener("FacebookPaymentStartError", createFlashMethodInvoker("onFacebookPaymentStartError", function (event) { return event.error; }));
        facebookPaymentService.addEventListener("FacebookPaymentComplete", createFlashMethodInvoker("onFacebookPaymentComplete", function (event) { return event.result; }));
    };

    /* methods exposed to Flash from Javascript */
    sdkSupport.isTwoWayBridgeCommunicationSupported = isTwoWayBridgeCommunicationSupported;
    sdkSupport.startCachingFlashInvocations = startCachingFlashInvocations;
    sdkSupport.getUnprocessedInvocations = getUnprocessedInvocations;
    sdkSupport.onGetChips = createFlashMethodInvoker("onGetChips");
    sdkSupport.onInviteFriends = createFlashMethodInvoker("onInviteFriends");
    sdkSupport.onBuddies = createFlashMethodInvoker("onBuddies");
    sdkSupport.onFreeGifts = createFlashMethodInvoker("onFreeGifts");
    sdkSupport.onGoMobile = createFlashMethodInvoker("onGoMobile");

    window.ClientSdkSupport = sdkSupport;

}(YAZINO));
