/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Call Out Box', function () {

    var rootElem,
        initializingElem,
        message,
        callOutBox,
        closeButton,
        mockedCurrentTime,
        localStorageIsAvailable;

    function hours() {
        return 60 * 60 * 1000;
    }

    function setupCallOutBox(time) {
        initializingElem.appendTo(rootElem);
        initializingElem.callOutBox(message, time);
        callOutBox = rootElem.find('.callOutBox');
        closeButton = callOutBox.find('.close');
    }

    window.localStorageMock.isAvailable = function () { return localStorageIsAvailable; };

    beforeEach(function () {
        rootElem = $('<div/>');
        initializingElem = $('<span/>');
        mockedCurrentTime = 1346228375051;
        message = 'this is my message';
        localStorageIsAvailable = true;
        spyOn(Date.prototype, 'getTime').andReturn(mockedCurrentTime); // to avoid time ticking while test is running
        spyOn(YAZINO, 'getLocalStorageInstance').andReturn(window.localStorageMock);
    });

    it('should exist', function () {
        expect(typeof $('body').callOutBox).toBe('function');
    });

    it('should return initialized element (this is the jQuery way)', function () {
        var attrValue = 'alaskdfjals',
            attrName = 'data-this-is-my-elem',
            elem = $('<div/>').attr(attrName, attrValue);
        expect(elem.callOutBox().attr(attrName)).toBe(attrValue);
    });

    it('should attach message after element initialized', function () {
        setupCallOutBox();

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.children().eq(1).hasClass('callOutBox')).toBeTruthy();
        expect(rootElem.children().eq(1).text()).toBe(message);
    });
    it('should allow HTML in message', function () {
        message = 'this is <em>my</em> message';
        setupCallOutBox();

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.children().eq(1).hasClass('callOutBox')).toBeTruthy();
        expect(rootElem.children().eq(1).contains('em')).toBeTruthy();
        expect(rootElem.children().eq(1).text()).toBe('this is my message');
    });

    it('should not attach if message is empty', function () {
        var rootElem = $('<div/>'),
            initializingElem = $('<span/>'),
            message = '';
        initializingElem.appendTo(rootElem);
        initializingElem.callOutBox(message);

        expect(rootElem.children().length).toBe(1);
        expect(rootElem.contains('.callOutBox')).toBeFalsy();
    });

    it('should contain close button', function () {
        setupCallOutBox();

        expect(callOutBox.contains('.close')).toBeTruthy();
        expect(closeButton.attr('alt')).toBe('Close');
    });

    it('should not contain close button when no local storage available', function () {
        localStorageIsAvailable = false;
        setupCallOutBox();

        expect(callOutBox.contains('.close')).toBeFalsy();
    });

    it('should be removed when close button clicked', function () {
        setupCallOutBox();

        expect(rootElem.contains('.callOutBox')).toBeTruthy();

        closeButton.click();

        expect(rootElem.contains('.callOutBox')).toBeFalsy();
    });

    it('should request a namespaced localStorage store', function () {
        setupCallOutBox();

        expect(YAZINO.getLocalStorageInstance).toHaveBeenCalledWith('callOutBox');
    });

    it('should check for message existance in the store', function () {
        spyOn(localStorageMock, 'getItem');
        setupCallOutBox();

        expect(localStorageMock.getItem).toHaveBeenCalledWith('this_is_my_message');
    });

    it('should not attach if message exists in the store', function () {
        spyOn(localStorageMock, 'getItem').andReturn(new Date().getTime());
        setupCallOutBox();

        expect(rootElem.children().length).toBe(1);
        expect(rootElem.contains('.callOutBox')).toBeFalsy();
    });

    it('should show message if never shown before', function () {
        spyOn(localStorageMock, 'getItem').andReturn(null);
        setupCallOutBox();

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.contains('.callOutBox')).toBeTruthy();
    });

    it('should save the time the message last cancelled (to allow time-sensitive logic)', function () {
        spyOn(localStorageMock, 'getItem').andReturn(null);
        spyOn(localStorageMock, 'setItem');
        setupCallOutBox();

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.contains('.callOutBox')).toBeTruthy();
        expect(localStorageMock.setItem).not.toHaveBeenCalled();

        closeButton.click();

        expect(localStorageMock.setItem).toHaveBeenCalledWith(message.split(' ').join('_'), mockedCurrentTime);
    });

    it('should not display message if hidden in last 24 hours', function () {
        spyOn(localStorageMock, 'getItem').andReturn(mockedCurrentTime - 23 * hours());
        spyOn(localStorageMock, 'setItem');
        setupCallOutBox();

        expect(rootElem.children().length).toBe(1);
        expect(rootElem.contains('.callOutBox')).toBeFalsy();
        expect(localStorageMock.setItem).not.toHaveBeenCalled();

    });

    it('should display message if hidden more than 24 hours ago', function () {
        var key = message.split(' ').join('_');
        spyOn(localStorageMock, 'getItem').andReturn(mockedCurrentTime - 25 * hours());
        spyOn(localStorageMock, 'removeItem');
        setupCallOutBox();

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.contains('.callOutBox')).toBeTruthy();
        expect(localStorageMock.removeItem).toHaveBeenCalledWith(key);

    });

    it('should be able to configure length of time to hide for', function () {
        var key = message.split(' ').join('_');
        spyOn(localStorageMock, 'getItem').andReturn(mockedCurrentTime - 3 * hours());
        spyOn(localStorageMock, 'removeItem');
        setupCallOutBox(2);

        expect(rootElem.children().length).toBe(2);
        expect(rootElem.contains('.callOutBox')).toBeTruthy();
        expect(localStorageMock.removeItem).toHaveBeenCalledWith(key);

    });

});
