package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a {@link EmailRequest} for players that have purchased chips.
 */
public class BoughtChipsEmailBuilder extends AbstractEmailBuilder {

    public static final String TEMPLATE_NAME = "ConfirmationBuyChipsEmail.vm";
    public static final String SUBJECT_TEMPLATE = "Play with your purchase, %s!";
    public static final String SUBJECT_TEMPLATE_NO_FIRST_NAME = "Play with your purchase!";
    public static final String FROM_ADDRESS = "Yazino <contact@yazino.com>";

    public BoughtChipsEmailBuilder() {
        setTemplateProperty("includeGamesInFooter", true);
    }

    public BoughtChipsEmailBuilder withPurchasedChips(final BigDecimal chips) {
        setOtherProperty("chips", chips);
        return this;
    }

    public BigDecimal getPurchasedChips() {
        return (BigDecimal) getOtherProperty("chips");
    }

    public BoughtChipsEmailBuilder withCost(final BigDecimal cost) {
        setOtherProperty("cost", cost);
        return this;
    }

    public BigDecimal getCost() {
        return (BigDecimal) getOtherProperty("cost");
    }

    public BoughtChipsEmailBuilder withPaymentId(final String paymentId) {
        setTemplateProperty("paymentId", paymentId);
        return this;
    }

    public String getPaymentId() {
        return (String) getTemplateProperty("paymentId");
    }

    public BoughtChipsEmailBuilder withCurrency(final Currency currency) {
        setOtherProperty("currency", currency);
        return this;
    }

    public Currency getCurrency() {
        return (Currency) getOtherProperty("currency");
    }

    public BoughtChipsEmailBuilder withPaymentDate(final Date date) {
        setOtherProperty("paymentDate", date);
        return this;
    }

    public Date getPaymentDate() {
        return (Date) getOtherProperty("paymentDate");
    }

    public BoughtChipsEmailBuilder withPaymentEmailBodyTemplate(final PaymentEmailBodyTemplate bodyTemplate) {
        setOtherProperty("paymentEmailBodyTemplate", bodyTemplate);
        return this;
    }

    public PaymentEmailBodyTemplate getPaymentEmailBodyTemplate() {
        return (PaymentEmailBodyTemplate) getOtherProperty("paymentEmailBodyTemplate");
    }

    public BoughtChipsEmailBuilder withCardNumber(final String cardNumber) {
        setOtherProperty("cardNumber", cardNumber);
        return this;
    }

    public BoughtChipsEmailBuilder withTargetUrl(String targetUrl) {
        setTemplateProperty("targetUrl", targetUrl);
        return this;
    }

    public BoughtChipsEmailBuilder withIncludeGamesInFooter(boolean includeGamesInFooter) {
        setTemplateProperty("includeGamesInFooter", includeGamesInFooter);
        return this;
    }

    public String getCardNumber() {
        return (String) getOtherProperty("cardNumber");
    }

    public BoughtChipsEmailBuilder withEmailAddress(final String emailAddress) {
        setOtherProperty("emailAddress", emailAddress);
        return this;
    }

    public String getEmailAddress() {
        return (String) getOtherProperty("emailAddress");
    }

    public BoughtChipsEmailBuilder withFirstName(final String firstName) {
        setOtherProperty("firstName", firstName);
        return this;
    }

    public String getFirstName() {
        return (String) getOtherProperty("firstName");
    }

    @Override
    public EmailRequest buildRequest(final PlayerProfileService profileService) {
        final String firstname = getFirstName();
        String subject = SUBJECT_TEMPLATE_NO_FIRST_NAME;
        if (firstname != null && !"".equals(firstname)) {
            subject = String.format(SUBJECT_TEMPLATE, firstname);
        }

        addCorrectItemsToTemplateWithCurrency(getCurrency(), getCost(), getPaymentDate(), getTemplateProperties());
        setTemplateProperty("playerFirstName", firstname);
        setTemplateProperty("PAYMENT_BODY", getPaymentEmailBodyTemplate().getBody(getCardNumber()));
        final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        final String purchasedChipsAsString = numberFormat.format(getPurchasedChips());
        setTemplateProperty("purchasedChips", purchasedChipsAsString);

        return new EmailRequest(TEMPLATE_NAME, subject, FROM_ADDRESS, getTemplateProperties(), getEmailAddress());
    }

    final void addCorrectItemsToTemplateWithCurrency(final Currency currency,
                                                     final BigDecimal amount,
                                                     final Date paymentDate,
                                                     final Map<String, Object> templateProperties) {

        final Locale locale = getLocaleFromCurrency(currency);
        final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);

        templateProperties.put("amountSpent", currencyFormatter.format(amount));
        templateProperties.put("paymentDate", dateFormat.format(paymentDate));
    }

    final Locale getLocaleFromCurrency(final Currency currency) {
        // added to optimize our top currencies
        if (currency.getCurrencyCode().equals("GBP")) {
            return Locale.UK;
        } else if (currency.getCurrencyCode().equals("USD")) {
            return Locale.US;
        } else if (currency.getCurrencyCode().equals("MYR")) {
            return new Locale("ms", "MY");
        } else if (currency.getCurrencyCode().equals("EUR")) {
            return Locale.ITALY;
        } else {
            return searchForLocaleWithCurrency(currency);
        }
    }

    private Locale searchForLocaleWithCurrency(final Currency currency) {
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                if (Currency.getInstance(locale).getCurrencyCode().equals(currency.getCurrencyCode())) {
                    return locale;
                }
            } catch (Exception e) {
                // ignore
            }

        }

        return Locale.UK;
    }
}
