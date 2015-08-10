package strata.server.operations.promotion.service;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.model.PackagePrice;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang.Validate.notNull;

public class PaymentOptionsToChipPackageTransformer {
    private final Map<Platform, List<ChipPackage>> defaultPackages = new HashMap<>();
    private final JDBCPaymentOptionDAO paymentOptionDAO;

    @Autowired
    public PaymentOptionsToChipPackageTransformer(final JDBCPaymentOptionDAO paymentOptionDAO) {
        notNull(paymentOptionDAO, "paymentOptionDAO may not be null");

        this.paymentOptionDAO = paymentOptionDAO;
    }

    public Map<Platform, List<ChipPackage>> getDefaultPackages() {
        for (Platform platform : Platform.values()) {
            defaultPackages.put(platform, getChipPackagesFrom(paymentOptionDAO.findByPlatform(platform)));
        }

        return defaultPackages;
    }

    private List<ChipPackage> getChipPackagesFrom(final Collection<PaymentOption> paymentOptions) {
        final Map<BigDecimal, ChipPackage> chipPackageMap = new HashMap<BigDecimal, ChipPackage>();
        for (PaymentOption paymentOption : paymentOptions) {
            ChipPackage chipPackage = chipPackageMap.get(paymentOption.getNumChipsPerPurchase());
            if (chipPackage == null) {
                chipPackage = new ChipPackage();
                chipPackage.setDefaultChips(paymentOption.getNumChipsPerPurchase());
                chipPackage.addPackagePrice(new PackagePrice(
                        Currency.valueOf(paymentOption.getRealMoneyCurrency()),
                        paymentOption.getAmountRealMoneyPerPurchase(),
                        paymentOption.getCurrencyLabel()));
                chipPackageMap.put(paymentOption.getNumChipsPerPurchase(), chipPackage);
            } else {
                chipPackage.addPackagePrice(new PackagePrice(
                        Currency.valueOf(paymentOption.getRealMoneyCurrency()),
                        paymentOption.getAmountRealMoneyPerPurchase(),
                        paymentOption.getCurrencyLabel()));
            }
        }

        final List<ChipPackage> chipPackages = new ArrayList<ChipPackage>(chipPackageMap.values());
        Collections.sort(chipPackages, new Comparator<ChipPackage>() {
            @Override
            public int compare(final ChipPackage chipPackage, final ChipPackage chipPackage1) {
                return chipPackage.getDefaultChips().compareTo(chipPackage1.getDefaultChips());
            }
        });
        return chipPackages;
    }
}
