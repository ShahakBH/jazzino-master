package strata.server.operations.promotion.view;

import com.yazino.platform.Platform;
import strata.server.operations.promotion.model.ChipPackage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChipPackageHelper {

    // private constructor for utility classes
    private ChipPackageHelper() {
    }

    // refactored out from promotioncontroller
    public static Map<Platform, BigDecimal[]> getDefaultChipAmountsForAllPlatforms(final Map<Platform, List<ChipPackage>> defaultChipPackages) {
        final Map<Platform, BigDecimal[]> defaultChipAmounts = new HashMap<Platform, BigDecimal[]>();

        for (Platform platform : defaultChipPackages.keySet()) {
            final int defaultChipPackageListSize = defaultChipPackages.get(platform).size();

            final BigDecimal[] defaultAmountsForPlatform = new BigDecimal[defaultChipPackageListSize];
            defaultChipAmounts.put(platform, defaultAmountsForPlatform);

            final List<ChipPackage> defaultChipPackagesForPlatform = defaultChipPackages.get(platform);
            for (int i = 0; i < defaultChipPackageListSize; i++) {
                defaultAmountsForPlatform[i] = defaultChipPackagesForPlatform.get(i).getDefaultChips();
            }
            defaultChipAmounts.put(platform, defaultAmountsForPlatform);
        }
        return defaultChipAmounts;
    }

}
