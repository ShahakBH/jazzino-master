/*global FB, window, parent, top, document, jQuery */

var YAZINO = YAZINO || {};

YAZINO.createPaymentService = function () {
    var result = {},
        selectedPaymentMethod,
        selectedCurrency = YAZINO.configuration.get('payment.preferences.currency'),
        selectedPaymentOption,
        paymentOption,
        packages = YAZINO.configuration.get('payment.packages'),
        getPackagesForCurrentSelection,
        findPackage,
        findChipMessage,
        getDefaultPackage,
        dispatchPaymentOptionEvent,
        dispatchAvailablePaymentOptions;

    YAZINO.EventDispatcher.apply(result);

    function getSelectedPaymentMethod() {
        return selectedPaymentMethod || YAZINO.configuration.get('payment.preferences.paymentMethod', '').toLowerCase();
    }


    findPackage = function (paymentOptionId) {
        var packages = getPackagesForCurrentSelection(),
            i = 0;
        for (i = 0; i < packages.length; i += 1) {
            if (packages[i].id === paymentOptionId) {
                return packages[i];
            }
        }
    };

    findChipMessage = function () {
        if (!selectedCurrency || !selectedPaymentOption) {
            return;
        }
        return {
            title: selectedPaymentOption.paymentOptionValueStatus,
            playerTitle: selectedPaymentOption.paymentOptionPlayerTitle,
            description: selectedPaymentOption.upsellMessage
        };
    };

    getDefaultPackage = function () {
        var packages = getPackagesForCurrentSelection(),
            defaultPackageIndex = Math.ceil(packages.length / 2) - 1;
        return packages[defaultPackageIndex];
    };

    dispatchPaymentOptionEvent = function () {
        if (!selectedPaymentOption) {
            return;
        }
        result.dispatchEvent({
            eventType: "PaymentOptionSelected",
            paymentOption: selectedPaymentOption,
            chipMessage: findChipMessage()
        });
    };

    dispatchAvailablePaymentOptions = function () {
        result.dispatchEvent({
            eventType: "AvailablePaymentOptionsChanged",
            paymentOptions: getPackagesForCurrentSelection()
        });
    };

    getPackagesForCurrentSelection = function () {
        if (!getSelectedPaymentMethod() || !selectedCurrency) {
            return [];
        }

        YAZINO.logger.log('payment packages', packages, YAZINO.configuration.get('payment.packages'));
        paymentOption = getSelectedPaymentMethod().toUpperCase();
        var currenciesForPaymentMethod = packages[paymentOption];
        if (!currenciesForPaymentMethod) {
            return [];
        }
        return currenciesForPaymentMethod[selectedCurrency];
    };

    result.getPackages = getPackagesForCurrentSelection;

    result.selectPaymentMethod = function (paymentMethod) {
        selectedPaymentMethod = paymentMethod;
        result.dispatchEvent({
            eventType: "PaymentMethodSelected",
            paymentMethod: paymentMethod
        });
        dispatchAvailablePaymentOptions();
        YAZINO.businessIntelligence.track.purchase.viewedMethod(paymentMethod);
        selectedPaymentOption = getDefaultPackage();
        dispatchPaymentOptionEvent();
    };

    result.selectCurrency = function (currencyCode) {
        selectedCurrency = currencyCode;
        result.dispatchEvent({
            eventType: "CurrencyChanged",
            currency: selectedCurrency
        });
        dispatchAvailablePaymentOptions();
        selectedPaymentOption = getDefaultPackage();
        dispatchPaymentOptionEvent();
    };

    result.selectPaymentOption = function (paymentOptionId) {
        selectedPaymentOption = findPackage(paymentOptionId);
        dispatchPaymentOptionEvent();
    };

    result.init = function () {
        result.selectPaymentMethod(getSelectedPaymentMethod());
        result.selectCurrency(selectedCurrency);
    };

    return result;
};

