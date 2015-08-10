(function ($) {
    $(document).ready(function () {
        $('.action-buddies-invite').click(window.ClientSdkSupport.onInviteFriends);
        $('.action-buddies-manage').click(window.ClientSdkSupport.onBuddies);
        $('.action-chips-get').click(window.ClientSdkSupport.onGetChips);
        $('.action-link-fanpage').click(function () {
            var gameType = YAZINO.configuration.get('gameType'),
                fanPage;
            if (gameType == 'BLACKJACK') {
                fanPage = 'YazinoBlackjack';
            } else {
                fanPage = 'YazinoWheelDeal';
            }
            window.open('https://www.facebook.com/' + fanPage, '_blank');
        });
        $('.action-gifts-get').click(window.ClientSdkSupport.onFreeGifts);
        $('.action-go-mobile').click(window.ClientSdkSupport.onGoMobile);
    });
})(jQuery);
