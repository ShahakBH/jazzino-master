package strata.server.operations.promotion.model;

import com.yazino.platform.reference.Currency;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ChipPackage implements Serializable {
    private static final long serialVersionUID = -208942382276212921L;
    private BigDecimal defaultChips;

    private Map<Currency, PackagePrice> packagePrices;

    public BigDecimal getDefaultChips() {
        return defaultChips;
    }

    public void setDefaultChips(final BigDecimal defaultChips) {
        this.defaultChips = defaultChips;
    }

    public Map<Currency, PackagePrice> getPackagePrices() {
        return packagePrices;
    }

    public void setPackagePrices(final Map<Currency, PackagePrice> packagePrices) {
        this.packagePrices = packagePrices;
    }

    public void addPackagePrice(final PackagePrice packagePrice) {
        if (packagePrices == null) {
            packagePrices = new HashMap<Currency, PackagePrice>();
        }
        packagePrices.put(packagePrice.getCurrency(), packagePrice);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ChipPackage that = (ChipPackage) o;

        if (defaultChips != null) {
            if (!defaultChips.equals(that.defaultChips)) {
                return false;
            }
        } else if (that.defaultChips != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (defaultChips != null) {
            return defaultChips.hashCode();
        } else {
            return 0;
        }
    }
}