jQuery.fn.extend({
    currencySelectorWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this);
                widget.change(function () {
                    paymentService.selectCurrency(widget.val());
                });
            }
        );
    }
});

jQuery.fn.extend({
    paymentOptionSelectorWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this),
                    hasPromotionAndChipOverride = function (paymentOption) {
                        return paymentOption && paymentOption.promoId && paymentOption.promotionChipsPerPurchase
                            && paymentOption.numChipsPerPurchase !== paymentOption.promotionChipsPerPurchase;
                    },
                    definePromotion = function (promoId, promoChips) {
                        jQuery("#paymentSelection input[name='promotionId']").val(promoId);
                        jQuery("#paymentSelection input[name='promotionChips']").val(promoChips);
                    };
                widget.find('ul li').click(function () {
                    var paymentOptionId = jQuery(this).find(".chips-radio").val();
                    paymentService.selectPaymentOption(paymentOptionId);
                });
                paymentService.addEventListener("PaymentOptionSelected", function (event) {
                    var paymentOption = event.paymentOption,
                        packageWidget = jQuery("." + paymentOption.id);
                    packageWidget.addClass('checked');
                    packageWidget.siblings().removeClass('checked');
                    packageWidget.children('input').attr('checked', 'checked');
                    packageWidget.siblings().find('.promotion-chips').removeClass('selected');
                    packageWidget.find('.promotion-chips').addClass('selected');
                    definePromotion("", "");
                    if (hasPromotionAndChipOverride(paymentOption)) {
                        definePromotion(paymentOption.promoId, paymentOption.promotionChipsPerPurchase);
                    }
                });
                paymentService.addEventListener("AvailablePaymentOptionsChanged", function (event) {
                    var paymentOptionListItems = widget.find("li"),
                        paymentOption,
                        listItem,
                        i = 0;
                    for (i = 0; i < event.paymentOptions.length; i += 1) {
                        paymentOption = event.paymentOptions[i];
                        listItem = jQuery(paymentOptionListItems[i]);
                        listItem.removeClass().addClass(paymentOption.id);
                        listItem.find("input").val(paymentOption.id);
                        listItem.find(".chips-title-value").text(paymentOption.formattedChipsPerPurchase);
                        listItem.find(".checkbox").text(paymentOption.currencyLabel + paymentOption.amountRealMoneyPerPurchase);
                        listItem.find("span.img").removeClass().addClass("img").addClass("chips-" + paymentOption.numChipsPerPurchase);
                        if (hasPromotionAndChipOverride(paymentOption)) {
                            listItem.find(".value-wrapper").addClass("promotion");
                            listItem.find(".promotion-chips").text(YAZINO.util.formatCurrency(paymentOption.promotionChipsPerPurchase, 0));
                            listItem.find(".promotion-chips").show();
                        } else {
                            listItem.find(".value-wrapper").removeClass("promotion");
                            listItem.find(".promotion-chips").text("");
                            listItem.find(".promotion-chips").hide();
                        }
                    }
                });
            }
        );
    }
});

jQuery.fn.extend({
    trialPayWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var cashierUrl = YAZINO.configuration.get('payment.trialPay.cashierUrl');
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    if (event.paymentMethod === "trialpay") {
                        jQuery('#tabs').show();
                        jQuery("body").attr("class", "");
                        if (jQuery("#action_box_iframe").length === 0) {
                            var trialpayFrame = jQuery("<iframe id='action_box_iframe' src='" + cashierUrl + "' frameborder='no' scrolling='no' style='height:379px; width: 630px;'></iframe>");
                            trialpayFrame.insertBefore("#action_box");
                            jQuery("#action_box").hide();
                        }
                    } else {
                        jQuery("#action_box_iframe").remove();
                        jQuery("#action_box").show();
                    }
                });
            }
        );
    }
});

