package strata.server.lobby.promotion.service;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.promotion.domain.ExternalCredentials;

import java.math.BigInteger;

@Service("delegatePromotionControlGroupService")
public class PromotionControlGroupServiceImpl implements PromotionControlGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionControlGroupServiceImpl.class);

    private final LegacyPromotionFunctions legacyFunctions = new LegacyPromotionFunctions();

    public boolean isControlGroupMember(final PlayerProfile playerProfile,
                                        final Promotion promotion) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing for control group membership for player[{}], not cached",
                    playerProfile.getPlayerId());
        }

        switch (promotion.getControlGroupFunction()) {
            case PLAYER_ID:
                return legacyFunctions.isControlGroupMember(
                        BigInteger.valueOf(playerProfile.getPlayerId().longValue()),
                        promotion.getSeed(),
                        promotion.getControlGroupPercentage());
            case EXTERNAL_ID:
                return legacyFunctions.isControlGroupMember(
                        new ExternalCredentials(playerProfile.getProviderName(), externalIdForProvider(playerProfile)),
                        promotion.getSeed(),
                        promotion.getControlGroupPercentage());
            default:
                throw new IllegalArgumentException("Unsupported function type: \""
                        + promotion.getControlGroupFunction() + "\".");
        }
    }

    private String externalIdForProvider(final PlayerProfile playerProfile) {
        if (StringUtils.equals(playerProfile.getProviderName(), "YAZINO")) {
            return playerProfile.getPlayerId().toString();
        } else {
            return playerProfile.getExternalId();
        }
    }

    /**
     * The control group function changed after the introduction of Maximiles because they are not aware of
     * player ids.  The logic for choosing between the pre- and post-Maximiles functions is isolated in the
     * outer class.  Independent use of these functions is discouraged and should ultimately be prevented.
     */
    public static class LegacyPromotionFunctions {

        private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);

        /**
         * Calculates whether a player is considered an active member of a promotion or part of that promotion's control
         * group using the following function:
         * <p/>
         * <pre> f(P) = ((P + seed) mod 100) - Cg
         * <p/>
         * where P is the player Id (an integer)
         * seed is a random number between 0 and 99 (see below for explanation)
         * Cg is the percentage of players assigned to the control group (integer 0:100)
         * <p/>
         * if f(P) >= 0 then player P is offered the promotional packages
         * if f(P) < 0 then player P is not offered the promotional packages.}
         * </pre>
         *
         * @param playerId               the player id, an integer > 0
         * @param seed                   number used to randomize the player id so that the same subset of players are
         *                               not consistently assigned to promotion control groups. Int in range 0 to 99.
         * @param controlGroupPercentage the percentage of players in a promotion to assign to the control group.
         * @return true if control group member, false if active/targeted member
         */
        public boolean isControlGroupMember(final BigInteger playerId,
                                            final int seed,
                                            final int controlGroupPercentage) {
            return playerId.add(BigInteger.valueOf(seed))
                    .mod(ONE_HUNDRED)
                    .subtract(BigInteger.valueOf(controlGroupPercentage))
                    .signum() == -1;
        }

        /**
         * Tests whether a player, represented by the given external credentials, is a control or active member of a
         * promotion.
         *
         * @param externalCredentials    the player's external credentials.
         * @param seed                   the control group's seed, 0..99
         * @param controlGroupPercentage the control group percentage
         * @return true if control group member, false if active/targeted member
         */
        public boolean isControlGroupMember(final ExternalCredentials externalCredentials,
                                            final int seed,
                                            final int controlGroupPercentage) {
            if (externalCredentials == null) {
                return false;
            }

            final String credentialsStr = (externalCredentials.getProviderName()
                    + externalCredentials.getExternalId()).toUpperCase();
            final byte[] digest = DigestUtils.md5(credentialsStr);
            return new BigInteger(1, ArrayUtils.subarray(digest, 12, 16))
                    .add(BigInteger.valueOf(seed))
                    .mod(ONE_HUNDRED)
                    .subtract(BigInteger.valueOf(controlGroupPercentage))
                    .signum() == -1;
        }
    }
}
