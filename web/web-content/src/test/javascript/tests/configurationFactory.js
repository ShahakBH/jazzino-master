/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('configuration factory', function () {

    it('should exist', function () {
        expect(typeof YAZINO.configurationFactory).toBe('function');
    });

    it('should expose properties passed into constructor', function () {
        var configuration = YAZINO.configurationFactory({
            property1: "value1"
        });

        expect(configuration.property1).toBe('value1');
    });

    it('should make properties available via get function', function () {
        var configuration = YAZINO.configurationFactory({
            property1: "value1"
        });

        expect(configuration.get('property1')).toBe('value1');
    });

    it('should not affect input object', function () {
        var configIn = {
            property1: "value1"
        },
            configuration = YAZINO.configurationFactory(configIn);

        expect(typeof configIn.get).toBe('undefined');
    });

    it('generated configuration should not be affected by changes to input object', function () {
        var configIn = {
            property1: ["value1"]
        },
            configuration = YAZINO.configurationFactory(configIn);

        configIn.property1.push('test');

        expect(configuration.property1.length).toBe(1);
    });

    it('should allow hierarchical get', function () {
        var configuration = YAZINO.configurationFactory({
            property1: {
                subproperty1: 'value1'
            }
        });

        expect(configuration.get('property1.subproperty1')).toBe('value1');
    });

    it('should return undefined if parent not found in hierarchical get', function () {
        var configuration = YAZINO.configurationFactory({
            property1: {
                subproperty1: 'value1'
            }
        });

        expect(configuration.get('property2.subproperty1')).toBeUndefined();
    });

    it('should return undefined if parent is null in hierarchical get', function () {
        var configuration = YAZINO.configurationFactory({
            property1: null
        });

        expect(configuration.get('property1.subproperty1')).toBe(undefined);
    });

    it('should honour null properties', function () {
        var configuration = YAZINO.configurationFactory({
            property1: {
                subproperty1: null
            }
        });

        expect(configuration.get('property1.subproperty1')).toBe(null);
    });

    it('should log warnings for unreachable configuration keys', function () {
        var configuration = YAZINO.configurationFactory({
            'allkeys': {}
        });
        spyOn(YAZINO.logger, 'warn');

        configuration.get('allkeys.mykeys.key');

        expect(YAZINO.logger.warn).toHaveBeenCalledWith("unreachable configuration key 'allkeys.mykeys.key' because 'allkeys.mykeys' did not exist");
    });

    it('should be able to get config nodes added after config creation', function () {
        var configuration = YAZINO.configurationFactory({
            property1: 'value1'
        });
        configuration.property2 = 'value2';

        expect(configuration.get('property2')).toBe("value2");
    });

    it('should have function to set config nodes', function () {
        var configuration = YAZINO.configurationFactory({
            property1: 'value1'
        });
        configuration.set('property2', 'value2');

        expect(configuration.get('property2')).toBe("value2");
    });

    it('should be able to set child config nodes', function () {
        var configuration = YAZINO.configurationFactory({
            propertyContainer: {
                property1: 'abc'
            }
        });
        configuration.set('propertyContainer.property2', 'value2');

        expect(configuration.get('propertyContainer.property2')).toBe("value2");
    });

    it('should be able to set child config nodes when parent doesn\'t exist', function () {
        var configuration = YAZINO.configurationFactory({});
        configuration.set('propertyContainer.property2', 'value2');

        expect(configuration.get('propertyContainer.property2')).toBe("value2");
    });

    it('should have contains function', function () {
        var configuration = YAZINO.configurationFactory({});
        configuration.set('propertyContainer.property2', 'value2');

        expect(configuration.contains('propertyContainer')).toBeTruthy();
        expect(configuration.contains('propertyContainer.property2')).toBeTruthy();
        expect(configuration.contains('propertyContainer.property3')).toBeFalsy();
        expect(configuration.contains('propertyContainer1.property2')).toBeFalsy();
    });

    it('should be able to set whole object config nodes when parent doesn\'t exist', function () {
        var configuration = YAZINO.configurationFactory({});
        configuration.set('propertyContainer.test', {
            property1: 'value1',
            property2: 'value2'
        });

        expect(configuration.get('propertyContainer.test.property1')).toBe("value1");
        expect(configuration.get('propertyContainer.test.property2')).toBe("value2");
    });

    it('should not be able to externally change configuration after setting it', function () {
        var configuration = YAZINO.configurationFactory({}),
            myChildConfig = {
                property1: 'value1',
                property2: 'value2'
            };
        configuration.set('propertyContainer.test', myChildConfig);
        myChildConfig.property2 = 'value3';

        expect(configuration.get('propertyContainer.test.property1')).toBe("value1");
        expect(configuration.get('propertyContainer.test.property2')).toBe("value2");
    });

    it('should not be able to externally change configuration after getting it', function () {
        var configuration = YAZINO.configurationFactory({}),
            myChildConfig;
        configuration.set('propertyContainer.test', {
            property1: 'value1',
            property2: 'value2'
        });
        myChildConfig = configuration.get('propertyContainer.test');
        myChildConfig.property2 = 'value3';

        expect(configuration.get('propertyContainer.test.property1')).toBe("value1");
        expect(configuration.get('propertyContainer.test.property2')).toBe("value2");
    });

    it('should surface new properties in legacy format', function () {
        var configIn = {
                property1: "value1"
            },
            configuration = YAZINO.configurationFactory(configIn);
        configuration.set('property2', 'value2');

        expect(configuration.property1).toBe('value1');
        expect(configuration.property2).toBe('value2');
    });

    it('should set up with no params', function () {
        var configuration = YAZINO.configurationFactory();
        configuration.set('property2', 'value2');

        expect(configuration.property2).toBe('value2');
    });

    it('should allow default value for get', function () {
        var configuration = YAZINO.configurationFactory();

        expect(configuration.get('property2', 'defaultvalue')).toBe('defaultvalue');
    });

    it('should log missing property', function () {
        var configuration = YAZINO.configurationFactory();
        spyOn(YAZINO.logger, 'info');
        configuration.get('property2', 'defaultvalue');

        expect(YAZINO.logger.info).toHaveBeenCalledWith("couldn't find key [%s], returning default [%s]", 'property2', 'defaultvalue');
    });

});
