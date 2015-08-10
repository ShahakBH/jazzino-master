/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs, _gaq, ActiveXObject, XMLHttpRequest*/

describe('ajax util', function () {
    var ajaxUtil = YAZINO.util.ajax;
    describe('postJsonAsynchronously', function () {
        var mockRequest,
            defaultData = {
                prop1: 'value1',
                prop2: 'value2'
            },
            successCallback,
            failureCallback;

        function sendPost(url, data) {
            ajaxUtil.postJsonAsynchronously(url || '/somewhere', data, successCallback, failureCallback);
        }

        beforeEach(function () {
            mockRequest = {
                open: jasmine.createSpy('request-open'),
                send: jasmine.createSpy('request-send'),
                setRequestHeader: jasmine.createSpy('request-setRequestHeader')
            };
            spyOn(JSON, 'stringify').andCallThrough();
            spyOn(ajaxUtil, 'requestFactory').andReturn(mockRequest);
            successCallback = jasmine.createSpy('successCallback');
            failureCallback = jasmine.createSpy('failureCallback');
        });
        it('should exist', function () {
            expect(ajaxUtil.postJsonAsynchronously).toBeDefined();
        });
        it('should obtain request from request factory', function () {
            sendPost();
            expect(ajaxUtil.requestFactory).toHaveBeenCalled();
        });
        it('should open request', function () {
            var url = '/tmp';
            sendPost(url);
            expect(mockRequest.open).toHaveBeenCalledWith("POST", url, true);
        });
        it('should not open request if there\'s no request available', function () {
            ajaxUtil.requestFactory.andReturn(null);
            sendPost();
            expect(mockRequest.open).not.toHaveBeenCalled();
        });
        it('should log a warning if there\'s no request available', function () {
            ajaxUtil.requestFactory.andReturn(null);
            spyOn(YAZINO.logger, 'error');
            sendPost();
            expect(YAZINO.logger.error).toHaveBeenCalledWith("Unable to POST using native Ajax.");
        });
        it('should call send after open', function () {
            mockRequest.send.andCallFake(function () {
                expect(mockRequest.open).toHaveBeenCalled();
            });
            sendPost();
            expect(mockRequest.send).toHaveBeenCalled();
        });
        it('should send empty json when no data provided', function () {
            sendPost();
            expect(mockRequest.send).toHaveBeenCalledWith('{}');
        });
        it('should json serialise data when provided', function () {
            sendPost(null, defaultData);
            expect(JSON.stringify).toHaveBeenCalledWith(defaultData);
        });
        it('should send data as serialised json when data is provided', function () {
            var mockSerialisedData = 'abc';
            JSON.stringify.andReturn(mockSerialisedData);
            sendPost(null, defaultData);
            expect(mockRequest.send).toHaveBeenCalledWith(mockSerialisedData);
        });
        it('should wrap non-object data in serialised json', function () {
            var mockSerialisedData = '{data:"matWasHere"}',
                preSerialisedData = 'matWasHere';
            JSON.stringify.andReturn(mockSerialisedData);
            sendPost(null, preSerialisedData);
            expect(JSON.stringify).toHaveBeenCalledWith({data: preSerialisedData});
            expect(mockRequest.send).toHaveBeenCalledWith(mockSerialisedData);
        });
        it('should set content-type to json', function () {
            sendPost();
            expect(mockRequest.setRequestHeader).toHaveBeenCalledWith("Content-type", "application/json");
        });
        it('should register state change handler', function () {
            sendPost();
            expect(typeof mockRequest.onreadystatechange).toBe('function');
        });
        it('should call success handler on successful request completion', function () {
            sendPost();
            mockRequest.readyState = 4;
            mockRequest.status = 200;
            mockRequest.onreadystatechange();
            expect(successCallback).toHaveBeenCalled();
            expect(failureCallback).not.toHaveBeenCalled();
        });
        it('should not call handlers on incomplete request', function () {
            sendPost();
            $.each([1, 2, 3], function (key, value) {
                mockRequest.readyState = value;
                mockRequest.onreadystatechange();
                expect(successCallback).not.toHaveBeenCalled();
                expect(failureCallback).not.toHaveBeenCalled();
            });
        });
        it('should call failure handler on failed request completion', function () {
            sendPost();
            mockRequest.readyState = 4;
            mockRequest.status = 500;
            mockRequest.onreadystatechange();
            expect(successCallback).not.toHaveBeenCalled();
            expect(failureCallback).toHaveBeenCalledWith(500);
        });
        it('support a range of success and failure status codes', function () {
            sendPost();
            mockRequest.readyState = 4;
            $.each([100, 200, 201, 202, 204, 0 /* returned by some browsers on success */], function (key, value) {
                successCallback.reset();
                failureCallback.reset();
                mockRequest.status = value;
                mockRequest.onreadystatechange();
                expect(successCallback).toHaveBeenCalled();
                expect(failureCallback).not.toHaveBeenCalled();
            });
            $.each([300, 400, 404, 500, 503, 504], function (key, value) {
                successCallback.reset();
                failureCallback.reset();
                mockRequest.status = value;
                mockRequest.onreadystatechange();
                expect(successCallback).not.toHaveBeenCalled();
                expect(failureCallback).toHaveBeenCalledWith(value);
            });
        });
    });

    describe('requestFactory', function () {
        var toCleanupInGlobalScope = {},
            nativeAjaxImplementations = ['ActiveXObject', 'XMLHttpRequest'];
        beforeEach(function () {
            $.each(nativeAjaxImplementations, function () {
                toCleanupInGlobalScope[this] = window[this];
                window[this] = undefined;
            });
        });
        afterEach(function () {
            $.each(toCleanupInGlobalScope, function (key, value) {
                window[key] = value;
            });
        });
        it('should exist', function () {
            expect(ajaxUtil.requestFactory).toBeDefined();
        });
        it('should return microsoft implementation where available', function () {
            window.ActiveXObject = function () {};
            expect(ajaxUtil.requestFactory()).toMatch(jasmine.any(ActiveXObject));
        });
        it('should return standard implementation where microsoft implementation is not available and standard implementation is available', function () {
            window.XMLHttpRequest = function () {};
            expect(ajaxUtil.requestFactory()).toMatch(jasmine.any(XMLHttpRequest));
        });
        it('should return null where microsoft implementation is not available and standard implementation is not available', function () {
            expect(ajaxUtil.requestFactory()).toBe(null);
        });
        it('should instantiate microsoft implementation with appropriate param', function () {
            window.ActiveXObject = function () {};
            spyOn(window, 'ActiveXObject');
            ajaxUtil.requestFactory();
            expect(ActiveXObject).toHaveBeenCalledWith("Microsoft.XMLHTTP");
        });
    });
});
