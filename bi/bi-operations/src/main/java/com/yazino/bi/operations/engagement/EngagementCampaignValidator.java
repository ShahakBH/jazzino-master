package com.yazino.bi.operations.engagement;

import com.yazino.android.GoogleCloudMessagingConstants;
import com.yazino.engagement.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import com.yazino.bi.operations.view.DateFormat;

@Service
public class EngagementCampaignValidator implements Validator {

    public static final int DEFAULT_REQUEST_MESSAGE_CHAR_LIMIT = 100;
    public static final int NOTIFICATION_MESSAGE_CHAR_LIMIT = 180;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormat.forPattern(DateFormat.DEFAULT);

    @Override
    public boolean supports(final Class<?> aClass) {
        return EngagementCampaign.class.equals(aClass);
    }

    @Override
    public void validate(final Object o, final Errors errors) {
        final EngagementCampaign campaign = (EngagementCampaign) o;

        if (StringUtils.isBlank(campaign.getTitle())) {
            errors.rejectValue("title", "empty", "Title can not be blank");
        }
        if (StringUtils.isBlank(campaign.getDescription())) {
            errors.rejectValue("description", "empty", "Description can not be blank");
        }
        if (StringUtils.isBlank(campaign.getMessage())) {
            errors.rejectValue("message", "empty", "Message can not be blank");
        } else {

            Integer messageCharLimit = null;
            if (campaign.getChannelType() == ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION) {
                messageCharLimit = NOTIFICATION_MESSAGE_CHAR_LIMIT;
            } else {
                messageCharLimit = DEFAULT_REQUEST_MESSAGE_CHAR_LIMIT;
            }
            if ((campaign.getMessage().length() > messageCharLimit)) {
                errors.rejectValue("message", "empty", "Message Is over " + messageCharLimit + " Characters");
            }
        }

        if (campaign.getChannelType() == null) {
            errors.rejectValue("message", "empty", "Must Choose Channel Type");
        }

        validateDates(campaign, errors);
    }

    private void validateDates(EngagementCampaign campaign, Errors errors) {
        DateTime scheduled = campaign.getScheduled();
        DateTime expires = campaign.getExpires();
        if (scheduled == null || expires == null) {
            return;
        }

        if (!expires.isAfter(scheduled)) {
            errors.rejectValue("expires", "expires.not.after.scheduled", "Expiry date must be after the scheduled date");
        }
        if (campaign.getChannelType() == ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID) {
            DateTime maxExpires = scheduled.plusSeconds(GoogleCloudMessagingConstants.MAX_SECONDS_TO_LIVE);
            if (expires.isAfter(maxExpires)) {
                errors.rejectValue("expires", "", "Expiry date must be no more than 4 weeks after the scheduled date (i.e. "
                        + DATE_TIME.print(maxExpires) + ")");
            }
        }
    }
}
