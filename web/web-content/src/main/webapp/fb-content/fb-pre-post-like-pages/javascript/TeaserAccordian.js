if (typeof YAZINO === 'undefined') {
    var YAZINO = {};
}

if (typeof console === 'undefined') {
    var console = {};
    console.log = function() {
    };
}

YAZINO.TeaserAccordian = (function () {

    var baseElem,
        $ = jQuery,
        ORIGINAL_HEIGHT_ATTR_NAME = 'data-original-height',
        ORIGINAL_PADDING_TOP_ATTR_NAME = 'data-original-padding-top',
        ORIGINAL_PADDING_BOTTOM_ATTR_NAME = 'data-original-padding-bottom',
        CLOSED_CLASS_NAME = 'closedItem',
        ANIMATING_CLASS_NAME = 'transitioningItem',
        ACCORDIAN_MODE = true,
        DEFAULT_ANIMATION_TIME = 500;

    function init(baseElemToInitiate) {
        var items;
        baseElem = $(baseElemToInitiate);
        items = baseElem.find('.contentToggler').click(togglerHandler).css({cursor: 'pointer'});
        console.log('initiated with ' + items.length + ' items');
        closeAllOpenItems(true);
    }

    function closeAllOpenItems(immediate) {
        baseElem.find('.teaserItem').not('.closedItem').each(function() {
            closeItem($(this), immediate);
        });
    }

    function togglerHandler() {
        toggleItem($(this).closest('.teaserItem'));
    }

    function toggleItem(itemToToggle) {
        if (itemToToggle.hasClass(CLOSED_CLASS_NAME)) {
            openItem(itemToToggle);
        } else {
            closeItem(itemToToggle);
        }
    }

    function openItem(itemToOpen, immediate) {
        var elemToShow = itemToOpen.find('.contentExtra');
        if (ACCORDIAN_MODE) {
            closeAllOpenItems();
        }
        if (elemToShow.length === 0) {
            return false;
        }
        showElem(elemToShow, immediate ? 0 : DEFAULT_ANIMATION_TIME);
        itemToOpen.removeClass(CLOSED_CLASS_NAME);
    }

    function closeItem(itemToClose, immediate) {
        var elemToClose = itemToClose.find('.contentExtra');
        archiveCurrentRenderedSizes(elemToClose);
        if (elemToClose.length === 0) {
            return false;
        }
        hideElem(elemToClose, immediate ? 0 : DEFAULT_ANIMATION_TIME);
        itemToClose.addClass(CLOSED_CLASS_NAME);
    }

    function showElem(elemToShow, animationTime) {
        var elemOriginalHeight = retrieveArchivedHeight(elemToShow);
        if (elemOriginalHeight) {
            console.log('opening');
            elemToShow.css({
                display: 'block'
            }).addClass(ANIMATING_CLASS_NAME).animate({
                    height: elemOriginalHeight,
                    paddingTop: retrieveArchivedTop(elemToShow),
                    paddingBottom: retrieveArchivedBottom(elemToShow)
                }, animationTime, function() {
                    console.log('open');
                    $(this).css({height:'auto'}).removeClass(ANIMATING_CLASS_NAME);
                });
        } else {
            elemtoShow.show();
        }
    }

    function hideElem(elemToHide, animationTime) {
        elemToHide.addClass(ANIMATING_CLASS_NAME).animate({
            height: 0,
            paddingTop: 0,
            paddingBottom: 0
        }, animationTime, function() {
            $(this).css({display: 'none'}).removeClass(ANIMATING_CLASS_NAME);
        });
    }

    function archiveCurrentRenderedSizes(elem) {
        elem.attr(ORIGINAL_HEIGHT_ATTR_NAME, elem.height() + 'px');
        elem.attr(ORIGINAL_PADDING_TOP_ATTR_NAME, elem.css('padding-top'));
        elem.attr(ORIGINAL_PADDING_BOTTOM_ATTR_NAME, elem.css('padding-bottom'));
    }

    function retrieveArchivedHeight(elem) {
        return elem.attr(ORIGINAL_HEIGHT_ATTR_NAME);
    }

    function retrieveArchivedTop(elem) {
        return elem.attr(ORIGINAL_PADDING_TOP_ATTR_NAME);
    }

    function retrieveArchivedBottom(elem) {
        return elem.attr(ORIGINAL_PADDING_BOTTOM_ATTR_NAME);
    }

    return init;

})();