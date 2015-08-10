(function ($) {

    $().ready(function() {
        var browserWarningCookieName = "browserWarning";
        if ($.cookie(browserWarningCookieName) == null) {
            $('header.topBar').before('<div id="browser-notification"><p>You&rsquo;re using an unsupported browser. Some features may not work. <a href="/browser">Find out more.</a></p><button>Ignore</button>');
            $('#browser-notification button').click(function () {
                $.cookie(browserWarningCookieName, "warned", {expires: 30});
                $(this).closest('div').slideUp(200);
            });
            $('#browser-notification a').click(function () {
                $.cookie(browserWarningCookieName, "warned");
                return true;
            });
        }
    });

}(jQuery));
