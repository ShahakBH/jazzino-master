/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe("EventDispatcher", function () {
    var underTest,
        spyListener,
        spyListener2;

    beforeEach(function () {
        underTest = new YAZINO.EventDispatcher();
        spyListener = jasmine.createSpy("spyListener");
        spyListener2 = jasmine.createSpy("spyListener");
    });

    it("Listener is invoked when event is dispatched", function () {
        underTest.addEventListener("event", spyListener);
        underTest.dispatchEvent({eventType: "event", message: "myEvent"});
        expect(spyListener).toHaveBeenCalledWith({eventType: "event", message: "myEvent"});
    });

    it("Can register more than one listener and they will all be invoked when event is dispatched", function () {
        underTest.addEventListener("multipleEvent", spyListener);
        underTest.addEventListener("multipleEvent", spyListener2);
        underTest.dispatchEvent({eventType: "multipleEvent", message: "myEvent"});
        expect(spyListener).toHaveBeenCalledWith({eventType: "multipleEvent", message: "myEvent"});
        expect(spyListener2).toHaveBeenCalledWith({eventType: "multipleEvent", message: "myEvent"});
    });

    it("Only listeners to specific event are invoked", function () {
        underTest.addEventListener("someEvent", spyListener);
        underTest.addEventListener("myEvent", spyListener2);
        underTest.dispatchEvent({eventType: "myEvent"});
        expect(spyListener).not.toHaveBeenCalled();
        expect(spyListener2).toHaveBeenCalledWith({eventType: "myEvent"});
    });

    it("If a listener fails the other are still invoked", function () {
        underTest.addEventListener("errorEvent", spyListener.andCallFake(function () {
            throw {message: "testError"};
        }));
        underTest.addEventListener("errorEvent", spyListener2);
        underTest.dispatchEvent({eventType: "errorEvent"});
        expect(spyListener).toHaveBeenCalledWith({eventType: "errorEvent"});
        expect(spyListener2).toHaveBeenCalledWith({eventType: "errorEvent"});
    });

    it("Listener can be removed", function () {
        underTest.addEventListener("event", spyListener);
        underTest.dispatchEvent({eventType: "event", message: "pre"});
        underTest.removeEventListener("event", spyListener);
        underTest.dispatchEvent({eventType: "event", message: "post"});
        expect(spyListener).toHaveBeenCalledWith({eventType: "event", message: "pre"});
        expect(spyListener).not.toHaveBeenCalledWith({eventType: "event", message: "post"});
    });

    it("Special notification is sent when listener is added", function () {
        var MyDispatcher = function () {
            YAZINO.EventDispatcher.apply(this);
        },
            underTest = new MyDispatcher();
        underTest.addEventListener("_ListenerAdded", spyListener);
        underTest.addEventListener("some event", function (event) {});
        expect(spyListener).toHaveBeenCalledWith({ eventType : '_ListenerAdded', listenerType : 'some event' });
    });

    it("Special notification is sent when listener is removed", function () {
        var listener = function (event) {},
            MyDispatcher = function () {
                YAZINO.EventDispatcher.apply(this);
                this.addEventListener("_ListenerRemoved", spyListener);
            },
            underTest = new MyDispatcher();
        underTest.addEventListener("some event", listener);
        underTest.removeEventListener("some event", listener);
        expect(spyListener).toHaveBeenCalledWith({ eventType : '_ListenerRemoved', listenerType : 'some event' });
    });

});
