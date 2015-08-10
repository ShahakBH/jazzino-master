package com.yazino.bi.payment;

import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.bi.payment.PaymentOptionTest.UpsellBuilder.getUpsell;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class PaymentOptionTest {

    @Test
    public void descriptionShouldHaveValuesSubstituted() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setCurrencyLabel("ß");
        paymentOption.setNumChipsPerPurchase(new BigDecimal("10000"));
        paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal("10.00"));
        paymentOption.setDescription("That's $CHIPS$ for $CURRENCY$$PRICE$");

        assertThat(paymentOption.getDescription(), is(equalTo("That's 10,000 for ß10")));
    }

    @Test
    public void upsellDescriptionShouldHaveValuesSubstituted() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setCurrencyLabel("ß");
        paymentOption.setNumChipsPerPurchase(new BigDecimal("10000"));
        paymentOption.setUpsellNumChipsPerPurchase(new BigDecimal("20000"));
        paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal("10.00"));
        paymentOption.setUpsellRealMoneyPerPurchase(new BigDecimal("20.00"));
        paymentOption.setUpsellDescription("That's $CHIPS$ for $CURRENCY$$PRICE$ which is $CURRENCY$$PRICE_DELTA$ more");

        assertThat(paymentOption.getUpsellDescription(), is(equalTo("That's 20,000 for ß20 which is ß10 more")));
    }

    @Test
    public void descriptionShouldHaveValuesWithSignificantDecimalPartSubstituted() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setCurrencyLabel("ß");
        paymentOption.setNumChipsPerPurchase(new BigDecimal("10000"));
        paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal("10.5"));
        paymentOption.setDescription("That's $CHIPS$ for $CURRENCY$$PRICE$");

        assertThat(paymentOption.getDescription(), is(equalTo("That's 10,000 for ß10.5")));
    }

    @Test
    public void upsellDescriptionShouldHaveValuesWithSignificantDecimalPartSubstituted() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setCurrencyLabel("ß");
        paymentOption.setNumChipsPerPurchase(new BigDecimal("10000"));
        paymentOption.setUpsellNumChipsPerPurchase(new BigDecimal("20000"));
        paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal("5.50"));
        paymentOption.setUpsellRealMoneyPerPurchase(new BigDecimal("10.00"));
        paymentOption.setUpsellDescription("That's $CHIPS$ for $CURRENCY$$PRICE$ which is $CURRENCY$$PRICE_DELTA$ more");

        assertThat(paymentOption.getUpsellDescription(), is(equalTo("That's 20,000 for ß10 which is ß4.5 more")));
    }

    @Test
    public void getUpsellShouldGiveCorrectAmount() {
        assertThat(getUpsell(10000, "5"), is(equalTo("THAT'S 2,000 chips per $1")));
        assertThat(getUpsell(21000, "10"), is(equalTo("THAT'S 2,100 chips per $1")));
        assertThat(getUpsell(50000, "20"), is(equalTo("THAT'S 2,500 chips per $1")));
        assertThat(getUpsell(150000, "50"), is(equalTo("THAT'S 3,000 chips per $1")));
        assertThat(getUpsell(400000, "100"), is(equalTo("THAT'S 4,000 chips per $1")));
        assertThat(getUpsell(1000000, "150"), is(equalTo("THAT'S 6,667 chips per $1")));
        UpsellBuilder.label = "€";
        assertThat(getUpsell(10000, "3.5"), is(equalTo("THAT'S 2,858 chips per $1")));
        assertThat(getUpsell(21000, "7"), is(equalTo("THAT'S 3,000 chips per $1")));
        assertThat(getUpsell(50000, "14"), is(equalTo("THAT'S 3,572 chips per $1")));
        assertThat(getUpsell(150000, "35"), is(equalTo("THAT'S 4,286 chips per $1")));
        assertThat(getUpsell(400000, "70"), is(equalTo("THAT'S 5,715 chips per $1")));
        assertThat(getUpsell(1000000, "105"), is(equalTo("THAT'S 9,524 chips per $1")));
    }

    static class UpsellBuilder {
        public static String label;

        public static String getUpsell(Integer chipAmount, String cashAmount) {
            label = "$";
            final PaymentOption paymentOption = new PaymentOption();
            paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal(cashAmount));
            paymentOption.setNumChipsPerPurchase(new BigDecimal(chipAmount));

            paymentOption.setCurrencyLabel(label);
            return paymentOption.getUpsellMessage();
        }
    }
}
