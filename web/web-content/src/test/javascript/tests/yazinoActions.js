/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('yazino actions', function () {

    var assume = expect,
        fakeConfig,
        actionContainer,
        externalActionsMap;

    beforeEach(function () {
        fakeConfig = YAZINO.configurationFactory({
            gameType: 'SLOTS'
        });
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);
        externalActionsMap = {};
        actionContainer = YAZINO.generateActionContainer(externalActionsMap);
    });

    it('should surface created actions in externalActionsMap - legacy functionality to be removed', function () {
        assume(externalActionsMap.myAction).not.toBeDefined();

        actionContainer.create('myAction', function () {});

        expect(externalActionsMap.myAction).toBeDefined();
    });

    it('should surface created actions in externalActionsMap', function () {
        var myFunc = jasmine.createSpy('myFunc');
        assume(externalActionsMap.myAction).not.toBeDefined();
        actionContainer.create('myAction', myFunc);
        assume(externalActionsMap.myAction).toBeDefined();

        expect(myFunc).not.toHaveBeenCalled();
        actionContainer.run('myAction', 'ctaRef');
        expect(myFunc).toHaveBeenCalled();
    });

    it('should send through cta ref and game type for tracking', function () {
        var myFunc = jasmine.createSpy('myFunc');
        spyOn(YAZINO.businessIntelligence.track, 'yazinoAction');
        actionContainer.create('myAction', myFunc);

        actionContainer.run('myAction', 'ctaRef');

        expect(YAZINO.configuration.get).toHaveBeenCalledWith('gameType');
        expect(YAZINO.businessIntelligence.track.yazinoAction).toHaveBeenCalledWith('myAction', 'ctaRef', 'SLOTS');
        expect(myFunc).toHaveBeenCalledWith();
    });

    it('should be able to use action without providing external map', function () {
        actionContainer = YAZINO.generateActionContainer();
        var myFunc = jasmine.createSpy('myFunc');
        actionContainer.create('myAction', myFunc);

        actionContainer.run('myAction', 'ctaRef');

        expect(myFunc).toHaveBeenCalledWith();
    });

    it('should have be able to lookup function existence', function () {
        assume(actionContainer.contains('myAction')).toBeFalsy();
        actionContainer.create('myAction', function () {});

        expect(actionContainer.contains('myAction')).toBeTruthy();
    });

});