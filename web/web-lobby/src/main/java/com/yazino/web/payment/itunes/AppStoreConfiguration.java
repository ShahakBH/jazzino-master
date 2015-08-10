package com.yazino.web.payment.itunes;

import com.yazino.mobile.yaps.config.TypedMapBean;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Represents any configuration that is required to communicate with or map values to Apple.
 * Chip package mappings are used to map our internal identifiers (IOS_USD8 etc) to
 * apple game specific ones (BLACKJACK_USD8_50K).
 */
public class AppStoreConfiguration implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(AppStoreConfiguration.class);

    private final Map<String, Map<String, String>> mAllPackageMappings = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> mStandardPackageMappings = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> mPromotionPackageMappings = new HashMap<String, Map<String, String>>();
    private Map<String, String> mStandardHeaders = new HashMap<String, String>();
    private Map<String, String> mStandardSubHeaders = new HashMap<String, String>();
    private Map<String, String> mGameBundleMappings = new HashMap<String, String>();

    @Override
    public void afterPropertiesSet() throws Exception {
        final Set<String> games = new HashSet<String>(mStandardPackageMappings.keySet());
        games.addAll(mPromotionPackageMappings.keySet());
        for (String game : games) {
            final Map<String, String> mappings = new HashMap<String, String>();
            if (mStandardPackageMappings.containsKey(game)) {
                mappings.putAll(mStandardPackageMappings.get(game));
            }
            if (mPromotionPackageMappings.containsKey(game)) {
                mappings.putAll(mPromotionPackageMappings.get(game));
            }
            mAllPackageMappings.put(game, mappings);
        }
    }

    public String findInternalIdentifier(final String gameType, final String appleIdentifier) {
        notNull(gameType);
        notNull(appleIdentifier);
        final Map<String, String> mappings = mAllPackageMappings.get(gameType);
        if (mappings == null) {
            LOG.warn("Game [{}] had no mapping for apple identifier [{}]", gameType, appleIdentifier);
            return null;
        }
        for (String key : mappings.keySet()) {
            final String value = mappings.get(key);
            if (value.equalsIgnoreCase(appleIdentifier)) {
                return key;
            }
        }
        return null;
    }

    public String findAppleIdentifier(final String gameType, final String internalIdentifier) {
        notNull(gameType);
        notNull(internalIdentifier);
        final Map<String, String> mappings = mAllPackageMappings.get(gameType);
        if (mappings == null) {
            LOG.warn("Game [{}] had no mapping for internal identifier [{}]", gameType, internalIdentifier);
            return null;
        }
        return mappings.get(internalIdentifier);
    }

    public Map<String, String> productIdentifierMappingsForGame(final String gameType) {
        if (mAllPackageMappings.containsKey(gameType)) {
            return mAllPackageMappings.get(gameType);
        } else {
            return Collections.<String, String>emptyMap();
        }
    }

    public Set<String> standardProductsForGame(final String gameType) {
        return productsForGameFromMap(gameType, mStandardPackageMappings);
    }

    public Set<String> promotionProductsForGame(final String gameType) {
        return productsForGameFromMap(gameType, mPromotionPackageMappings);
    }

    public String lookupGameType(final String gameIdentifier) {
        if (mGameBundleMappings.containsKey(gameIdentifier)) {
            return mGameBundleMappings.get(gameIdentifier);
        } else {
            return gameIdentifier;
        }
    }

    private static Set<String> productsForGameFromMap(final String gameType,
                                                      final Map<String, Map<String, String>> mappings) {
        final Map<String, String> products = mappings.get(gameType);
        if (products != null) {
            return new HashSet<String>(products.values());
        }
        return Collections.emptySet();
    }

    public void setStandardPackageMappings(final Map<String, Map<String, String>> chipPackageMappings) {
        notNull(chipPackageMappings);
        mStandardPackageMappings = chipPackageMappings;
    }

    public void setPromotionPackageMappings(final Map<String, Map<String, String>> chipPackageMappings) {
        notNull(chipPackageMappings);
        mPromotionPackageMappings = chipPackageMappings;
    }

    public void setStandardHeaders(final Map<String, String> standardHeaders) {
        notNull(standardHeaders);
        mStandardHeaders = standardHeaders;
    }

    public Map<String, String> getStandardHeaders() {
        return mStandardHeaders;
    }

    public void setStandardSubHeaders(final Map<String, String> standardSubHeaders) {
        notNull(standardSubHeaders);
        mStandardSubHeaders = standardSubHeaders;
    }

    public Map<String, String> getStandardSubHeaders() {
        return mStandardSubHeaders;
    }

    public void setGameBundleMappings(final TypedMapBean<String, String> gameBundleMappings) {
        notNull(gameBundleMappings);
        mGameBundleMappings = gameBundleMappings.getSource();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
