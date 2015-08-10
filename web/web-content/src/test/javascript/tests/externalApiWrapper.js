/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('External Api Wrapper', function () {

    var myApi = {
        pointA: function () {},
        pointB: function () {}
    };


    it('should exist', function () {
        expect(typeof YAZINO.generateApiWrapper).toBe('function');
    });

    it('should wrap all endpoints of an API', function () {
        var wrappedApi = YAZINO.generateApiWrapper(myApi);

        expect(typeof wrappedApi.pointA).toBe('function');
        expect(typeof wrappedApi.pointB).toBe('function');
    });

    it('should not contain the exact same functions', function () {
        var wrappedApi = YAZINO.generateApiWrapper(myApi);

        expect(wrappedApi.pointA).not.toBe(myApi.pointA);
        expect(wrappedApi.pointB).not.toBe(myApi.pointB);
    });

    it('should call through to original API functions', function () {
        spyOn(myApi, 'pointA');
        spyOn(myApi, 'pointB');
        var wrappedApi = YAZINO.generateApiWrapper(myApi);

        wrappedApi.pointA();
        wrappedApi.pointB();

        expect(myApi.pointA).toHaveBeenCalled();
        expect(myApi.pointB).toHaveBeenCalled();
    });

    it('should pass through arguments to original API functions', function () {
        spyOn(myApi, 'pointA');
        spyOn(myApi, 'pointB');
        var wrappedApi = YAZINO.generateApiWrapper(myApi),
            arg1 = 'abc',
            arg2 = 'def';

        wrappedApi.pointA(arg1, arg2);
        wrappedApi.pointB(arg2);

        expect(myApi.pointA).toHaveBeenCalledWith(arg1, arg2);
        expect(myApi.pointB).toHaveBeenCalledWith(arg2);
    });

    it('should allow setting up pre-agreed endpoints', function () {
        spyOn(myApi, 'pointA');
        var wrappedApi = YAZINO.generateApiWrapper(myApi, ['pointA']),
            arg1 = 'abc',
            arg2 = 'def';

        wrappedApi.pointA(arg1, arg2);

        expect(myApi.pointA).toHaveBeenCalledWith(arg1, arg2);
        expect(wrappedApi.pointB).toBeUndefined();
    });

    it('should allow intercepting arguments (e.g. logger sanitising) per function', function () {
        spyOn(myApi, 'pointA');
        spyOn(myApi, 'pointB');
        var arg1 = 'abc',
            arg2 = 'def',
            wrappedApi = YAZINO.generateApiWrapper(myApi, null, function (fnName, args) {
                var unwantedArgIndex = args.indexOf(arg1);
                if (fnName === 'pointA' && unwantedArgIndex > -1) {
                    args[unwantedArgIndex] = '!UNWANTED!';
                }
            });

        wrappedApi.pointA(arg1, arg2);
        wrappedApi.pointB(arg1);

        expect(myApi.pointA).toHaveBeenCalledWith('!UNWANTED!', arg2);
        expect(myApi.pointB).toHaveBeenCalledWith(arg1);
    });

    it('should not fail when originalApi is missing endpoint (for browser compaitibility)', function () {
        spyOn(myApi, 'pointA');
        spyOn(myApi, 'pointB');
        spyOn(YAZINO.logger, 'warn');
        var wrappedApi = YAZINO.generateApiWrapper(myApi, ['pointA', 'pointB', 'unsupportedEndpoint']);

        wrappedApi.pointA();
        wrappedApi.unsupportedEndpoint();
        wrappedApi.pointB();

        expect(myApi.pointA).toHaveBeenCalled();
        expect(myApi.pointB).toHaveBeenCalled();
        expect(YAZINO.logger.warn).toHaveBeenCalledWith("endpoint [%s] doesn't exist in underlying API", 'unsupportedEndpoint');
    });

    it("should not fail when originalApi is completely missing (like browsers that don't support console)", function () {
        spyOn(YAZINO.logger, 'warn');
        var myWindow = {},
            wrappedApi = YAZINO.generateApiWrapper(myWindow.console, ['pointA', 'pointB', 'unsupportedEndpoint']);

        wrappedApi.pointA();
        wrappedApi.unsupportedEndpoint();
        wrappedApi.pointB();

        expect(YAZINO.logger.warn).toHaveBeenCalledWith("endpoint [%s] doesn't exist in underlying API", 'pointA');
        expect(YAZINO.logger.warn).toHaveBeenCalledWith("endpoint [%s] doesn't exist in underlying API", 'pointB');
        expect(YAZINO.logger.warn).toHaveBeenCalledWith("endpoint [%s] doesn't exist in underlying API", 'unsupportedEndpoint');
    });

    it("should return value", function () {
        var myWindow = {
                pointA: function () {
                    return 'thisIsPointA';
                }
            },
            wrappedApi = YAZINO.generateApiWrapper(myWindow, ['pointA']);

        expect(wrappedApi.pointA()).toBe('thisIsPointA');
    });

});