/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

(function () {

    var fakeConfig,
        paymentConfig = {
            preferences: {
                paymentMethod: ""
            },
            packages: {
                "CREDITCARD": {
                    "EUR": [
                        {
                            "id": "optionEUR1",
                            "numChipsPerPurchase": "9000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "3.50",
                            "formattedChipsPerPurchase": "9,000"
                        },
                        {
                            "id": "optionEUR2",
                            "numChipsPerPurchase": "19000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "7",
                            "formattedChipsPerPurchase": "19,000"
                        }
                    ],
                    "USD": [
                        {
                            "id": "optionUSD1",
                            "numChipsPerPurchase": "11000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "5",
                            "formattedChipsPerPurchase": "11,000"
                        },
                        {
                            "id": "optionUSD2",
                            "numChipsPerPurchase": "22000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "10",
                            "formattedChipsPerPurchase": "22,000",
                            "promoId": "any promo",
                            "promotionChipsPerPurchase": "55000"
                        }
                    ]
                },
                "FACEBOOK": {
                    "EUR": [
                        {
                            "id": "optionEUR1",
                            "numChipsPerPurchase": "9000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "3.50",
                            "formattedChipsPerPurchase": "9,000"
                        },
                        {
                            "id": "optionEUR2",
                            "numChipsPerPurchase": "19000",
                            "currencyLabel": "€",
                            "amountRealMoneyPerPurchase": "7",
                            "formattedChipsPerPurchase": "19,000"
                        }
                    ],
                    "USD": [
                        {
                            "id": "optionUSD1",
                            "numChipsPerPurchase": "11000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "5",
                            "formattedChipsPerPurchase": "11,000"
                        },
                        {
                            "id": "optionUSD2",
                            "numChipsPerPurchase": "22000",
                            "currencyLabel": "$",
                            "amountRealMoneyPerPurchase": "10",
                            "formattedChipsPerPurchase": "22,000",
                            "promoId": "any promo",
                            "promotionChipsPerPurchase": "55000"
                        }
                    ]
                }
            }
        };

    function setupConfig() {
        fakeConfig = YAZINO.configurationFactory();
        fakeConfig.set('payment', paymentConfig);
        spyOn(YAZINO.configuration, 'get').andCallFake(fakeConfig.get);
    }

    describe('PaymentMethodSelectorWidget', function () {
        var widgetTemplate, paymentService = {
            selectPaymentMethod: jasmine.createSpy()
        };

        YAZINO.EventDispatcher.apply(paymentService);

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#payment_method_box").paymentMethodSelectorWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should ask service to select payment method", function () {
            jQuery("#paiment_credit").click();
            expect(paymentService.selectPaymentMethod).toHaveBeenCalledWith("creditcard");
        });
    });

    describe('CurrencySelectorWidget', function () {
        var widgetTemplate, paymentService = {
            selectCurrency: jasmine.createSpy()
        };

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#currency_list").currencySelectorWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should ask service to select currency", function () {
            jQuery("#currency_list").val('USD');
            jQuery("#currency_list").change();
            expect(paymentService.selectCurrency).toHaveBeenCalledWith("USD");
        });

    });

    describe('  PaymentOptionSelectorWidget', function () {
        var widgetTemplate, paymentService = {
            selectPaymentOption: jasmine.createSpy()
        };

        YAZINO.EventDispatcher.apply(paymentService);

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#select_chips_value").paymentOptionSelectorWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should ask service to select option", function () {
            jQuery("#paymentOptions li.optionUSD5").click();
            expect(paymentService.selectPaymentOption).toHaveBeenCalledWith("optionUSD5");
        });

        it("Should update packages based on event", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {
                    id: "optionUSD1"
                }
            });
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {
                    id: "optionUSD2"
                }
            });
            var unselected = jQuery("#paymentOptions li.optionUSD1"),
                selected = jQuery("#paymentOptions li.optionUSD2");
            expect(unselected.children("input").is(":checked")).toBeFalsy();
            expect(selected.children("input").is(":checked")).toBeTruthy();
        });

        it("Should update form inputs for promotion depending on option selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {
                    id: "optionUSD1"
                }
            });
            expect(jQuery("#paymentSelection input[name='promotionId']").val()).toEqual("");
            expect(jQuery("#paymentSelection input[name='promotionChips']").val()).toEqual("");
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {
                    id: "optionUSD2",
                    promoId: "my promo",
                    numChipsPerPurchase: "12",
                    promotionChipsPerPurchase: "24"
                }
            });
            expect(jQuery("#paymentSelection input[name='promotionId']").val()).toEqual("my promo");
            expect(jQuery("#paymentSelection input[name='promotionChips']").val()).toEqual("24");
        });

        it("Should update packages when changing currency", function () {

            paymentService.dispatchEvent({
                eventType: "AvailablePaymentOptionsChanged",
                paymentOptions: fakeConfig.get('payment.packages.CREDITCARD.EUR')

            });
            var checkWidget = function (nameOfOption, formattedChips, amountOfMoney) {
                expect(jQuery("#paymentOptions li." + nameOfOption).length).toBeGreaterThan(0);
                expect(jQuery("#paymentOptions input[value='" + nameOfOption + "']").length).toBeGreaterThan(0);
                expect(jQuery("#paymentOptions li." + nameOfOption + " .chips-title-value").text()).toEqual(formattedChips);
                expect(jQuery("#paymentOptions li." + nameOfOption + " .checkbox").text()).toEqual(amountOfMoney);
            };

            checkWidget("optionEUR1", "9,000", "€3.50");
            checkWidget("optionEUR2", "19,000", "€7");


            expect(jQuery("#paymentOptions li.optionUSD1").length).toBe(0);
            expect(jQuery("#paymentOptions li.optionUSD2").length).toBe(0);

        });

        it("Should display promotions if present", function () {
            paymentService.dispatchEvent({
                eventType: "AvailablePaymentOptionsChanged",
                paymentOptions: fakeConfig.get('payment.packages.CREDITCARD.USD')
            });
            expect(jQuery("#paymentOptions li.optionUSD1 .value-wrapper").attr("class")).not.toContain("promotion");
            expect(jQuery("#paymentOptions li.optionUSD1 .promotion-chips").is(":visible")).toBeFalsy();
            expect(jQuery("#paymentOptions li.optionUSD1 .promotion-chips").text()).toEqual("");

            expect(jQuery("#paymentOptions li.optionUSD2 .value-wrapper").attr("class")).toContain("promotion");
            expect(jQuery("#paymentOptions li.optionUSD2 .promotion-chips").is(":visible")).toBeTruthy();
            expect(jQuery("#paymentOptions li.optionUSD2 .promotion-chips").text()).toEqual("55,000");
        });

    });


    describe('TrialPayWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            fakeConfig.set('payment.trialPay.cashierUrl', "trialPayUrl");
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#paymentSelection").trialPayWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
            jQuery("#action_box_iframe").remove();
        });

        /**
         * This test runs fine on the browser, but makes the build fail
         * Apparently HtmlUnit doesn't like that kind of iframe manipulation :(
         */
        /**
         it("Should add iframe when payment method is selected", function () {
         paymentService.dispatchEvent({
         eventType: "PaymentMethodSelected",
         paymentMethod: "trialpay"
         });
         expect(jQuery("#action_box_iframe").length).toBeGreaterThan(0);
         expect(jQuery("#action_box_iframe").is(":visible")).toBeTruthy();
         });
         **/

        it("Should remove iframe when payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "anything_else"
            });
            expect(jQuery("#action_box_iframe").length).toEqual(0);
        });
    });

    describe('PayPalWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#paymentSelection").payPalWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should show paypal button if payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "paypal"
            });
            expect(jQuery("#paypalBtn").is(":visible")).toBeTruthy(0);
        });

        it("Should hide paypal button when other payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "anything_else"
            });
            expect(jQuery("#paypalBtn").is(":visible")).toBeFalsy(0);
        });
    });

    describe('CreditCardWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#paymentSelection").creditCardWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should show regular submit button if payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "creditcard"
            });
            expect(jQuery("#submitBtn").is(":visible")).toBeTruthy(0);
        });

        it("Should hide submit button payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "anything_else"
            });
            expect(jQuery(".creditCardElement").is(":visible")).toBeFalsy(0);
        });
    });

    describe('PaymentTabsWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#container").paymentTabsWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should apply correct class for paypal", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "paypal"
            });
            expect(jQuery("#container").hasClass("paypal-or-mobile")).toBeTruthy();
        });

        it("Should remove class for other payment methods", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "anything_else"
            });
            expect(jQuery("#container").hasClass("paypal-or-mobile")).toBeFalsy();
        });
    });

    describe('FacebookWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            spyOn(YAZINO.fb, 'purchase');
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#paymentSelection").facebookWidget(paymentService);
            console.log('MCTEST');
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should show facebook submit button if payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "facebook"
            });
            console.log('Facebook button', jQuery('#facebookBtn'));
            expect(jQuery("#facebookBtn").is(":visible")).toBeTruthy();
        });

        it("Should hide submit button payment method is selected", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentMethodSelected",
                paymentMethod: "anything_else"
            });
            expect(jQuery("#facebookBtn").is(":visible")).toBeFalsy();
        });

