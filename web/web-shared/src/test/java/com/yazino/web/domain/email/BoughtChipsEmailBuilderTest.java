package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.CreditCard;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class BoughtChipsEmailBuilderTest {

    private static final String TO_ADDRESS = "just@me.com";
    private static final String PLAYER_FIRST_NAME = "playerFirstName";
    private static final String CARD_NUMBER = "5500000000000004";
    private static final String PAYMENT_ID = "paymentID";
    private static final Date PAYMENT_DATE = new GregorianCalendar(1977, Calendar.DECEMBER, 1).getTime();
    private static final Currency CURRENCY = Currency.getInstance("GBP");
    private static final BigDecimal AMOUNT = new BigDecimal(99);
    private static final BigDecimal PURCHASED_CHIPS = new BigDecimal(150999);

    private final PlayerProfileService profileService = mock(PlayerProfileService.class);
    private final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();

    @Before
    public void setup() {
        builder.withCardNumber(CARD_NUMBER);
        builder.withEmailAddress(TO_ADDRESS);
        builder.withPaymentId(PAYMENT_ID);
        builder.withFirstName(PLAYER_FIRST_NAME);
        builder.withPaymentDate(PAYMENT_DATE);
        builder.withCurrency(CURRENCY);
        builder.withCost(AMOUNT);
        builder.withPurchasedChips(PURCHASED_CHIPS);
        builder.withPaymentEmailBodyTemplate(PaymentEmailBodyTemplate.CreditCard);
    }
    
    @Test
    public void shouldBuildRequestWithCorrectEmailAddress() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(newHashSet(TO_ADDRESS), request.getAddresses());
    }

    @Test
    public void shouldBuildRequestWithCorrectSubject() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("Play with your purchase, playerFirstName!", request.getSubject());
    }

    @Test
    public void shouldBuildRequestWithCorrectTemplate() throws Exception {
        EmailRequest request = builder.buildRequest(profileService);
        assertEquals("ConfirmationBuyChipsEmail.vm", request.getTemplate());
    }

    @Test
    public void shouldBuildRequestWithCorrectPropertiesForGBP() throws Exception {

        Map<String, Object> expectedMap = new HashMap<String, Object>();
        expectedMap.put("playerFirstName", PLAYER_FIRST_NAME);
        expectedMap.put("purchasedChips", "150,999");
        expectedMap.put("amountSpent", "£99.00");
        expectedMap.put("paymentDate", "01/12/77");
        expectedMap.put("PAYMENT_BODY", CreditCard.getBody(CARD_NUMBER));
        expectedMap.put("paymentId", PAYMENT_ID);
        expectedMap.put("includeGamesInFooter", true);

        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(expectedMap, request.getProperties());
    }

    @Test
    public void shouldCallEmailServiceWithCorrectMapForEUR() throws Exception {
        builder.withCurrency(Currency.getInstance("EUR"));

        Map<String, Object> expectedMap = newHashMap();
        expectedMap.put("playerFirstName", PLAYER_FIRST_NAME);
        expectedMap.put("purchasedChips", "150,999");
        expectedMap.put("amountSpent", "€ 99,00");
        expectedMap.put("paymentDate", "01/12/77");
        expectedMap.put("PAYMENT_BODY", CreditCard.getBody(CARD_NUMBER));
        expectedMap.put("paymentId", PAYMENT_ID);
        expectedMap.put("includeGamesInFooter", true);

        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(expectedMap, request.getProperties());
    }

    @Test
    public void shouldBuildRequestWithCorrectPropertiesForUSD() throws Exception {
        builder.withCurrency(Currency.getInstance("USD"));

        Map<String, Object> expectedMap = new HashMap<String, Object>();
        expectedMap.put("playerFirstName", PLAYER_FIRST_NAME);
        expectedMap.put("purchasedChips", "150,999");
        expectedMap.put("amountSpent", "$99.00");
        expectedMap.put("paymentDate", "12/1/77");
        expectedMap.put("PAYMENT_BODY", CreditCard.getBody(CARD_NUMBER));
        expectedMap.put("paymentId", PAYMENT_ID);
        expectedMap.put("includeGamesInFooter", true);

        EmailRequest request = builder.buildRequest(profileService);
        assertEquals(expectedMap, request.getProperties());
    }

    @Test
    public void addCorrectItemToTemplateWithCurrencyShouldWorkForUK() throws Exception {
        Map<String, Object> expectedMap = newHashMap();
        Map<String, Object> actualMap = newHashMap();

        expectedMap.put("amountSpent", "£10.99");
        expectedMap.put("paymentDate", "01/12/77");

        builder.addCorrectItemsToTemplateWithCurrency(Currency.getInstance("GBP"), new BigDecimal(10.987), PAYMENT_DATE, actualMap);
        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void addCorrectItemToTemplateWithCurrencyShouldWorkForUSD() throws Exception {
        Map<String, Object> expectedMap = newHashMap();
        Map<String, Object> actualMap = newHashMap();

        expectedMap.put("amountSpent", "$10.99");
        expectedMap.put("paymentDate", "12/1/77");

        builder.addCorrectItemsToTemplateWithCurrency(Currency.getInstance("USD"), new BigDecimal(10.987), PAYMENT_DATE, actualMap);
        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void addCorrectItemToTemplateWithCurrencyShouldWorkForAUD() throws Exception {
        Map<String, Object> expectedMap = newHashMap();
        Map<String, Object> actualMap = newHashMap();

        expectedMap.put("amountSpent", "$10.99");
        expectedMap.put("paymentDate", "1/12/77");

        builder.addCorrectItemsToTemplateWithCurrency(Currency.getInstance("AUD"), new BigDecimal(10.987), PAYMENT_DATE, actualMap);
        assertEquals(expectedMap, actualMap);
    }

        @Test
    public void addCorrectItemToTemplateWithCurrencyShouldWorkForMYR() throws Exception {
        Map<String, Object> expectedMap = newHashMap();
        Map<String, Object> actualMap = newHashMap();

        expectedMap.put("amountSpent", "RM10.99");
        expectedMap.put("paymentDate", "01/12/1977");

        builder.addCorrectItemsToTemplateWithCurrency(Currency.getInstance("MYR"), new BigDecimal(10.987), PAYMENT_DATE, actualMap);
        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void addCorrectItemToTemplateWithCurrencyShouldWorkForJPY() throws Exception {
        Map<String, Object> expectedMap = newHashMap();
        final BigDecimal amount = new BigDecimal(103456);

        final Locale locale = firstLocaleWithYen(); // the available locales are JVM specific
        expectedMap.put("amountSpent", NumberFormat.getCurrencyInstance(locale).format(amount));
        expectedMap.put("paymentDate", DateFormat.getDateInstance(DateFormat.SHORT, locale).format(PAYMENT_DATE));

        Map<String, Object> actualMap = newHashMap();
        builder.addCorrectItemsToTemplateWithCurrency(
                Currency.getInstance("JPY"), amount, PAYMENT_DATE, actualMap);
        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void getLocaleFromCurrencyReturnsCorrectLocale() {
          assertTrue(builder.getLocaleFromCurrency(Currency.getInstance("GBP")).equals(Locale.UK));
          assertTrue(builder.getLocaleFromCurrency(Currency.getInstance("USD")).equals(Locale.US));
    }

    @Test
    public void shouldBuildSubjectForPlayerWithoutFirstName(){
        builder.withFirstName(null);
        String subject = builder.buildRequest(profileService).getSubject();
        assertThat(subject, equalTo("Play with your purchase!"));
    }

    @Test
    public void shouldBuildSubjectForPlayerWithEmptyFirstName(){
        builder.withFirstName("");
        String subject = builder.buildRequest(profileService).getSubject();
        assertThat(subject, equalTo("Play with your purchase!"));
    }

    private Locale firstLocaleWithYen() {
        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.isNotBlank(locale.getCountry())
                    && Currency.getInstance(locale).getCurrencyCode().equals("JPY")) {
                return locale;
            }
        }

        throw new IllegalStateException("Couldn't find a locale using Yen");
    }
}
