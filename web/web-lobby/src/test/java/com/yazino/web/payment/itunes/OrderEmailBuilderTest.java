package com.yazino.web.payment.itunes;

import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class OrderEmailBuilderTest {

    private final Order order = new Order("ITUNES", PaymentPreferences.PaymentMethod.ITUNES);
    private final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(BigDecimal.TEN)
            .withInternalTransactionId("internalId")
            .withExternalTransactionId("externalId")
            .withMessage("creditCardMessage", new DateTime())
            .withAmount(Currency.getInstance("USD"), BigDecimal.ONE)
            .withPaymentOption(BigDecimal.valueOf(100), null)
            .withCreditCardNumber("")
            .withCashierName("ITUNES")
            .withStatus(ExternalTransactionStatus.SUCCESS)
            .withType(ExternalTransactionType.DEPOSIT)
            .withGameType("SLOTS")
            .withPlayerId(BigDecimal.TEN)
            .withPromotionId(123l)
            .withPlatform(Platform.IOS)
            .build();
    private final OrderEmailBuilder builder = new OrderEmailBuilder(order, transaction);
    private final PlayerProfileService service = mock(PlayerProfileService.class);
    private final PlayerProfile profile = new PlayerProfile();

    @Before
    public void setup() {
        when(service.findByPlayerId(any(BigDecimal.class))).thenReturn(profile);
    }

    @Test
    public void shouldSetFirstNameFromProfile() {
        String expected = "MyFirstName";
        profile.setFirstName(expected);
        builder.buildRequest(service);
        assertEquals(expected, builder.getFirstName());
    }

    @Test
    public void shouldSetFirstNameToDisplayNameIfFirstNameIsNull() {
        String expected = "MyDisplayName";
        profile.setFirstName(null);
        profile.setDisplayName(expected);
        builder.buildRequest(service);
        assertEquals(expected, builder.getFirstName());
    }

    @Test
    public void shouldSetEmailAddressFromProfile() {
        String expected = "test@test.com";
        profile.setEmailAddress(expected);
        builder.buildRequest(service);
        assertEquals(expected, builder.getEmailAddress());
    }

    @Test
    public void shouldSetPurchasedChipsFromTransaction() {
        BigDecimal expected = transaction.getAmountChips();
        builder.buildRequest(service);
        assertEquals(expected, builder.getPurchasedChips());
    }

    @Test
    public void shouldSetCurrencyFromTransaction() {
        Currency expected = transaction.getCurrency();
        builder.buildRequest(service);
        assertEquals(expected, builder.getCurrency());
    }

    @Test
    public void shouldSetCostFromTransaction() {
        BigDecimal expected = transaction.getAmountCash();
        builder.buildRequest(service);
        assertEquals(expected, builder.getCost());
    }

    @Test
    public void shouldSetPaymentDateFromTransaction() {
        DateTime expected = transaction.getMessageTimeStamp();
        builder.buildRequest(service);
        assertEquals(expected, new DateTime(builder.getPaymentDate()));
    }

    @Test
    public void shouldClearCardNumber() {
        builder.buildRequest(service);
        assertTrue(StringUtils.isEmpty(builder.getCardNumber()));
    }

    @Test
    public void shouldSetPaymentIdFromTransaction() {
        String expected = transaction.getInternalTransactionId();
        builder.buildRequest(service);
        assertEquals(expected, builder.getPaymentId());
    }

    @Test
    public void shouldSetTemplateFromOrder() {
        builder.buildRequest(service);
        assertEquals(PaymentEmailBodyTemplate.iTunes, builder.getPaymentEmailBodyTemplate());
    }

}
