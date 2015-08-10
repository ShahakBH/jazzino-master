/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Payment Service', function () {

    var options = {},
        messages = {},
        underTest,
        fakeConfig,
        messageConfig = {
            "8000": { "title": "Starter Style", "valueStatus": ""},
            "10000": { "title": "Starter Style", "valueStatus": ""},
            "18000": { "title": "Clever Competitor", "valueStatus": ""},
            "21000": { "title": "Clever Competitor", "valueStatus": ""},
            "50000": { "title": "Lucky Break", "valueStatus": ""},
            "125000": { "title": "Savvy Star", "valueStatus": "Good Value"},
            "150000": { "title": "Savvy Star", "valueStatus": "Good Value"},
            "300000": { "title": "Power Player", "valueStatus": "Great Value"},
            "400000": { "title": "Power Player", "valueStatus": "Great Value"},
            "1000000": { "title": "Millionaire Maven", "valueStatus": "Best Value"}
        },
        paymentConfig = {
            preferences: {},
            messages: messageConfig,
            packages: {
                "CREDITCARD": {
                    "EUR": [
                        {
                            "id": "optionEUR1",
                            "numChipsPerPurchase": "10000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "3.50",
                            "upsellMessage": "THAT'S 3 chips per €1",
                            "paymentOptionValueStatus": messageConfig["10000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["10000"].title
                        },
                        {
                            "id": "optionEUR2",
                            "numChipsPerPurchase": "21000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "7",
                            "upsellMessage": "THAT'S 2,100 chips per €1",
                            "paymentOptionValueStatus": messageConfig["21000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["21000"].title
                        }
                    ],
                    "USD": [
                        {
                            "id": "optionUSD1",
                            "numChipsPerPurchase": "10000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "5",
                            "upsellMessage": "THAT'S 3 chips per $1",
                            "paymentOptionValueStatus": messageConfig["10000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["10000"].title
                        },
                        {
                            "id": "optionUSD2",
                            "numChipsPerPurchase": "21000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "10",
                            "upsellMessage": "THAT'S 2,100 chips per $1",
                            "paymentOptionValueStatus": messageConfig["21000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["21000"].title

                        }
                    ]
                },
                "PAYPAL": {
                    "EUR": [
                        {
                            "id": "optionEUR1",
                            "numChipsPerPurchase": "10000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "3.50",
                            "upsellMessage": "THAT'S 3 chips per €1",
                            "paymentOptionValueStatus": messageConfig["10000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["10000"].title
                        },
                        {
                            "id": "optionEUR2",
                            "numChipsPerPurchase": "21000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "7",
                            "upsellMessage": "THAT'S 2,100 chips per £1",
                            "paymentOptionValueStatus": messageConfig["21000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["21000"].title
                        }
                    ],
                    "USD": [
                        {
                            "id": "optionUSD1",
                            "numChipsPerPurchase": "10000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "5",
                            "upsellMessage": "THAT'S 3 chips per $1",
                            "paymentOptionValueStatus": messageConfig["10000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["10000"].title
                        },
                        {
                            "id": "optionUSD2",
                            "numChipsPerPurchase": "21000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "10",
                            "upsellMessage": "THAT'S 2,100 chips per $1",
                            "paymentOptionValueStatus": messageConfig["21000"].valueStatus,
                            "paymentOptionPlayerTitle": messageConfig["21000"].title
                        }
                    ]
                }
            }
        };

    beforeEach(function () {
        fakeConfig = YAZINO.configurationFactory();
        fakeConfig.set('payment', paymentConfig);
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);
        underTest = YAZINO.createPaymentService(options, messages);
    });

    it('Should dispatch event for a payment method being selected', function () {
        var listener = jasmine.createSpy();
        spyOn(YAZINO.businessIntelligence.track.purchase, 'viewedMethod');
        underTest.addEventListener("PaymentMethodSelected", listener);
        underTest.selectPaymentMethod("paypal");
        expect(listener).toHaveBeenCalled();
        expect(YAZINO.businessIntelligence.track.purchase.viewedMethod).toHaveBeenCalledWith("paypal");
    });

    it('Should dispatch available payment options event for a payment method being selected', function () {
        var listener = jasmine.createSpy('listener');

        underTest.addEventListener("AvailablePaymentOptionsChanged", listener);
        underTest.selectPaymentMethod("paypal");
        expect(listener).toHaveBeenCalled();

        expect(listener).toHaveBeenCalledWith({
            eventType: "AvailablePaymentOptionsChanged",
            paymentOptions: []
        });

    });


    it('Should not dispatch event if currency is selected before payment method', function () {
        var listener = jasmine.createSpy('listener');
        underTest.addEventListener("PaymentOptionSelected", listener);
        underTest.selectCurrency("USD");
        expect(listener).not.toHaveBeenCalled();
    });

    it('Should  dispatch event if currency is selected and payment method is present', function () {
        var listener = jasmine.createSpy();
        underTest.addEventListener("PaymentOptionSelected", listener);
        underTest.selectPaymentMethod("paypal");
        underTest.selectCurrency("USD");
        expect(listener).toHaveBeenCalledWith({
            eventType: "PaymentOptionSelected",
            paymentOption: fakeConfig.get('payment.packages.PAYPAL.USD')[0],
            chipMessage: {
                playerTitle: fakeConfig.get('payment.messages.10000.title'),
                title: fakeConfig.get('payment.messages.10000.valueStatus'),
                description: fakeConfig.get('payment.packages.CREDITCARD.USD.0.upsellMessage')
            }

        });
    });

    it('Should include chip message for selected payment option', function () {
        var listener = jasmine.createSpy();
        underTest.addEventListener("PaymentOptionSelected", listener);
        underTest.selectPaymentMethod("creditcard");
        underTest.selectCurrency("USD");
        underTest.selectPaymentOption("optionUSD2");
        expect(listener).toHaveBeenCalledWith({
            eventType: "PaymentOptionSelected",
            paymentOption: fakeConfig.get('payment.packages.CREDITCARD.USD.1'),
            chipMessage: {
                playerTitle: messageConfig["21000"].title,
                title: messageConfig["21000"].valueStatus,
                description: fakeConfig.get('payment.packages.CREDITCARD.USD.1.upsellMessage')
            }
        });

    });

    it('Should dispatch event with all available packages when currency selected', function () {
        var listener = jasmine.createSpy('listener');
        underTest.addEventListener("AvailablePaymentOptionsChanged", listener);
        underTest.selectPaymentMethod("creditcard");
        underTest.selectCurrency("USD");
        expect(listener).toHaveBeenCalledWith({
            eventType: "AvailablePaymentOptionsChanged",
            paymentOptions: fakeConfig.get('payment.packages.CREDITCARD.USD')
        });
    });
});

