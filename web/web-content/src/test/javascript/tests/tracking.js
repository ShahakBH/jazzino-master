/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs, _gaq */

describe('Tracking util behaviours', function () {

    var testQue,
        testQueContainer,
        gaKey = 'my-google-analytics-key',
        fake_postJsonAsynchronously;

    beforeEach(function () {
        testQue = [];
        testQueContainer = {que: testQue};
        spyOn(YAZINO.util.ajax, 'postJsonAsynchronously');
    });

    afterEach(function () {
        YAZINO.businessIntelligence.reset();
    });

    it('Should add events to external instance of ga que', function () {
        var testEvent = ['tracker2._trackPageview', '/virtual/eventType/events/context/event'];

        YAZINO.businessIntelligence.setup.googleAnalytics(gaKey, testQueContainer, 'que');
        YAZINO.businessIntelligence.trackPlayerEvent('context', 'event', 'eventType');

        expect(testQue).toContain(testEvent);
    });

    it('Should stack up events fired before ga que is initialised', function () {
        var testEvent = ['tracker2._trackPageview', '/virtual/eventType/events/context/event'];

        YAZINO.businessIntelligence.trackPlayerEvent('context', 'event', 'eventType');
        YAZINO.businessIntelligence.setup.googleAnalytics(gaKey, testQueContainer, 'que');

        expect(testQue).toContain(testEvent);
    });

    it('Should stack up new ga que if none exists', function () {
        var testContext = {};

        YAZINO.businessIntelligence.setup.googleAnalytics(gaKey, testContext, 'que');

        expect(testContext.que).toBeDefined();
    });

    it('(End-To-End) Should sync with global-scope fake_gaq tracking que', function () {
        var newQue = [],
            testEvent = 'test-event';

        YAZINO.businessIntelligence.setup.googleAnalytics(gaKey, testQueContainer, 'que');
        expect(YAZINO.businessIntelligence.getGaQue()).toBe(testQue);
        YAZINO.businessIntelligence.gaTrack(testEvent);
        expect(testQue).toContain(testEvent);
        expect(testQue.length).toBe(3); // Should be setup event, page view and custom event
        testQueContainer.que = newQue;
        expect(YAZINO.businessIntelligence.getGaQue()).toBe(newQue);
        expect(YAZINO.businessIntelligence.getGaQue() === newQue).toBeTruthy();
        YAZINO.businessIntelligence.gaTrack(testEvent);
        expect(newQue).toContain(testEvent);
        expect(newQue.length).toBe(1);
    });

});


describe('Yazino Tracking behaviours', function () {
    var ajaxParams;

    beforeEach(function () {
        ajaxParams = {};
        spyOn(YAZINO.util.ajax, 'postJsonAsynchronously').andCallFake(function (url, data, successCallback, failureCallback) {
            ajaxParams = {
                url: url,
                data: data,
                successCallback: successCallback,
                failureCallback: failureCallback
            };
        });
    });

    it('should POST to tracking end-point reflecting event name', function () {
        var testEventName = 'eventName';
        YAZINO.businessIntelligence.yazinoTrack(testEventName);
        expect(ajaxParams.url).toEqual("/tracking/event?name=" + testEventName);
    });

    it('should POST with empty event properties when none specified', function () {
        var testEventName = 'eventName';
        YAZINO.businessIntelligence.yazinoTrack(testEventName);
        expect(ajaxParams.data).toEqual({ });
    });

    it('should POST with event properties when specified', function () {
        var testEventName = 'eventName',
            testEventProperties = { property1: 'one' };
        YAZINO.businessIntelligence.yazinoTrack(testEventName, testEventProperties);
        expect(ajaxParams.data).toEqual(testEventProperties);
    });

    it('should log successful tracking post at debug level', function () {
        var testEventName = 'eventName';
        spyOn(YAZINO.logger, 'debug');
        YAZINO.businessIntelligence.yazinoTrack(testEventName);
        expect(typeof ajaxParams.successCallback).toBe('function');
        ajaxParams.successCallback();
        expect(YAZINO.logger.debug).toHaveBeenCalledWith("Successfully tracked [" + testEventName + "] using internal tracking");
    });

    it('should log failed tracking post at error level', function () {
        var testEventName = 'eventName';
        spyOn(YAZINO.logger, 'error');
        YAZINO.businessIntelligence.yazinoTrack(testEventName);
        expect(typeof ajaxParams.successCallback).toBe('function');
        ajaxParams.failureCallback(500);
        expect(YAZINO.logger.error).toHaveBeenCalledWith("failed to track [" + testEventName + "] due to status code [500] using internal tracking");
    });

    it('should log 503 status at warn level', function () {
        var testEventName = 'eventName';
        spyOn(YAZINO.logger, 'warn');
        YAZINO.businessIntelligence.yazinoTrack(testEventName);
        expect(typeof ajaxParams.successCallback).toBe('function');
        ajaxParams.failureCallback(503);
        expect(YAZINO.logger.warn).toHaveBeenCalledWith("failed to track [" + testEventName + "] due to status code [503] using internal tracking");
    });

});



