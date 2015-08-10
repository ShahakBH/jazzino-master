/*global jQuery */

jQuery.ajax = function () { // stub ajax to avoid external calls in jasmine tests - this should always be mocked.
    var returnVal = {
        done: function () {
            return returnVal;
        },
        fail: function () {
            return returnVal;
        }
    };
    return returnVal;
};

function deepEquals(actual, expected) {
    jQuery.each(expected, function (key, val) {
        if (typeof actual[key] === 'undefined') {
            expect("deepCopy to have key [" + key + "]").toBe();
            return;
        }
        expect(actual[key]).toBeDefined();
        if (typeof actual[key] === "object" && actual[key] !== null && actual[key].length) {
            deepEquals(actual[key], val);
        } else {
            expect(actual[key]).toBe(val);
        }
    });
}
window.gapi = {
    auth: {
        authorize: function () {
        },
        getToken: function () {
        },
        init: function(callback){
            callback();
        }
    },
    client: {
        setApiKey: function () {
        }
    }
};
