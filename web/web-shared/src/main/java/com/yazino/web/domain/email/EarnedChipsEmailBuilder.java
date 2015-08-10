package com.yazino.web.domain.email;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Map;

/**
 * Build an {@link EmailRequest} for notifying players of earned chips.
 */
public class EarnedChipsEmailBuilder extends AbstractEmailBuilder {
    static final String TEMPLATE_NAME = "ConfirmationOfEarnedChipsEmail.vm";
    static final String SUBJECT_TEMPLATE = "Thanks for contributing, %s!";
    private static final String FROM_ADDRESS = "Yazino <contact@yazino.com>";

    public EarnedChipsEmailBuilder(final BigDecimal playerId,
                                   final String identifier,
                                   final BigDecimal chips) {
        final String earnedChipsAsString = NumberFormat.getIntegerInstance().format(chips);
        setTemplateProperty("earnedChips", earnedChipsAsString);
        setTemplateProperty("earnedId", identifier);
        withPlayerId(playerId);
    }

    public String getEarnedChips() {
        return (String) getTemplateProperty("earnedChips");
    }

    public String getIdentifier() {
        return (String) getTemplateProperty("earnedId");
    }

    @Override
    public EmailRequest buildRequest(final PlayerProfileService profileService) {
        final PlayerProfile profile = profileService.findByPlayerId(getPlayerId());
        final String name;
        if (profile.getFirstName() != null) {
            name = profile.getFirstName();
        } else {
            name = profile.getDisplayName();
        }
        final String subject = String.format(SUBJECT_TEMPLATE, name);
        final Map<String, Object> properties = getTemplateProperties();
        properties.put("playerFirstName", name);
        return new EmailRequest(TEMPLATE_NAME, subject, FROM_ADDRESS, properties, profile.getEmailAddress());
    }
}
