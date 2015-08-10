/*global jQuery */
/*jslint browser:true */
var YAZINO = YAZINO || {};

YAZINO.worldpay = (function () {

    function addHiddenProperty(form, key, value) {
        var hiddenField = document.createElement("input");
        hiddenField.setAttribute("type", "hidden");
        hiddenField.setAttribute("name", key);
        hiddenField.setAttribute("value", value);
        form.appendChild(hiddenField);
    }

    function registerCard() {
        YAZINO.logger.log('Submitting card to WorldPay for registration.');
        var cardRegistrationURL = jQuery('#cardRegistrationURL').val(),
            cardRegistrationToken = jQuery('#cardRegistrationOTT').val(),
            accountName = jQuery('#cardHolderName').val(),
            accountNumber = jQuery('#creditCardNumber').val(),
            expiryMonth = jQuery('#expirationMonth').val(),
            expiryYear = jQuery('#expirationYear').val(),
            securityCode = jQuery('#cvc2').val(),
            email = jQuery('#emailAddress').val(),
            form = document.createElement("form");

        form.setAttribute("method", "post");
        form.setAttribute("action", cardRegistrationURL);

        form._submit_function_ = form.submit;

        addHiddenProperty(form, "Action", "Add");
        addHiddenProperty(form, "AcctName", accountName);
        addHiddenProperty(form, "AcctNumber", accountNumber);
        addHiddenProperty(form, "ExpMonth", expiryMonth);
        addHiddenProperty(form, "ExpYear", expiryYear);
        addHiddenProperty(form, "CVN", securityCode);
        addHiddenProperty(form, "Email", email);
        addHiddenProperty(form, "OTT", cardRegistrationToken);

        document.body.appendChild(form);
        form._submit_function_();
    }
    return {
        registerCard: registerCard
    };
}());