//        it("should call fb on click of fb button", function () {
//            paymentService.dispatchEvent({
//                eventType: "PaymentMethodSelected",
//                paymentMethod: "facebook"
//            });
//            jQuery("#facebookBtn").click();
//            expect(YAZINO.fb.purchase).toHaveBeenCalledWith("optionUSD6", "uniqueId");
//        });
//

    });

    describe('PaymentMessageWidget', function () {
        var widgetTemplate, paymentService = new YAZINO.EventDispatcher();

        beforeEach(function () {
            setupConfig();
            widgetTemplate = jQuery("#paymentSelection").clone();
            jQuery("#value_status").paymentMessageWidget(paymentService);
        });

        afterEach(function () {
            jQuery("#paymentSelection").replaceWith(widgetTemplate);
        });

        it("Should update message based on event", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {},
                chipMessage: {
                    title: "a title",
                    playerTitle: "a player title",
                    description: "a description"
                }
            });
            expect(jQuery('#value_status').is(":visible")).toBeTruthy();
            expect(jQuery('#value_status .value-status').text()).toEqual("a title");
            expect(jQuery('#value_status .player-status').text()).toEqual("a player title");
            expect(jQuery('#value_status .cell').text()).toEqual("a description");
        });

        it("Should clear message if message not present in event", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {}
            });
            expect(jQuery('#value_status .value-status').text()).toEqual("");
            expect(jQuery('#value_status .player-status').text()).toEqual("");
            expect(jQuery('#value_status .cell').text()).toEqual("");
        });

        it("Should clear message if message fields not present in event", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {},
                chipMessage: {}
            });
            expect(jQuery('#value_status .value-status').text()).toEqual("");
            expect(jQuery('#value_status .player-status').text()).toEqual("");
            expect(jQuery('#value_status .cell').text()).toEqual("");
        });

        it("Should clear message if payment option has promotion", function () {
            paymentService.dispatchEvent({
                eventType: "PaymentOptionSelected",
                paymentOption: {
                    promoId: "a promo"
                },
                chipMessage: {
                    title: "a title that shouldn't be displayed",
                    playerTitle: "a player title that shouldn't be displayed",
                    description: "a description that shouldn't be displayed"
                }
            });
            expect(jQuery('#value_status').is(":visible")).toBeFalsy();
        });
    });
}());
