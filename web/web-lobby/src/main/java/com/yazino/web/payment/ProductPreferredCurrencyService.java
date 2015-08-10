package com.yazino.web.payment;

import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import com.yazino.web.service.FacebookCurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Looks up the player's or platform's preferred currency to display product info in.
 */
@Service
public class ProductPreferredCurrencyService {

    private FacebookCurrencyService facebookCurrencyService;

    @Autowired
    public ProductPreferredCurrencyService(FacebookCurrencyService facebookCurrencyService) {
        this.facebookCurrencyService = facebookCurrencyService;
    }

    /**
     * Look up the preferred currency for player/platform.<br/>
     * For platform FACEBOOK_CANVAS, the {@link com.yazino.web.service.FacebookCurrencyService facebook currency service} is used to
     * lookup the player's preferred currency.<br/>
     * For all other platforms, USD is returned.
     * @param context lookup context
     * @return player's preferred currency (facebook) or USD for other platforms.
     */
    public Currency getPreferredCurrency(ProductRequestContext context) {
        if (context.getPlatform() == Platform.FACEBOOK_CANVAS) {
            return facebookCurrencyService.getPreferredCurrencyFromFacebook(context.getRequest(), context.getGameType());
        }
        return Currency.USD;
    }
}
