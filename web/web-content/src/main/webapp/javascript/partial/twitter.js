/*global Math, window, encodeURIComponent, screen, YAZINO, alert, screenDimensions */
YAZINO.generateTwitterService = function (screenDimension) {
    var twitterBase = 'https://twitter.com',
        windowOptions = 'scrollbars=yes,resizable=yes,toolbar=no,location=yes',
        width = 550,
        height = 420,
        winHeight = screenDimension.height,
        winWidth = screenDimension.width,
        getSpecs = function (windowOptions, width, height, left, top) {
            return windowOptions + ',width=' + width +
                ',height=' + height + ',left=' + left + ',top=' + top;
        },
        getLeft = function (winWidth, width) {
            return Math.round((winWidth / 2) - (width / 2));
        },
        getTop = function (winHeight, height) {
            if (winHeight > height) {
                return Math.round((winHeight / 2) - (height / 2));
            } else {
                return 0;
            }
        },
        openTwitterWindow = function (urlToTwitter) {
            var left = getLeft(winWidth, width),
                top = getTop(winHeight, height),
                specs = getSpecs(windowOptions, width, height, left, top);
            window.open(urlToTwitter, 'twitterWindow', specs);
        },
        shareUrl = function (urlToShare) {
            return twitterBase + '/share?url=' + encodeURIComponent(urlToShare);
        },
        shareUrlAndOpenWindow = function (urlToShare) {
            openTwitterWindow(shareUrl(urlToShare));
        },
        shareUrlAndText = function (urlToShare, textToShare) {
            return twitterBase + '/share?url=' + encodeURIComponent(urlToShare) + '&text=' + encodeURIComponent(textToShare);
        },
        shareUrlAndTextAndOpenWindow = function (urlToShare, textToShare) {
            openTwitterWindow(shareUrlAndText(urlToShare, textToShare));
        },
        shareText = function (textToShare) {
            return shareUrlAndText('', textToShare);
        },
        shareTextAndOpenWindow = function (textToShare) {
            openTwitterWindow(shareText(textToShare));
        };

    return {
        shareUrl: shareUrl,
        shareUrlAndOpenWindow: shareUrlAndOpenWindow,
        shareText: shareText,
        shareTextAndOpenWindow: shareTextAndOpenWindow,
        shareUrlAndText: shareUrlAndText,
        shareUrlAndTextAndOpenWindow: shareUrlAndTextAndOpenWindow
    };
};

YAZINO.twitterService = YAZINO.generateTwitterService(window.screen);
