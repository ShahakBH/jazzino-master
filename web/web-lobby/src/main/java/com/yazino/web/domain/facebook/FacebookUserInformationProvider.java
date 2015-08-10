package com.yazino.web.domain.facebook;

import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;
import com.restfb.types.User;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.player.*;
import com.yazino.web.service.GeolocationLookup;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("facebookUserInformationProvider")
public class FacebookUserInformationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookUserInformationProvider.class);

    private static final String PROVIDER_NAME = "facebook";
    private static final String PROPERTY_PARTNER_ID = "strata.lobby.partnerid";
    private static final String DEFAULT_PARTNER_ID = "YAZINO";

    private final FacebookClientFactory clientFactory;
    private final GeolocationLookup geolocationLookup;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public FacebookUserInformationProvider(
            @Qualifier("facebookClientFactory") final FacebookClientFactory clientFactory,
            @Qualifier("geolocationLookup") final GeolocationLookup geolocationLookup,
            final YazinoConfiguration yazinoConfiguration) {
        notNull(clientFactory, "clientFactory is null");
        notNull(geolocationLookup, "geolocation is null");
        notNull(yazinoConfiguration, "yazinoConfiguration is null");

        this.geolocationLookup = geolocationLookup;
        this.clientFactory = clientFactory;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public PlayerInformationHolder getUserInformationHolder(final String accessToken,
                                                            final String requestIds,
                                                            String remoteIpAddress,
                                                            final boolean canvasActionsAllowed) {
        notBlank(accessToken, "accessToken is blank");
        notNull(remoteIpAddress, "remoteIpAddress is null");

        final PlayerInformationHolder holder = new PlayerInformationHolder();

        final FacebookClient facebookClient = clientFactory.getClient(accessToken);
        PlayerProfile facebookProfile = null;
        try {
            holder.setSessionKey(accessToken);
            final FacebookUser user = facebookClient.fetchObject("me", FacebookUser.class);
            if (user == null) {
                LOG.warn("Facebook client returned a null response for 'me'");

            } else {
                facebookProfile = buildFacebookProfile(user, requestIds, remoteIpAddress, facebookClient);
                if (facebookProfile != null) {
                    holder.setReferralRecipient(new FacebookReferralRecipient(facebookProfile.getExternalId()));
                }
                holder.setAvatarUrl("https://graph.facebook.com/" + user.getId() + "/picture");

                if (canvasActionsAllowed) {
                    holder.setFriends(resolveFriends(facebookClient));
                }
            }

        } catch (final FacebookException e) {
            logFacebookExceptionFor("process login", e);
            facebookProfile = null;
        }
        holder.setPlayerProfile(facebookProfile);

        return holder;
    }

    private PlayerProfile buildFacebookProfile(final User user,
                                               final String requestIds,
                                               final String remoteIpAddress,
                                               final FacebookClient facebookClient) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Facebook user: {}", ReflectionToStringBuilder.reflectionToString(user));
        }

        final String externalId = user.getId();
        if (externalId == null) {
            throw new IllegalStateException("Facebook user does not have an ID: " + user);
        }

        final String displayName = defaultIfNull(user.getName(), externalId);
        final String firstName = user.getFirstName();
        final String lastName = user.getLastName();
        final Gender gender = parseGender(user);
        final DateTime dateOfBirth = new FacebookDateParser().parseDate(user.getBirthday());

        String country = geolocationLookup.lookupCountryCodeByIp(remoteIpAddress);

        if (StringUtils.isBlank(country)) {
            country = new FacebookLocaleParser().parseCountry(user.getLocale());
            LOG.warn("geolocationLookup failing, falling back to FB, country: {}", country);
        }
        final String referralString = referralPlayerIdFrom(requestIds, facebookClient);
        PlayerProfile profile = new PlayerProfile(user.getEmail(), displayName, displayName, gender, country, firstName, lastName,
                dateOfBirth, referralString, PROVIDER_NAME, PROVIDER_NAME, externalId, true);
        profile.setGuestStatus(GuestStatus.NON_GUEST); // TODO refactor - Only Yazino accounts can be guest accounts. We should not really be
        profile.setPartnerId(Partner.parse(yazinoConfiguration.getString(PROPERTY_PARTNER_ID, DEFAULT_PARTNER_ID)));
        // using PlayerProfile to return information from Facebook as these sort of mismatches result...
        return profile;
    }

    private String referralPlayerIdFrom(final String requestIds,
                                        final FacebookClient facebookClient) {
        String referralPlayerId = null;
        if (requestIds != null) {
            final String requestId = requestIds.split(",")[0];
            referralPlayerId = getReferralPlayerIdFromFacebookRequest(facebookClient, requestId);
            deleteFacebookRequest(facebookClient, requestId);
        }
        return referralPlayerId;
    }

    private String getReferralPlayerIdFromFacebookRequest(final FacebookClient facebookClient,
                                                          final String requestId) {
        try {
            final JsonObject appRequest = facebookClient.fetchObject(requestId, JsonObject.class);
            if (appRequest != null) {
                return appRequest.getString("data");
            }
        } catch (final Exception e) {
            LOG.debug("Could not retrieve the app request: {}", requestId, e);
        }
        return null;
    }

    private void deleteFacebookRequest(final FacebookClient facebookClient, final String requestId) {
        if (requestId != null) {
            try {
                facebookClient.deleteObject(requestId);
            } catch (final FacebookException e) {
                LOG.debug("Could not delete app request: {}", requestId, e);
            }
        }
    }

    private Gender parseGender(final User user) {
        final String gender = user.getGender();
        if ("female".equals(gender)) {
            return Gender.FEMALE;
        }
        if ("male".equals(gender)) {
            return Gender.MALE;
        }
        return null;
    }

    private Set<String> resolveFriends(final FacebookClient facebookClient) {
        final Set<String> result = new HashSet<>();
        try {
            final Connection<User> friends = facebookClient.fetchConnection("me/friends", User.class);
            if (friends != null) {
                final List<User> data = friends.getData();
                if (data != null) {
                    LOG.debug("Received {} friends", data.size());
                    for (final User friend : data) {
                        result.add(friend.getId());
                    }
                }

            } else {
                LOG.error("Facebook client returned a null response for 'me/friends'");
            }

        } catch (final FacebookException e) {
            logFacebookExceptionFor("retrieve Facebook user's friends", e);
        }

        return result;
    }

    private void logFacebookExceptionFor(final String action,
                                         final FacebookException e) {
        if (e.getMessage().contains("has been invalidated because the user has changed the password")) {
            LOG.info("Failed to {} due to session invalidation by password change", action);

        } else if (e.getMessage().contains("Session has expired")) {
            LOG.info("Failed to {} due to session expiration", action);

        } else if (e.getMessage().contains("Session does not match current stored session")) {
            LOG.info("Failed to {} due to mismatched session", action);

        } else if (e.getMessage().contains("follow the instructions")) {
            LOG.info("Failed to {} due to user action required", action);

        } else if (e.getMessage().contains("user is not a confirmed user")) {
            LOG.info("Failed to {} due to user being unconfirmed", action);

        } else if (e.getMessage().contains("not authorized application")) {
            LOG.info("Failed to {} due to user not authorising app", action);

        } else if (e.getMessage().contains("Please retry your request later")) {
            LOG.info("Failed to {} Facebook Internal Server error, client should retry request", action);
        } else {
            LOG.error("Failed to {}", action, e);
        }
    }
}
