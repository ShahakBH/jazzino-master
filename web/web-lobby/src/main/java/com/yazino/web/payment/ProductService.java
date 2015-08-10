package com.yazino.web.payment;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import com.yazino.web.api.payments.BestValueProductPolicy;
import com.yazino.web.api.payments.ChipProduct;
import com.yazino.web.api.payments.ChipProducts;
import com.yazino.web.api.payments.MostPopularProductPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod;

/**
 * A service providing available chip products for a player, platform, payment method and game type.
 *
 * @see ChipProducts
 */
public class ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    private final Platform platform;
    private final PaymentMethod paymentMethod;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final MostPopularProductPolicy mostPopularProductPolicy;
    private final BestValueProductPolicy bestValueProductPolicy;
    private final PaymentOptionIdTransformer paymentOptionIdTransformer;
    private ProductPreferredCurrencyService productPreferredCurrencyService;

    @Autowired
    public ProductService(Platform platform,
                          PaymentMethod paymentMethod,
                          @Qualifier("safeBuyChipsPromotionService") BuyChipsPromotionService buyChipsPromotionService,
                          MostPopularProductPolicy mostPopularProductPolicy,
                          BestValueProductPolicy bestValueProductPolicy,
                          PaymentOptionIdTransformer paymentOptionIdTransformer,
                          ProductPreferredCurrencyService productPreferredCurrencyService) {
        this.platform = platform;
        this.paymentMethod = paymentMethod;
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.mostPopularProductPolicy = mostPopularProductPolicy;
        this.bestValueProductPolicy = bestValueProductPolicy;
        this.paymentOptionIdTransformer = paymentOptionIdTransformer;
        this.productPreferredCurrencyService = productPreferredCurrencyService;
    }

    /**
     * Loads available products for player.
     * Queries promotion service for available payment options in player's preferred currency.
     * <p/>
     * Preferred currency defaults to USD. If this isn't acceptable, override {@link #preferredCurrency(javax.servlet.http.HttpServletRequest, java.math.BigDecimal, String) preferredCurrency}.
     *
     * @param request
     * @param playerId
     * @param gameType
     * @return products currently available to the player.
     */
    public ChipProducts getAvailableProducts(HttpServletRequest request, BigDecimal playerId, String gameType) {
        // get all payment options
        Map<Currency, List<PaymentOption>> paymentOptionsPerCurrency = buyChipsPromotionService.getBuyChipsPaymentOptionsFor(playerId, platform);
        // restrict to options to player's preferred currency or that required by platform
        Currency currency = preferredCurrency(request, playerId, gameType);
        List<PaymentOption> paymentOptions = paymentOptionsPerCurrency.get(currency);

        if (paymentOptions == null || paymentOptions.isEmpty()) {
            LOG.error("No payment options found for player {}, currency {} and platform {}", playerId, currency.name(), platform.name());
            return new ChipProducts();
        }

        ChipProducts chipProducts = buildChipProductsFrom(gameType, paymentOptions);
        setMostPopularProductId(chipProducts);
        setBestValueProductId(chipProducts);
        return chipProducts;
    }

    private Currency preferredCurrency(HttpServletRequest request, BigDecimal playerId, String gameType) {
        return productPreferredCurrencyService.getPreferredCurrency(new ProductRequestContext(platform, playerId, gameType, request));
    }

    protected String getLabelForLevel(int level) {
        switch (level) {
            case 6:
                return "Millionaire Maven";
            case 5:
                return "Power Player";
            case 4:
                return "Savvy Star";
            case 3:
                return "Lucky Break";
            case 2:
                return "Clever Competitor";
            case 1:
                return "Starter Style";
            default:
                return "";
        }
    }

    private ChipProducts buildChipProductsFrom(String gameType, List<PaymentOption> options) {
        ChipProducts products = new ChipProducts();
        for (PaymentOption option : options) {
            String transformedId = paymentOptionIdTransformer.transformPaymentOptionId(gameType, option, paymentMethod);
            if (transformedId == null) {
                LOG.error("Failed to transform payment option id. Option will not be returned. gameType={}, option={}", gameType, option);
                continue;
            }
            BigDecimal chips = option.getNumChipsPerPurchase();
            ChipProduct.ProductBuilder builder = new ChipProduct.ProductBuilder()
                    .withChips(chips)
                    .withProductId(transformedId)
                    .withPrice(option.getAmountRealMoneyPerPurchase())
                    .withCurrencyLabel(option.getCurrencyLabel())
                    .withLabel(getLabelForLevel(option.getLevel()));
            addPromotionDetailsToProduct(builder, option, chips);
            products.addChipProduct(builder.build());
        }
        return products;
    }

    private void addPromotionDetailsToProduct(ChipProduct.ProductBuilder builder, PaymentOption option, BigDecimal chips) {
        if (option.hasPromotion(paymentMethod)) {
            PromotionPaymentOption promotion = option.getPromotion(paymentMethod);
            builder.withPromoId(promotion.getPromoId());
            BigDecimal promoChips = promotion.getPromotionChipsPerPurchase();
            if (promoChips != null && promoChips.compareTo(chips) != 0) {
                builder.withPromoChips(promoChips);
            }
        }
    }


    private void setBestValueProductId(ChipProducts chipProducts) {
        String productIdOfBestValueProduct = bestValueProductPolicy.findProductIdOfBestValueProduct(chipProducts.getChipProducts());
        chipProducts.setBestProductId(productIdOfBestValueProduct);
    }

    private void setMostPopularProductId(ChipProducts chipProducts) {
        String productIdOfMostPopularProduct = mostPopularProductPolicy.findProductIdOfMostPopularProduct(chipProducts.getChipProducts());
        chipProducts.setMostPopularProductId(productIdOfMostPopularProduct);
    }
}
