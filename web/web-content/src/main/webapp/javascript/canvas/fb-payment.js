/*global document, jQuery */

var YAZINO = YAZINO || {};

YAZINO.facebookPaymentService = function () {

    var service = new YAZINO.EventDispatcher();

    function hideGameWhileFBPopupIsOpen() {
        jQuery('.game object').addClass('hideObject').removeClass('showObject');
    }

    function showGame() {
        var gameSwfObject = jQuery('.game object');
        gameSwfObject.addClass('showObject').removeClass('hideObject');
        if (/*@cc_on!@*/0) { // handle IE
            var initialWidth = gameSwfObject.css("width");
            gameSwfObject.css("width", "936px");
            setTimeout(function() {
                jQuery('.game object').css("width", initialWidth);
            }, 500);
        }
    }

    function purchaseProduct(productId) {
        YAZINO.logger.log("requesting productUrl and requestId for productId: " + productId);
        YAZINO.logger.log("gameType: " + YAZINO.configuration.get('gameType'));

        hideGameWhileFBPopupIsOpen();
        jQuery
            .post(YAZINO.configuration.get('baseUrl') + "/api/1.0/payments/FACEBOOK_CANVAS/purchase/start", {
                gameType: YAZINO.configuration.get('gameType'),
                productId: productId
            })
            .done(function (result, textStatus, jqXHR) {
                YAZINO.logger.log("attempting facebook purchase. productUrl=" + result.productUrl + ", requestId=" + result.requestId);
                YAZINO.fb.purchase(result.productUrl, result.requestId);
                // etc.
                // how to return feedback to sdk?
                YAZINO.logger.log("Facebook purchase cancellation logged on the server.");
            })
            .fail(function (jqXHR, textStatus, errorThrown) {
                YAZINO.logger.log("Failed to start fb purchase. "
                    + "status=" + jqXHR.status
                    + ", responseText="
                    + jqXHR.responseText
                    + ", textStatus=" + textStatus
                    + ", errorThrown=" + errorThrown);
                service.dispatchEvent({
                    eventType: "FacebookPaymentStartError",
                    error: {httpStatus: jqXHR.status, response: jqXHR.responseText}
                });
                showGame();
            });
    }

    // forwarding event from purchaseCash (in fb.js), since fb is not exposed
    YAZINO.fb.addEventListener("FacebookPaymentComplete", function (event) {
        service.dispatchEvent(event);
        showGame();
    });

    service.purchaseProduct = purchaseProduct;

    return service;
};

(function ($) {
    $(document).ready(function () {
        var facebookPaymentService = YAZINO.facebookPaymentService();

        window.ClientSdkSupport.provideFacebookPaymentService(facebookPaymentService);
    });
}(jQuery));
