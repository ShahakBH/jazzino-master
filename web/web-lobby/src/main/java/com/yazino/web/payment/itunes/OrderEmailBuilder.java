package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.domain.email.EmailRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * An email builder which uses the player profile service to retrieve email and firstname.
 */
public class OrderEmailBuilder extends BoughtChipsEmailBuilder {

    private final ExternalTransaction mTransaction;
    private final Order mOrder;

    public OrderEmailBuilder(final Order order,
                             final ExternalTransaction transaction) {
        super();
        Validate.notNull(order);
        Validate.notNull(transaction);
        mOrder = order;
        mTransaction = transaction;
    }

    @Override
    public EmailRequest buildRequest(final PlayerProfileService profileService) {
        final PlayerProfile profile = profileService.findByPlayerId(mOrder.getPlayerId());
        String name = profile.getFirstName();
        if (StringUtils.isEmpty(name)) {
            name = profile.getDisplayName();
        }
        withFirstName(name);
        withEmailAddress(profile.getEmailAddress());
        withPurchasedChips(mTransaction.getAmountChips());
        withCurrency(mTransaction.getCurrency());
        withCost(mTransaction.getAmountCash());
        withPaymentDate(mTransaction.getMessageTimeStamp().toDate());
        withCardNumber("");
        withPaymentId(mTransaction.getInternalTransactionId());
        withPaymentEmailBodyTemplate(lookupTemplate(mOrder));
        return super.buildRequest(profileService);
    }

    private PaymentEmailBodyTemplate lookupTemplate(final Order order) {
        switch(order.getPaymentMethod()) {
            case ITUNES: return PaymentEmailBodyTemplate.iTunes;
            default: return null;
        }
    }

}
