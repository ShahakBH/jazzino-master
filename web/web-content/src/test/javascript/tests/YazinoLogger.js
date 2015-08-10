/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Yazino Logger', function () {

    function callLogWithSimpleValue(logLevel, value) {
        spyOn(console, logLevel);
        YAZINO.logger[logLevel](value);
        expect(console[logLevel]).toHaveBeenCalledWith(value);
    }

    it('Should be defined', function () {
        expect(YAZINO).toBeDefined();
        expect(YAZINO.logger).toBeDefined();
    });

    it('Should have defined standard console log functions', function () {
        expect(YAZINO.logger.log).toBeDefined();
        expect(YAZINO.logger.debug).toBeDefined();
        expect(YAZINO.logger.info).toBeDefined();
        expect(YAZINO.logger.warn).toBeDefined();
        expect(YAZINO.logger.error).toBeDefined();
        expect(YAZINO.logger.dir).toBeDefined();
    });

    it('Should invoke console.log for log level logging', function () {
        callLogWithSimpleValue('log', 'console log');
    });

    it('Should invoke console.debug for debug level logging', function () {
        callLogWithSimpleValue('debug', 'debug log');
    });

    it('Should invoke console.info for info level logging', function () {
        callLogWithSimpleValue('info', 'info log');
    });

    it('Should invoke console.warn for warn level logging', function () {
        callLogWithSimpleValue('warn', 'warn log');
    });

    it('Should invoke console.error for error level logging', function () {
        callLogWithSimpleValue('error', 'error log');
    });

    it('Should invoke console.dir for dir logging', function () {
        callLogWithSimpleValue('dir', 'console dir called');
    });

    it('Should pass on two arguments to system logger', function () {
        spyOn(console, 'log');
        YAZINO.logger.log('testing [%s] String replacement.', 'newString');
        expect(console.log).toHaveBeenCalledWith('testing [%s] String replacement.', 'newString');
    });

    it('Should pass on more than two arguments to system logger', function () {
        spyOn(console, 'log');
        YAZINO.logger.log('testing [%s] String replacement.', 'newString', 'lots more', 'arguments', 'these', 'can', 'theoretically', 'be', 'infinite');
        expect(console.log).toHaveBeenCalledWith('testing [%s] String replacement.', 'newString', 'lots more', 'arguments', 'these', 'can', 'theoretically', 'be', 'infinite');
    });

    it('Should convert undefined arguments to the string "<<undefined>>" (FF9 doesn\'t handle undefined values)', function () {
        var undefinedValue;
        spyOn(console, 'log');
        YAZINO.logger.log('testing [%s] String replacement.', 'newString', 'lots more', undefined, 'these', 'can', undefinedValue, 'be', undefined);
        expect(console.log).toHaveBeenCalledWith('testing [%s] String replacement.', 'newString', 'lots more', '<<undefined>>', 'these', 'can', '<<undefined>>', 'be', '<<undefined>>');
    });

    it('Should convert undefined message to the string "<<undefined>>" (to support legacy code)', function () {
        spyOn(console, 'log');
        YAZINO.logger.log(undefined);
        expect(console.log).toHaveBeenCalledWith('<<undefined>>');
    });

});
