/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

(function () {

    var realConfig;

    function generateMockElem(url, ctaContext) {
        return {
            attr: function (name) {
                switch (name) {
                case 'href':
                    return url;
                case 'data-ctaContext':
                    return ctaContext;
                default:
                    return;
                }
            },
            closest: function () {
                return generateMockElem();
            }
        };
    }

    beforeEach(function () {
        realConfig = YAZINO.configuration;
        YAZINO.configuration = YAZINO.configurationFactory(realConfig);
    });

    afterEach(function () {
        YAZINO.configuration = realConfig;
    });

    describe("Internal Url Mapping", function () {

        beforeEach(function () {
            spyOn(YAZINO.actions, 'buyChips');
        });

        it('should exist', function () {
            expect($('body').mapInternalUrls).toBeDefined();
            expect(YAZINO.util.internalUrlMapper).toBeDefined();
            expect(YAZINO.actions.buyChips).toBeDefined();
        });

        it('should start buy chips process', function () {
            YAZINO.util.internalUrlMapper.map(generateMockElem('yazino:buyChips'));

            expect(YAZINO.actions.buyChips).toHaveBeenCalled();
        });

        it('should start pass CTA reference when starting buy chips process', function () {
            YAZINO.util.internalUrlMapper.map(generateMockElem("yazino:buyChips", "testContext"));

            expect(YAZINO.actions.buyChips).toHaveBeenCalledWith('testContext');
        });

        it('should pick up ctaContext from parent', function () {
            var link = $('<a href="yazino:buyChips"/>').appendTo($('<div data-ctaContext="thiscontext"/>'));
            YAZINO.util.internalUrlMapper.map(link);

            expect(YAZINO.actions.buyChips).toHaveBeenCalledWith('thiscontext');
        });

        it('should fall back to default ctaContext', function () {
            var link = $('<a href="yazino:buyChips"/>').appendTo($('<div/>'));
            YAZINO.util.internalUrlMapper.map(link);

            expect(YAZINO.actions.buyChips).toHaveBeenCalledWith('site-url');
        });

        it('should not respond to non yazino: hrefs', function () {
            var returnValue = YAZINO.util.internalUrlMapper.map(generateMockElem("http://www.google.com/", "testContext"));

            expect(YAZINO.actions.buyChips).not.toHaveBeenCalled();
            expect(returnValue).toBeTruthy(); // allow default behaviour
        });

        it('should not respond to unknown yazino: hrefs', function () {
            var returnValue = YAZINO.util.internalUrlMapper.map(generateMockElem("yazino:unknownIdentifier"));

            expect(YAZINO.actions.buyChips).not.toHaveBeenCalled();
            expect(returnValue).toBeTruthy(); // allow default behaviour
        });


        it('should not break on links without hrefs', function () {
            var returnValue = YAZINO.util.internalUrlMapper.map(generateMockElem());

            expect(returnValue).toBeTruthy(); // allow default behaviour
        });

        it('should return itself (to allow chained commands)', function () {
            var rootElem = $('<div/>');

            expect(rootElem.mapInternalUrls()).toBe(rootElem);
        });

    });

    describe("Internal Url Mapping Actions", function () {
        it('should be able to open invite friends', function () {
            YAZINO.setupInvitations({addEventListener: function () {}, triggerPopup: function () {}, triggerPopupIfNotTriggeredRecently: function () {}});
            spyOn(YAZINO.actions, "inviteFriends");
            YAZINO.util.internalUrlMapper.map(generateMockElem("yazino:inviteFriends"));
            expect(YAZINO.actions.inviteFriends).toHaveBeenCalled();
        });

        it('should be able to open invite statement', function () {
            spyOn(YAZINO.actions, "invitationStatement").andCallThrough();
            spyOn(YAZINO.lightboxWidget, "createIframe");
            YAZINO.util.internalUrlMapper.map(generateMockElem("yazino:invitationStatement"));
            expect(YAZINO.actions.invitationStatement).toHaveBeenCalled();
            expect(YAZINO.lightboxWidget.createIframe).toHaveBeenCalledWith('/player/invitations');
        });

        it('should be able to open player profile', function () {
            spyOn(YAZINO.actions, "playerProfile").andCallThrough();
            spyOn(YAZINO.lightboxWidget, "createIframe");
            YAZINO.util.internalUrlMapper.map(generateMockElem("yazino:playerProfile"));
            expect(YAZINO.actions.playerProfile).toHaveBeenCalled();
            expect(YAZINO.lightboxWidget.createIframe).toHaveBeenCalledWith('/player/profile');
        });
    });
}());