describe("legacy tracking solution", function () {

    var fake_gaq;

    beforeEach(function () {
        fake_gaq = [];
        spyOn(fake_gaq, 'push');
    });

    it("should track event", function () {
        YAZINO.businessIntelligence.setup.googleAnalytics('key', {"fake_gaq": fake_gaq}, 'fake_gaq');
        YAZINO.util.trackEvent("cat", "act", "lab", "val");
        expect(fake_gaq.push).toHaveBeenCalledWith(["_trackEvent", "cat", "act", "lab", "val"]);
    });

    it("should replace player name in optionalLable", function () {
        YAZINO.businessIntelligence.setup.googleAnalytics('key', {"fake_gaq": fake_gaq}, 'fake_gaq');
        spyOn(YAZINO.configuration, 'get').andReturn("bob");
        YAZINO.util.trackEvent("cat", "act", "then bob did something", "val");
        expect(fake_gaq.push).toHaveBeenCalledWith(["_trackEvent", "cat", "act", "then {*actor*} did something", "val"]);
        expect(YAZINO.configuration.get).toHaveBeenCalledWith('playerName');
    });

    it("should ignore if tracker is undefined", function () {
        YAZINO.businessIntelligence.setup.googleAnalytics('key', {"fake_gaq": fake_gaq}, 'wrong_name_fake_gaq');
        YAZINO.util.trackEvent("cat", "act", "lab", "val");
    });
});

describe('Tracking system', function () {

    var gaq,
        bi = YAZINO.businessIntelligence,
        purchaseTracker = bi.track.purchase,
        gaqContainer,
        gaKey = 'my-google-analytics-key';


    function setupTrackers() {
        bi.setup.googleAnalytics(gaKey, gaqContainer, '_gaq');
    }

    beforeEach(function () {
        gaq = [];
        spyOn(gaq, 'push');
        spyOn(YAZINO.util.ajax, 'postJsonAsynchronously');
        gaqContainer = {'_gaq': gaq};
    });

    afterEach(function () {
        bi.reset();
    });

    it('should return success when setting up GA with a key', function () {
        var gaKey = 'this-is-my-ga-key',
            returnValue;

        returnValue = bi.setup.googleAnalytics(gaKey, gaqContainer, '_gaq');

        expect(gaq.push).toHaveBeenCalledWith(['_setAccount', gaKey]);
        expect(returnValue).toBe(true);
    });

    it('should return failure when setting up GA without a key', function () {
        var gaKey = '',
            returnValue;

        returnValue = bi.setup.googleAnalytics(gaKey, {gaq: gaq}, 'gaq');

        expect(gaq.push).not.toHaveBeenCalled();
        expect(returnValue).toBe(false);
    });



    it('should set Google Tracker ID when given ID', function () {
        var gaKey = 'this-is-my-ga-key';

        bi.setup.googleAnalytics(gaKey, gaqContainer, '_gaq');

        expect(gaq.push).toHaveBeenCalledWith(['_setAccount', gaKey]);
        expect(gaq.push).toHaveBeenCalledWith(['_trackPageview']);
    });

    it('should set second Google Tracker ID when given ID', function () {
        var gaKey = 'this-is-my-second-ga-key';

        bi.setup.googleAnalytics(gaKey, gaqContainer, '_gaq');
        bi.setup.googleAnalyticsTracker2(gaKey);

        expect(gaq.push).toHaveBeenCalledWith(['tracker2._setAccount', gaKey]);
    });

    it('should return failure when setting up second Google Tracker without a key', function () {
        var gaKey = '',
            returnValue;

        bi.setup.googleAnalytics(gaKey, gaqContainer, '_gaq');
        returnValue = bi.setup.googleAnalyticsTracker2(gaKey);

        expect(gaq.push).not.toHaveBeenCalled();
        expect(returnValue).toBe(false);
    });

    it('should track started get chips process', function () {
        var ctaClicked = 'my-starting-point',
            ctaContext = 'my-context';
        setupTrackers();

        purchaseTracker.startedProcess(ctaClicked, ctaContext);

        expect(gaq.push).toHaveBeenCalledWith(["tracker2._trackPageview", "/virtual/purchases/events/lobby/buy-chips-clicked"]);
    });


    it('should track errors being displayed in get chips process', function () {
        setupTrackers();

        purchaseTracker.errorsDisplayed('testmethod');

        expect(gaq.push).toHaveBeenCalledWith(["tracker2._trackPageview", "/virtual/purchases/events/lobby/testmethod-errors-displayed"]);
    });

    it('should track errors details being displayed in get chips process', function () {
        setupTrackers();

        purchaseTracker.errorsDisplayed('testmethod', ['name', 'card-number']);

        expect(gaq.push).toHaveBeenCalledWith(["tracker2._trackPageview", "/virtual/purchases/events/lobby/testmethod-errors-displayed"]);
    });

    it('should send payment viewed event to kiss and google', function () {
        setupTrackers();

        YAZINO.businessIntelligence.track.purchase.viewedMethod('testpackage');

        expect(gaq.push).toHaveBeenCalledWith(['tracker2._trackPageview', '/virtual/purchases/events/lobby/testpackage-selected']);
    });

    it('should send payment option viewed event to google', function () {
        setupTrackers();

        purchaseTracker.viewedOption();

        expect(gaq.push).toHaveBeenCalledWith(["tracker2._trackPageview", "/virtual/purchases/events/lobby/payment-option-selected"]);
    });

    it('should send payment form submitted event to google', function () {
        setupTrackers();

        purchaseTracker.submittedForm('testmethod');

        expect(gaq.push).toHaveBeenCalledWith(["tracker2._trackPageview", "/virtual/purchases/events/lobby/testmethod-submit-button-clicked"]);
    });

    it('should send payment selected event to google', function () {
        setupTrackers();

        purchaseTracker.selectedMethod('testpackage');

        expect(gaq.push).toHaveBeenCalledWith(['tracker2._trackPageview', '/virtual/purchases/events/lobby/buy-chips-continue-button-clicked']);
    });

    it('should send payment success event to kiss', function () {
        setupTrackers();

        purchaseTracker.success('testpackage');

        expect(gaq.push).toHaveBeenCalledWith(['tracker2._trackPageview', '/virtual/purchases/events/lobby/testpackage-success-displayed']);
    });


});
