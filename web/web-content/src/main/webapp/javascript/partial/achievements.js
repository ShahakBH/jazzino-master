/*global alert, clearInterval, clearTimeout, document, FB, jQuery, setInterval, setTimeout, window, YAZINO, StrataPartnerApi */
(function ($) {
    var insertionPoint,
        container;

    jQuery.fn.extend({
        achievementsContainer: function () {
            jQuery().achievementsContainerSetUp(jQuery(this));

            jQuery('.show-achievements-overlay').click(function () {
                jQuery().achievementsDisplay();
            });
        },
        achievementsContainerSetUp: function (parent) {
            YAZINO.logger.debug("Achievements Container setup starting");
            container = jQuery('<div id="achievements-container" class="masked-container" style="display: none;"><div class="container-border"><div class="container-content"></div>'
                    + '<div class="container-controls"><a class="close-button" href="#" title="Close the Achievements Window">Close</a></div></div></div>');
            insertionPoint = container.find(".container-content");
            parent.append(container);
            YAZINO.logger.debug("Achievements Container setup complete");
        },
        achievementsDisplay: function () {
            var content, close, keyHandler;

            content = jQuery('<iframe frameborder="no" scrolling="no" src="' + YAZINO.configuration.get('baseUrl') + '/achievements?gameType=' + YAZINO.configuration.get('gameType') + '"></iframe>');
            insertionPoint.append(content);

            keyHandler = function (e) {
                if (e.which === 27 || e.which === 13) {
                    if (e.which === 13) {
                        e.preventDefault();
                    }
                    close();
                }
            };
            close = function () {
                container.fadeOut(200);
                insertionPoint.empty();
                jQuery(document).unbind('keydown', keyHandler);
            };

            container.find(".close-button").click(close);
            jQuery(document).keydown(keyHandler);

            container.fadeIn(200);
        },
        achievementsWidget: function () {
            return this.each(function () {
                var widget = jQuery(this);

                widget.find("h1").click(function () {
                    var clickedHeader = jQuery(this);
                    widget.find("h1").each(function () {
                        jQuery(this).siblings("div").fadeOut(100);
                        jQuery(this).removeClass("selected");
                    });
                    setTimeout(function () {
                        clickedHeader.addClass("selected");
                        clickedHeader.siblings("div").fadeIn(100);
                    }, 100);
                });

                widget.find('li.achievements a').click(function () {
                    var element = jQuery(this);
                    StrataPartnerApi.postToFacebook({
                        title: element.attr('data-achievement-title'),
                        image: element.attr('data-achievement-id'),
                        message: element.attr('data-achievement-message'),
                        gameType: element.attr('data-achievement-gametype'),
                        popup : true
                    });
                });

                widget.find('li.trophy-cabinet a').click(function () {
                    var element = jQuery(this);
                    StrataPartnerApi.postToFacebook({
                        image: element.attr('data-trophy-image'),
                        message: element.attr('data-trophy-message'),
                        gameType: element.attr('data-trophy-gametype'),
                        popup : true
                    });
                });
            });
        }
    });
}(jQuery));
