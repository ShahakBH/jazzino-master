/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Local Storage Test Instance', function () {

    it('should exist', function () {
        expect(window.localStorage).toBeDefined();
    });

    it('should allow setting and retrieving value', function () {
        window.localStorage.setItem('abc', 'def');
        expect(window.localStorage.getItem('abc')).toBe('def');
    });

    it('should allow removing item', function () {
        window.localStorage.setItem('abc', 'def');
        window.localStorage.removeItem('abc');
        expect(window.localStorage.getItem('abc')).toBeNull();
    });

    it('should allow clearing', function () {
        window.localStorage.setItem('abc', 'def');
        window.localStorage.clear();
        expect(window.localStorage.getItem('abc')).toBeNull();
    });

});

describe('Yazino Local Storage Instance Generator', function () {
    var globalVariableBackup = {};

    function safeSpyOn(fnName) {
        window.localStorage[fnName] = (window.localStorage && window.localStorage[fnName]) || function () {};
        spyOn(window.localStorage, fnName).andCallThrough();
    }

    it('should exist', function () {
        expect(YAZINO.getLocalStorageInstance).toBeDefined();
    });

    it('should return a namespaced localStorage API', function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');

        safeSpyOn('getItem');
        expect(myStorage.get).toBeDefined();
        expect(myStorage.set).toBeDefined();
        expect(myStorage.getItem).toBeDefined();
        expect(myStorage.setItem).toBeDefined();
        expect(myStorage.removeItem).toBeDefined();
        expect(myStorage.clear).toBeDefined();

        myStorage.getItem('abc');

        expect(window.localStorage.getItem).toHaveBeenCalledWith('yazino.myArea.abc');
    });

    it("should protect against empty keys (because of namespacing)", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');
        safeSpyOn('getItem');

        expect(function () {
            myStorage.getItem('');
        }).toThrow("no key given, can't namespace that!");

        expect(window.localStorage.getItem).not.toHaveBeenCalled();
    });

    it("should protect against empty namespaces", function () {
        var myStorage;
        expect(function () {
            myStorage = YAZINO.getLocalStorageInstance('');
        }).toThrow("no namespace given, can't work with that!");

        expect(myStorage).toBeUndefined();
    });

    it("should protect against duplicate namespaces", function () {
        var duplicateMyStorage,
            myStorage = YAZINO.getLocalStorageInstance('myArea3', true);

        expect(function () {
            duplicateMyStorage = YAZINO.getLocalStorageInstance('myArea3');
        }).toThrow("duplicate namespace given, can't work with that!");

        expect(duplicateMyStorage).toBeUndefined();
    });

    it("should store variable name in index", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');
        safeSpyOn('setItem');

        myStorage.set('abc', 'one');

        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.abc', 'one');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.[index]', 'abc');
    });

    it("should be able to retrieve value", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');

        myStorage.set('abc', 'one');
        expect(myStorage.get('abc')).toBe('one');
    });

    it("should store index of multiple keys", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');
        safeSpyOn('setItem');

        myStorage.set('abc', 'one');
        myStorage.set('def', 'two');

        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.abc', 'one');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.def', 'two');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.[index]', 'abc,def');
        expect(myStorage.get('[index]')).toBe('abc,def');
    });

    it("should remove index of removed keys", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');
        safeSpyOn('setItem');

        myStorage.set('abc', 'one');
        myStorage.set('def', 'two');
        myStorage.removeItem('abc');

        expect(myStorage.get('[index]')).toBe('def');
        expect(myStorage.get('abc')).toBe(null);
        expect(myStorage.get('def')).toBe('two');
    });

    it("should show that local storage is available", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea');
        expect(myStorage.isAvailable).toBeDefined();
    });

    it("should attempt to use local storage when testing availability", function () {
        var result,
            myStorage = YAZINO.getLocalStorageInstance('myArea');
        spyOn(myStorage, 'setItem').andCallThrough();
        spyOn(myStorage, 'getItem').andCallThrough();
        spyOn(myStorage, 'removeItem').andCallThrough();

        result = myStorage.isAvailable();

        expect(myStorage.setItem).toHaveBeenCalledWith('[tmp]', 'true');
        expect(myStorage.getItem).toHaveBeenCalledWith('[tmp]');
        expect(myStorage.removeItem).toHaveBeenCalledWith('[tmp]');
        expect(result).toBeTruthy();
    });

    it("should show as not available when local storage not working", function () {
        var result,
            myStorage = YAZINO.getLocalStorageInstance('myArea');
        spyOn(myStorage, 'setItem').andCallThrough();
        spyOn(myStorage, 'getItem').andReturn(undefined);
        spyOn(myStorage, 'removeItem').andCallThrough();

        result = myStorage.isAvailable();

        expect(myStorage.setItem).toHaveBeenCalledWith('[tmp]', 'true');
        expect(myStorage.getItem).toHaveBeenCalledWith('[tmp]');
        expect(myStorage.removeItem).toHaveBeenCalledWith('[tmp]');
        expect(result).toBeFalsy();
    });

    it("should allow namespaced index", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea'),
            someoneElsesStorage = YAZINO.getLocalStorageInstance('notMyArea');
        safeSpyOn('setItem');

        window.localStorage.removeItem('yazino.myArea.[index]');
        window.localStorage.removeItem('yazino.notMyArea.[index]');

        myStorage.set('abc', 'one');
        someoneElsesStorage.set('def', 'two');
        myStorage.setItem('ghi', 'three');

        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.abc', 'one');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.notMyArea.def', 'two');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.ghi', 'three');

        expect(window.localStorage.getItem('yazino.myArea.[index]')).toBe('abc,ghi');
        expect(window.localStorage.getItem('yazino.notMyArea.[index]')).toBe('def');

    });

    it("should allow namespaced clear", function () {
        var myStorage = YAZINO.getLocalStorageInstance('myArea'),
            notMyStorage = YAZINO.getLocalStorageInstance('notMyArea');

        myStorage.set('abc', 'one');
        notMyStorage.set('def', 'two');
        myStorage.setItem('ghi', 'three');

        safeSpyOn('setItem');
        safeSpyOn('getItem');
        safeSpyOn('removeItem');
        safeSpyOn('clear');

        myStorage.set('abc', 'one');
        notMyStorage.set('def', 'two');
        myStorage.setItem('ghi', 'three');

        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.abc', 'one');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.notMyArea.def', 'two');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('yazino.myArea.ghi', 'three');

        expect(window.localStorage.getItem('yazino.myArea.[index]')).toBe('abc,ghi');
        expect(window.localStorage.getItem('yazino.notMyArea.[index]')).toBe('def');

        myStorage.clear();

        expect(window.localStorage.removeItem).toHaveBeenCalledWith('yazino.myArea.abc');
        expect(window.localStorage.removeItem).not.toHaveBeenCalledWith('yazino.notMyArea.def');
        expect(window.localStorage.removeItem).toHaveBeenCalledWith('yazino.myArea.ghi');

        expect(window.localStorage.getItem('yazino.myArea.[index]')).toBeFalsy();
        expect(window.localStorage.getItem('yazino.notMyArea.[index]')).toBe('def');

        notMyStorage.clear();

        expect(window.localStorage.removeItem).toHaveBeenCalledWith('yazino.notMyArea.def');
        expect(window.localStorage.getItem('yazino.notMyArea.[index]')).toBeFalsy();

    });

});
