package com.yazino.web.domain.email;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;

import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Builds a {@link com.yazino.web.domain.email.EmailRequest} for sending invites to friends.
 */
public class ChallengeBuddiesEmailBuilder extends AbstractEmailBuilder implements ToFriendEmailBuilder {

    static final String CHALLENGE_BUDDY_TEMPLATE = "challenge-buddies.vm";
    static final String CHALLENGE_BUDDY_SUBJECT_TEMPLATE = "%s has challenged you to play at Yazino";
    static final String USER_NAME_KEY = "userName";
    static final String FRIENDS_EMAIL_KEY = "friendsEmail";
    static final String TARGET_URL_KEY = "targetUrl";
    static final String REFERRAL_URL_KEY = "referralId";
    private final String fromAddress;

    public ChallengeBuddiesEmailBuilder(final String[] referralUrl,
                                        final BigDecimal playerId,
                                        final String gameType,
                                        final String fromAddress) {
        notNull(fromAddress);
        this.fromAddress = fromAddress;
        setTemplateProperty(TARGET_URL_KEY, String.format(referralUrl[0] + referralUrl[1], playerId));
        setTemplateProperty(REFERRAL_URL_KEY, referralUrl[1]);
        setTemplateProperty("gameType", gameType);
        withPlayerId(playerId);
    }


    public ChallengeBuddiesEmailBuilder withFriendEmailAddress(final String friendEmail) {
        setOtherProperty(FRIENDS_EMAIL_KEY, friendEmail);
        return this;
    }

    @Override
    public EmailRequest buildRequest(final PlayerProfileService profileService) {
        final PlayerProfile profile = profileService.findByPlayerId(getPlayerId());
        final String subject = String.format(CHALLENGE_BUDDY_SUBJECT_TEMPLATE, profile.getDisplayName());
        final String friendsEmail = (String) getOtherProperty(FRIENDS_EMAIL_KEY);
        final Map<String, Object> properties = getTemplateProperties();
        properties.put(USER_NAME_KEY, profile.getDisplayName());
        return new EmailRequest(CHALLENGE_BUDDY_TEMPLATE, subject, formattedEmailWithName(profile.getDisplayName(), fromAddress), properties, friendsEmail);
    }

}
