/*globals window*/

function createLocalStorageMock() {
    var mockStorage = {};
    return {
        setItem: function (key, value) {
            mockStorage[key] = value;
        },
        getItem: function (key) {
            return mockStorage[key] || null;
        },
        removeItem: function (key) {
            delete mockStorage[key];
        },
        clear: function () {
            mockStorage = [];
        },
        isTestDouble: true
    };
}
window.localStorageMock = createLocalStorageMock();
if (typeof window.localStorage === 'undefined') {
    window.localStorage = window.localStorageMock;
}
