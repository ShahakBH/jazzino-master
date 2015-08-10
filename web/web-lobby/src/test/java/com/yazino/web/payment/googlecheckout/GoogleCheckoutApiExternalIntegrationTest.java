package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.domain.CheckoutRedirect;
import com.google.checkout.sdk.domain.Item;
import com.google.checkout.sdk.domain.Money;
import com.yazino.logging.appender.ListAppender;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * To fully test the integration we need orders in the sandbox environment. Here we seem to have a couple of options
 * <ul>
 * <li>use existing orders in the sandbox </li>
 * <li>use the checkout sdk to create items and shopping carts, post the cart to the sandbox merchant, use WebDriver or
 * some such, to send url, log in with a google buyer account, place the order etc. Now we could can test our
 * integration against the orders just placed.</li>
 * </ul>
 * <p/>
 * Neither option is especially desirable. If we go with the first, then we rely on google not clearing out orders in
 * the sandbox. If they do then the tests would fail (well obviously). We also need orders in a known state.
 * we could log in to the merchant checkout account and change the
 * state of an order thus breaking a test. However at least that's in our control...
 * The second option requires a lot more code. Also, to test handling orders in particular financial states, we would
 * have to wait for the state to change. As there are no guarantees for how long this takes..., makes for a crap tests.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GoogleCheckoutApiExternalIntegrationTest {

    @Autowired
    @Qualifier("googleCheckoutApiIntegration")
    private GoogleCheckoutApiIntegration googleCheckoutApiIntegration;

    @Test
    @Ignore("this checkout api is deprecated by google. We no longer use this api to verify orders. We should remove all related code.")
    public void shouldHandleUnknownOrderNumber() throws IOException {
        String wellFormedOrderNumber = "2";
        final Order order = googleCheckoutApiIntegration.retrieveOrderState(wellFormedOrderNumber);
        assertThat(order.getStatus(), is(Order.Status.INVALID_ORDER_NUMBER));
    }

    @Test
    @Ignore("this checkout api is deprecated by google. We no longer use this api to verify orders. We should remove all related code.")
    public void shouldHandleMalformedOrderNumber() throws IOException {
        String wellFormedOrderNumber = "should be an integer";
        final Order order = googleCheckoutApiIntegration.retrieveOrderState(wellFormedOrderNumber);
        assertThat(order.getStatus(), is(Order.Status.INVALID_ORDER_NUMBER));
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("this checkout api is deprecated by google. We no longer use this api to verify orders. We should remove all related code.")
    public void shouldSendDeliverOrderRequest() {
        final ListAppender fallbackAppender = ListAppender.addTo(GoogleCheckoutApiIntegration.class);

        final boolean delivered = googleCheckoutApiIntegration.markOrderAsDelivered("x47457885120905");

        // AND the logger contains a warning about the state change failure
        Assert.assertThat((Iterable<String>) fallbackAppender.getMessages(),
                hasItem("Failed to change order state to delivered for order: x47457885120905"));

        assertThat(delivered, is(false));

    }

    //    this is here to quickly enable us to create carts that then can be bought via the redirect url.
    //    It is obviously not a test but is useful when playing with the server
    public void createShoppingCart() {
        Item item = new Item();
        item.setItemName("Buy 5,000 chips");
        item.setItemDescription("package of 5,000 chips");
        item.setMerchantItemId("texas_holdem_usd3_buys_5k");
        Money price = new Money();
        price.setCurrency("USD");
        price.setValue(BigDecimal.valueOf(2.99));
        item.setUnitPrice(price);
        item.setQuantity(1);
        final CheckoutRedirect checkoutRedirect = googleCheckoutApiIntegration
                .apiContext()
                .cartPoster()
                .makeCart()
                .addItem(item)
                .buildAndPost();
        System.out.println(
                "Buyer should be redirected to: " + checkoutRedirect.getRedirectUrl());
    }

    // debug this against against production keys (change env.props) to check notification history
    public void sendNotificationRequest() {
        final Order order = googleCheckoutApiIntegration.retrieveOrderState("1340900719509230");
    }
}
