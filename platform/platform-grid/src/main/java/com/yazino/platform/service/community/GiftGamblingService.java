package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.gifting.CollectChoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GiftGamblingService {
    private static final int CENT = 100;

    private final YazinoConfiguration configuration;
    private Map<String, Long[]> probabilitiesCache = newHashMap();

    @Autowired
    public GiftGamblingService(final YazinoConfiguration configuration) {
        notNull(configuration, "configuration may not be null");

        this.configuration = configuration;
    }

    Map<String, Long[]> getProbabilitiesCache() {
        return probabilitiesCache;
    }

    public BigDecimal collectGift(final CollectChoice choice) {
        final String probabilityString = configuration.getString("strata.gifting.probability." + choice.name());
        if (isBlank(probabilityString)) {
            throw new IllegalArgumentException("Env.prop strata.gifting.probability." + choice.name() + " is not allowed to be blank");
        }
        final Long[] possibleReturns = getProbabilitiesFor(probabilityString);
        return new BigDecimal(randomlyPickOne(possibleReturns));
    }

    private Long randomlyPickOne(final Long[] returns) {
        return returns[(getRandomPercent())];
    }

    protected int getRandomPercent() {
        return (int) (Math.random() * CENT);
    }

    private Long[] getProbabilitiesFor(final String probabilityString) {
        final Long[] longs = probabilitiesCache.get(probabilityString);
        if (longs == null) {
            final String[] proportions = getProportions(probabilityString);
            final Long[] probabilities = new Long[CENT];
            int cursor = 0;
            try {

                for (String proportion : proportions) {

                    final String percentage = proportion.split(":")[0];
                    final Long reward = Long.valueOf(proportion.split(":")[1]);
                    for (int i = 0; i < Integer.parseInt(percentage); i++) {
                        probabilities[cursor++] = reward;
                    }
                }

                if (cursor != 100) {
                    throw new IllegalArgumentException("Probabilities did not add up to 100:" + (cursor));
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Probabilities did not add up to 100:" + (cursor));
            }

            probabilitiesCache.put(probabilityString, probabilities);
            return probabilities;
        } else {
            return longs;
        }

    }

    private String[] getProportions(final String probabilityString) {
        if (probabilityString.contains(" ")) {
            return probabilityString.split(" ");
        } else {
            return new String[]{probabilityString};
        }
    }

}
