package strata.server.lobby.api.promotion;

import com.yazino.platform.Platform;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

public class PromotionConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> configuration;

    public PromotionConfiguration() {
        configuration = new HashMap<String, String>();
    }

    public String getConfigurationValue(final String configKey) {
        return configuration.get(configKey);
    }

    public List<String> getImageUrls() {
        final List<String> imageUrls = new ArrayList<String>();
        imageUrls.add(getConfigurationValue(MAIN_IMAGE_KEY));
        imageUrls.add(getConfigurationValue(SECONDARY_IMAGE_KEY));
        imageUrls.add(getConfigurationValue(IOS_IMAGE_KEY));
        return imageUrls;
    }

    public Integer getConfigurationValueAsInteger(final String configKey) {
        Integer value = null;
        if (configuration.containsKey(configKey)) {
            try {
                value = Integer.parseInt(configuration.get(configKey));
            } catch (NumberFormatException e) {
                // just return null
            }
        }
        return value;
    }

    public BigDecimal getConfigurationValueAsBigDecimal(final String configKey) {
        BigDecimal value = null;
        if (configuration.containsKey(configKey)) {
            try {
                value = new BigDecimal(configuration.get(configKey));
            } catch (NumberFormatException e) {
                // just return null
            }
        }
        return value;
    }

    public void addConfigurationItem(final String configKey, final String configValue) {
        if (StringUtils.isNotBlank(configKey) && StringUtils.isNotBlank(configValue)) {
            configuration.put(configKey, configValue);
        }
    }

    public void replaceConfigurationItem(final String configKey, final String configValue) {
        if (StringUtils.isNotBlank(configKey)) {
            configuration.remove(configKey);
            if (StringUtils.isNotBlank(configValue)) {
                configuration.put(configKey, configValue);
            }
        }
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public boolean hasConfigItems() {
        return (MapUtils.isNotEmpty(configuration));
    }

    public void setConfiguration(final Map<String, String> configuration) {
        if (configuration == null) {
            this.configuration.clear();
        }
        this.configuration = new HashMap<String, String>(configuration);
    }

    public void overrideChipAmountForPlatformAndPackage(final Platform platform, final BigDecimal defaultAmount, final BigDecimal overriddenAmount) {
        this.addConfigurationItem(createChipAmountKey(platform, defaultAmount), overriddenAmount.toPlainString());
    }

    public BigDecimal getOverriddenChipAmountFormPlatformAndPackage(final Platform platform, BigDecimal defaultAmount) {
        final String promoChipValue = this.getConfigurationValue(createChipAmountKey(platform, defaultAmount));
        if (StringUtils.isNotBlank(promoChipValue)) {
            return new BigDecimal(promoChipValue);
        }
        return null;
    }

    private String createChipAmountKey(final Platform platform, final BigDecimal defaultAmount) {
        return String.format(BuyChipsPromotion.CHIP_AMOUNT_FORMAT_KEY, platform, defaultAmount.toPlainString());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(17, 37, this);
    }

    public void removeChipAmountOverrideForPlatformAndPackage(Platform platform, BigDecimal defaultAmount) {
        configuration.remove(createChipAmountKey(platform, defaultAmount));
    }
}
