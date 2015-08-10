package com.yazino.web.session;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.community.*;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import com.yazino.web.data.CurrencyRepository;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("sessionRegistrar")
public class SessionRegistrar implements SessionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SessionRegistrar.class);

    private final CommunityService communityService;
    private final CurrencyRepository currencyRepository;
    private final PlayerProfileService playerProfileService;
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final PlayerCreditConfiguration playerCreditConfiguration;

    @Autowired
    public SessionRegistrar(final CommunityService communityService,
                            final CurrencyRepository currencyRepository,
                            final PlayerProfileService playerProfileService,
                            final PlayerService playerService,
                            final SessionService sessionService,
                            final PlayerCreditConfiguration playerCreditConfiguration) {
        notNull(communityService, "communityService may not be null");
        notNull(currencyRepository, "currencyRepository may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(sessionService, "sessionService may not be null");
        notNull(playerCreditConfiguration, "playerCreditConfiguration may not be null");

        this.communityService = communityService;
        this.currencyRepository = currencyRepository;
        this.playerProfileService = playerProfileService;
        this.playerService = playerService;
        this.sessionService = sessionService;
        this.playerCreditConfiguration = playerCreditConfiguration;
    }

    @Override
    public LobbySessionCreationResponse registerNewSession(final BigDecimal playerId,
                                                           final PartnerSession partnerSession,
                                                           Platform platform,
                                                           final LoginResult loginResult,
                                                           final Map<String, Object> clientContext) {
        final PlayerProfile playerProfile = playerProfileService.findByPlayerId(playerId);
        if (playerProfile == null) {
            return null;
        }
        ReferralResult referralResult = null;

        BasicProfileInformation player = playerService.getBasicProfileInformation(playerProfile.getPlayerId());
        if (player == null) {
            throw new IllegalArgumentException("Couldn't find player for ID " + playerProfile.getPlayerId());
        }

        if (loginResult == LoginResult.NEW_USER) {
            referralResult = processPlayerReferral(playerProfile, playerCreditConfiguration, player);

        } else {
            updateProfileIfRequired(playerProfile, player);
        }

        final LobbySession lobbySession = startNewSession(partnerSession, playerProfile, player, platform, clientContext);

        if (loginResult == LoginResult.EXISTING_USER) {
            sendLoggedOnEvent(playerId, lobbySession.getSessionId());
        }

        return new LobbySessionCreationResponse(lobbySession, loginResult == LoginResult.NEW_USER, referralResult);
    }

    private void updateProfileIfRequired(final PlayerProfile playerProfile, final BasicProfileInformation player) {
        try {
            final PaymentPreferences paymentPreferences = paymentPreferencesFor(playerProfile);
            if (havePlayerPropertiesBeenUpdated(player, paymentPreferences)) {
                communityService.asyncUpdatePlayer(player.getPlayerId(), player.getName(),
                        player.getPictureUrl(), paymentPreferences);
            }

        } catch (Exception e) {
            LOG.error("Unable to update profile for player {}", player.getPlayerId(), e);
        }
    }

    private void sendLoggedOnEvent(final BigDecimal playerId,
                                   final BigDecimal sessionId) {
        try {
            communityService.asyncSendPlayerLoggedOn(playerId, sessionId);

        } catch (Exception e) {
            LOG.error("Could not send player ID {} logged on as session {}", playerId, sessionId, e);
        }
    }

    private PaymentPreferences paymentPreferencesFor(final PlayerProfile playerProfile) {
        final Currency preferredCurrency = currencyRepository.getPreferredCurrencyFor(playerProfile.getCountry());
        return new PaymentPreferences(preferredCurrency);
    }

    private ReferralResult processPlayerReferral(final PlayerProfile userProfile,
                                                 final PlayerCreditConfiguration creditConfig,
                                                 final BasicProfileInformation details) {
        final BigDecimal referralPlayerId = determineReferringAccountId(userProfile.getReferralIdentifier());
        if (referralPlayerId == null) {
            return null;
        }

        try {
            final BasicProfileInformation referralPlayer =
                    playerService.getBasicProfileInformation(referralPlayerId);

            if (referralPlayer == null) {
                LOG.warn("Could not load player with player ID {} for referral of player ID {}", referralPlayerId, details.getPlayerId());
                return null;

            } else {
                createBiDirectionalFriendship(details, referralPlayer);

                return new ReferralResult(referralPlayerId, creditConfig.getReferralAmount());
            }

        } catch (Exception e) {
            LOG.error("Unable to process player referral for player {}", details.getPlayerId(), e);
            return null;
        }
    }

    private void createBiDirectionalFriendship(final BasicProfileInformation player1,
                                               final BasicProfileInformation player2) {
        requestRelationshipChange(player2, player1, RelationshipAction.SET_EXTERNAL_FRIEND);
        requestRelationshipChange(player1, player2, RelationshipAction.SET_EXTERNAL_FRIEND);
    }

    private static BigDecimal determineReferringAccountId(final String referrer) {
        if (!isBlank(referrer)) {
            try {
                return new BigDecimal(referrer);
            } catch (final Throwable t) {
                LOG.warn("Could not parse referring account id from referrer {}", referrer);
            }
        }
        return null;
    }

    private void requestRelationshipChange(final BasicProfileInformation actioning,
                                           final BasicProfileInformation related,
                                           final RelationshipAction relationshipAction) {
        communityService.asyncRequestRelationshipAction(new RelationshipRequest(actioning.getPlayerId(),
                related.getPlayerId(), related.getName(), relationshipAction, false));
        communityService.asyncRequestRelationshipAction(new RelationshipRequest(related.getPlayerId(),
                actioning.getPlayerId(), actioning.getName(), relationshipAction, true));
    }

    boolean havePlayerPropertiesBeenUpdated(final BasicProfileInformation player,
                                            final PaymentPreferences paymentPreferences) {
        notNull(player, "player may not be null");

        boolean propertiesUpdated = false;
        final PaymentPreferences currentPaymentPreferences =
                playerService.getPaymentPreferences(player.getPlayerId());
        if (!ObjectUtils.equals(currentPaymentPreferences, paymentPreferences)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating Preferred Currency for {} to {}", player.getPlayerId(),
                        paymentPreferences.toString());
            }
            propertiesUpdated = true;
        }
        return propertiesUpdated;
    }

    @SuppressWarnings("deprecation")
    private LobbySession startNewSession(final PartnerSession partnerSession,
                                         final PlayerProfile userProfile,
                                         final BasicProfileInformation player,
                                         Platform platform, final Map<String, Object> clientContext) {
        if (player == null || player.getPlayerId() == null) {
            throw new IllegalArgumentException("A player with a valid ID must be provided");
        }

        final Session playerSession =
                sessionService.createSession(player, partnerSession.getPartnerId(),
                        partnerSession.getReferrer(), partnerSession.getIpAddress(),
                        userProfile.getEmailAddress(), partnerSession.getPlatform(),
                        partnerSession.getLoginUrl(), clientContext);

        return LobbySession.forSession(playerSession, true, platform, AuthProvider.parseProviderName(userProfile.getProviderName()));
    }

}
