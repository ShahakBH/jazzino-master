/*global document, jQuery */

var YAZINO = YAZINO || {};

YAZINO.invitationService = function () {
    var isFacebookLogin = YAZINO.configuration.get('facebookConnect') && YAZINO.configuration.get('facebookCanvasActionsAllowed');
};

jQuery.fn.extend({
    pageSelectorWidget:/** @this {!Object} */function (createInviteFriendsService) {
        return this.each(
            function () {
                jQuery(".email").hide();
            }
        );
    }
});

(function ($) {
    $(document).ready(function () {
        var invitationService = YAZINO.invitationService();
        $(".pageSelectorWidget").pageSelectorWidget(invitationService);
        //paymentService.init();
    });
}(jQuery));
