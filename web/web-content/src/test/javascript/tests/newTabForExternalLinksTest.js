/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('New Tab For External Links', function () {
    var exampleDom;

    function expectLinkToOpenInSameTab(cssClass) {
        var elem = exampleDom.find('.' + cssClass);
        expect(elem.attr('target')).toBeUndefined();
    }

    function expectLinkToOpenInNewTab(cssClass) {
        var elem = exampleDom.find('.' + cssClass);
        expect(elem.attr('target')).toBe('_blank');
    }

    beforeEach(function () {
        exampleDom = $('<div/>')
            .append($('<a href="internal" class="int1">Internal</a>'))
            .append($('<a href="/internal" class="int2">Internal</a>'))
            .append($('<a href="http://www.yazino.com/internal" class="int3">Internal</a>'))
            .append($('<a href="http://facebook.com/yazino" class="ext1">External</a>'))
            .append($('<a href="https://google.com/?q=www.yazino.com" class="ext2">External</a>'));
        YAZINO.configuration.set('baseUrl', 'http://www.yazino.com:80');
    });
    it('should exist', function () {
        expect($('body').newTabForExternalLinks).toBeDefined();
    });
    it('should set target on external http links', function () {
        exampleDom.newTabForExternalLinks();
        expectLinkToOpenInNewTab('ext1');
    });
    it('should set target on external https links', function () {
        exampleDom.newTabForExternalLinks();
        expectLinkToOpenInNewTab('ext2');
    });
    it('should not set target on relative links', function () {
        exampleDom.newTabForExternalLinks();
        expectLinkToOpenInSameTab('int1');
    });
    it('should not set target on full URI internal links', function () {
        exampleDom.newTabForExternalLinks();
        expectLinkToOpenInSameTab('int2');
    });
    it('should not set target on fully qualified internal links', function () {
        exampleDom.newTabForExternalLinks();
        expectLinkToOpenInSameTab('int3');
    });
});