jQuery.fn.extend({
    payPalWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    if (event.paymentMethod === "paypal") {
                        jQuery('#tabs').show();
                        jQuery('#paypalBtn').show();
                        jQuery('#action_body').show();
                        jQuery("body").attr("class", "");
                    } else {
                        jQuery('#paypalBtn').hide();
                    }
                });
                jQuery('#paypalBtn').click(function () {
                    YAZINO.businessIntelligence.track.purchase.selectedMethod("paypal");
                    jQuery('#action_body').children().hide();
                    jQuery('#paypal_please_wait').show();
                });
            }
        );
    }
});

jQuery.fn.extend({
    facebookWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var success = function (data) {
                    YAZINO.logger.log(data);
                    if (data.status === "settled") {
                        YAZINO.logger.log('SUCCESS!');
                        var actionBody = jQuery('#action_body'),
                            cofirmationBody = jQuery('<div/>').attr("id", "cofirmation_Body"),
                            cofirmationStatus = jQuery('<div/>').addClass('cofirmation-status'),
                            statusSpan = jQuery('<span/>').addClass('status').text('THANK YOU'),
                            statusDesc = jQuery('<span/>').addClass('status-desc').text("you added chips to your account"),
                            closeButton = jQuery('<a/>').attr("href", YAZINO.configuration.get('baseUrl') + "/lobbyPartials/closeCashier")
                                .addClass("button").text('start playing now');

                        actionBody.html(cofirmationBody);
                        cofirmationBody.append(cofirmationStatus);
                        cofirmationStatus.append(statusSpan).append(statusDesc).append(closeButton);
                        jQuery('#main.buy-chips').addClass('confirmation').addClass('thank-you');

                        jQuery.ajax({
                            url: "/payment/facebook/earnedChips?player_id=" + YAZINO.configuration.get('playerId'),
                            type: "GET"
                        }).done(function (data) {
                            statusDesc.text("today you've earned " + data + " chips on your account");
                        }).fail(function (error) {
                            YAZINO.logger.warn('Unable to get earn chips amount: ' + error);
                        });
                    } else {
                        YAZINO.logger.log('FAILURE!');
                    }

                };
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    if (event.paymentMethod === "facebook") {
                        jQuery('#tabs').show();
                        jQuery('#facebookBtn').show();
                        jQuery('#facebookSupportedPayments').show();
                        jQuery('#action_body').show();
                        jQuery("body").attr("class", "");
                    } else {
                        jQuery('#facebookBtn').hide();
                        jQuery('#facebookSupportedPayments').hide();
                    }
                });
                jQuery('#facebookBtn').click(function () {
                    YAZINO.businessIntelligence.track.purchase.selectedMethod("facebook");
                    var currencyCode = "USD", amountPerPurchase = "0", paymentOption = jQuery(document).find('input:radio[name=paymentOption]:checked').val(),
                        requestId = jQuery(document).find('input[name=requestId]').val(), packages = paymentService.getPackages();

                    jQuery.each(packages, function (idx, value) {
                        if (paymentOption === value.id) {
                            currencyCode = value.currencyCode;
                            amountPerPurchase = value.amountRealMoneyPerPurchase;
                        }
                    });
                    YAZINO.logger.log("gonna try and send purchase for " + paymentOption + " with currency code:" + currencyCode
                        + " and amount per purchase" + amountPerPurchase + " and request " + requestId);

                    parent.YAZINO.fb.purchase(parent.YAZINO.configuration.get('callbackUrl') + "/fbog/product/" + paymentOption,
                        requestId);
                    parent.clearPaymentsOverlay();
                });

                jQuery('#paiment_facebook_earn').click(function () {
                    parent.YAZINO.fb.earn(parent.YAZINO.configuration.get('callbackUrl') + "/fbog/currency/" + parent.YAZINO.configuration.get('gameType') + "_earnchips",
                        success);
                });
            }
        );
    }
});

