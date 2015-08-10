package com.yazino.web.payment.amazon;

import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.service.QuietPlayerEmailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Currency;

import static org.joda.time.DateTime.now;

@Component
public class PlayerNotifier {
    private QuietPlayerEmailer emailer;

    @Autowired
    public PlayerNotifier(final QuietPlayerEmailer emailer) {
        this.emailer = emailer;
    }

    public void emailPlayer(final PaymentContext context, final VerifiedOrder order, final PaymentEmailBodyTemplate template) {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        builder.withEmailAddress(context.getEmailAddress());
        builder.withFirstName(context.getPlayerName());
        builder.withPurchasedChips(order.getChips());
        builder.withCurrency(Currency.getInstance(order.getCurrencyCode()));
        builder.withCost(order.getPrice());
        builder.withPaymentDate(now().toDate());
        builder.withCardNumber("");
        builder.withPaymentId(order.getOrderId());
        builder.withPaymentEmailBodyTemplate(template);
        emailer.quietlySendEmail(builder);
    }
}