jQuery.fn.extend({
    creditCardWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    if (event.paymentMethod === "creditcard") {
                        jQuery('.creditCardElement').show();
                        jQuery('#action_body').show();
                        jQuery("body").attr("class", "");
                    } else {
                        jQuery('.creditCardElement').hide();
                    }
                });
                jQuery('#submitBtn').click(function () {
                    YAZINO.businessIntelligence.track.purchase.selectedMethod("creditcard");
                });
            }
        );
    }
});


jQuery.fn.extend({
    paymentMethodSelectorWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this);
                widget.find('ul li input').click(function () {
                    var input = jQuery(this),
                        paymentMethod = input.val();
                    paymentService.selectPaymentMethod(paymentMethod);
                });
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    var selected = widget.find("input[value='" + event.paymentMethod + "']"),
                        li = selected.closest('li');
                    li.addClass('check');
                    li.siblings().removeClass('check');
                    selected.attr('checked', 'checked');
                });
            }
        );
    }
});

jQuery.fn.extend({
    paymentTabsWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this);
                paymentService.addEventListener("PaymentMethodSelected", function (event) {
                    if (event.paymentMethod === "paypal" || event.paymentMethod === "facebook") {
                        widget.addClass('paypal-or-mobile');
                    } else {
                        widget.removeClass('paypal-or-mobile');
                    }
                });
            }
        );
    }
});

jQuery.fn.extend({
    paymentMessageWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this);
                paymentService.addEventListener("PaymentOptionSelected", function (event) {
                    if (event.paymentOption && event.paymentOption.promoId) {
                        widget.hide();
                    } else {
                        var title = "", playerTitle = "", description = "";
                        if (event.chipMessage) {
                            title = event.chipMessage.title || "";
                            playerTitle = event.chipMessage.playerTitle || "";
                            description = event.chipMessage.description || "";
                        }
                        widget.find('.value-status').text(title);
                        widget.find('.player-status').text(playerTitle);
                        widget.find('.cell').text(description);
                        widget.show();
                    }
                });
            }
        );
    }
});

jQuery.fn.extend({
    promotionMessageWidget: /** @this {!Object} */function (paymentService) {
        return this.each(
            function () {
                var widget = jQuery(this),
                    hasPromotionAndChipOverride = function (paymentOption) {
                        return paymentOption && paymentOption.promoId && paymentOption.promotionChipsPerPurchase
                            && paymentOption.numChipsPerPurchase !== paymentOption.promotionChipsPerPurchase;
                    };
                paymentService.addEventListener("PaymentOptionSelected", function (event) {
                    var paymentOption = event.paymentOption;
                    if (!hasPromotionAndChipOverride(paymentOption)) {
                        widget.hide();
                    } else {
                        if (paymentOption.promotionHeader !== "" || paymentOption.promotionText !== "") {
                            widget.children('.header').text(paymentOption.promotionHeader);
                            widget.children('.text').text(paymentOption.promotionText);
                            widget.css('display', 'block');
                        }
                        widget.show();
                    }
                });
            }
        );
    }
});

(function ($) {
    $(document).ready(
        function () {
            var paymentService = YAZINO.createPaymentService();
            $(".paymentMethodSelectorWidget").paymentMethodSelectorWidget(paymentService);
            $(".paymentTabsWidget").paymentTabsWidget(paymentService);
            $(".currencySelectorWidget").currencySelectorWidget(paymentService);
            $(".paymentOptionSelectorWidget").paymentOptionSelectorWidget(paymentService);
            $(".paymentMessageWidget").paymentMessageWidget(paymentService);
            $(".promotionMessageWidget").promotionMessageWidget(paymentService);
            $(".trialPayWidget").trialPayWidget(paymentService);
            $(".payPalWidget").payPalWidget(paymentService);
            $(".creditCardWidget").creditCardWidget(paymentService);
            $(".facebookWidget").facebookWidget(paymentService);
            paymentService.init();
        }
    );
}(jQuery));
