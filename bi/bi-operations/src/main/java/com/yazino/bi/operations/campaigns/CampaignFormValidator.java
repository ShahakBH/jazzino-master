package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.engagement.ChannelType;
import com.yazino.promotions.BuyChipsForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static strata.server.lobby.api.promotion.PromotionType.BUY_CHIPS;
import static strata.server.lobby.api.promotion.PromotionType.DAILY_AWARD;

@Component
public class CampaignFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CampaignForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        CampaignForm campaignForm = (CampaignForm) target;

        validateCampaign(campaignForm, errors);
    }

    private void validateBuyChipsForm(BuyChipsForm buyChipsForm, Errors errors) {

        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm", "buyChipsForm.empty", "BuyChipsForm is Empty");

        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm.priority", "priority.empty", "Priority is Empty");
        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm.validForHours", "validForHours.empty", "Valid For field is Empty");
        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm.maxRewards", "maxRewards.empty", "Up to field is Empty");
        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm.platforms", "platforms.empty", "Select at least one platform");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "buyChipsForm.inGameNotificationHeader", "inGameNotificationHeader.empty",
                "On site Message Header is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                "buyChipsForm.inGameNotificationMsg",
                "inGameNotificationMsg.empty",
                "Notification Message is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                "buyChipsForm.rolloverHeader",
                "rolloverHeader.empty",
                "Buy Chips Header is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "buyChipsForm.rolloverText", "rolloverText.empty", "Buy Chips Message is Empty");
        ValidationUtils.rejectIfEmpty(errors, "buyChipsForm.chipsPackagePercentages", "chipsPackagePercentages.empty",
                "Chips Package Percentages is empty. Something has gone horribly wrong");

        if (buyChipsForm != null && buyChipsForm.getValidForHours() != null && buyChipsForm.getValidForHours() <= 0) {
            errors.rejectValue("buyChipsForm.validForHours", "validForHours.notValid", "Valid For field should be greater than zero");
        }
    }

    private void validateCampaign(CampaignForm campaignForm, Errors errors) {

        Campaign campaign = campaignForm.getCampaign();

        ValidationUtils.rejectIfEmpty(errors, "campaign", "campaign.empty", "Campaign is Empty");
        if (campaign == null) {
            return;
        }
        ValidationUtils.rejectIfEmpty(errors, "campaign.campaignScheduleWithName.campaignId", "campaignId.empty", "CampaignId is Empty");
        ValidationUtils.rejectIfEmpty(errors,
                "campaign.campaignScheduleWithName.nextRunTsAsDate",
                "nextRunTs.empty",
                "Next Run Time is Empty");
        ValidationUtils.rejectIfEmpty(errors, "campaign.campaignScheduleWithName.runHours", "runHours.empty", "Run Hours is Empty");
        ValidationUtils.rejectIfEmpty(errors, "campaign.campaignScheduleWithName.runMinutes", "runMinutes.empty", "Run Minutes is Empty");
        ValidationUtils.rejectIfEmpty(errors, "campaign.campaignScheduleWithName.endTimeAsDate", "endTime.empty", "End Time is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "campaign.campaignScheduleWithName.name", "name.empty", "Name is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "campaign.sqlQuery", "sqlQuery.empty", "Sql Query is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "campaign.content", "content.empty", "Content is Empty");

        validateCampaignGameTypes(campaignForm, errors);
        validateContentFields(campaign, errors);


        if (campaign.getSqlQuery() != null && containsNaughtySql(campaign.getSqlQuery())) {
            errors.rejectValue("campaign.sqlQuery", "sqlQuery.notValid", "select must start with select and not have naughty sql in it");
        }
        if (campaign.getCampaignScheduleWithName().getRunHours() != null && campaign.getCampaignScheduleWithName().getRunHours() < 0) {
            errors.rejectValue("campaign.campaignScheduleWithName.runHours", "runHours.notValid", "Run Hours cannot be less than zero");
        }

        if (campaign.getCampaignScheduleWithName().getRunMinutes() != null && campaign.getCampaignScheduleWithName().getRunMinutes() < 0) {
            errors.rejectValue("campaign.campaignScheduleWithName.runMinutes",
                    "runMinutes.notValid",
                    "Run Minutes cannot be less than zero");
        }

        if (campaign.isDelayNotifications() && campaign.getCampaignScheduleWithName().getRunHours() != null
                && campaign.getCampaignScheduleWithName().getRunHours() < 24) {
            errors.rejectValue("campaign.campaignScheduleWithName.runHours", "runHours.notValid",
                    "cannot have delayed notification campaign which runs <24 hours");
        }

        if (!(campaign.isPromo())) {
            //Check for campaign channels only if it is not a promotion
            ValidationUtils.rejectIfEmpty(errors, "campaign.channels", "channels.empty", "Tick at least one notification Channel");
        }

        if (BUY_CHIPS.name().equals(campaignForm.getPromotionType())) {
            validateBuyChipsForm(campaignForm.getBuyChipsForm(), errors);
        }

        if (DAILY_AWARD.name().equals(campaignForm.getPromotionType())) {
            validateDailyAwardForm(errors);
        }
    }

    private void validateCampaignGameTypes(final CampaignForm campaignForm, Errors errors) {
        final List<String> gameTypes = campaignForm.getCampaign().getGameTypes();
        if (gameTypes == null || gameTypes.size() == 0) {
            errors.rejectValue("campaign.gameTypes", "campaign.gameTypes.error", "Tick at least one game type");
        }
    }

    private void validateContentFields(final Campaign campaign, final Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "campaign.content[message]", "content.message.empty", "Message Content is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                "campaign.content[description]",
                "content.description.empty",
                "Description Content is Empty");
        if (campaign.getContent() != null) {

            parseContentTags(campaign, "message", errors);
            parseContentTags(campaign, "description", errors);
        }

        if (campaign.getChannels() != null) {
            if (campaign.getChannels().contains(ChannelType.EMAIL)) {
                ValidationUtils.rejectIfEmptyOrWhitespace(
                        errors, "campaign.channelConfig[TEMPLATE]", "channelConfig.template.empty", "Email Template can not be Empty");
            }
        }
    }

    private List<String> parseContentTags(final Campaign campaign, String field, final Errors errors) {
        List<String> result = newArrayList();
        List<String> missingTags = newArrayList();
        final String messageText = campaign.getContent().get(field);
        if (!isBlank(messageText)) {
            final Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(messageText);
            while (matcher.find()) {
                final String placeholder = matcher.group(0);
                result.add(placeholder.substring(1, placeholder.length() - 1));
            }
        }

        final String sqlQuery;
        if (campaign.getSqlQuery() == null) {
            sqlQuery = "";
        } else {
            sqlQuery = campaign.getSqlQuery().toUpperCase().split("from")[0];
        }
        for (String contentTag : result) {
            if (contentTag.equals("PROGRESSIVE") || contentTag.equals("DISPLAY_NAME")) {
                continue;
            }

            if (!sqlQuery.contains(contentTag)) {
                missingTags.add(contentTag);

            }
            if (!contentTag.toUpperCase().equals(contentTag)) {
                errors.rejectValue(format("campaign.content[%s]", field),
                        "content.description.lowercase",
                        "content tags must be upper case:" + contentTag);
            }
        }

        if (missingTags.size() > 0) {
            errors.rejectValue(format("campaign.content[%s]", field),
                    "content.description.invalid",
                    "SQL does not contain column matching content tags:" + join(missingTags, ", "));
        }
        return result;
    }

    boolean containsNaughtySql(final String sqlQuery) {
        final String sql = sqlQuery.toLowerCase().trim();
        return !sql.startsWith("select ")
                || sql.contains(";")
                || sql.contains("delete")
                || sql.contains("update")
                || sql.contains("drop")
                || sql.contains("truncate")
                || sql.contains("alter")
                || sql.contains("exit")
                || sql.contains("replace");
    }

    private void validateDailyAwardForm(final Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "dailyAwardForm", "dailyAwardForm.empty", "DailyAwardForm is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dailyAwardForm.topUpAmount", "topUpAmount.empty", "topUpAmount is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dailyAwardForm.priority", "priority.empty", "Priority is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                "dailyAwardForm.validForHours",
                "validForHours.empty",
                "Valid For field is Empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dailyAwardForm.maxRewards", "maxRewards.empty", "Up to field is Empty");
        ValidationUtils.rejectIfEmpty(errors, "dailyAwardForm.platforms", "platforms.empty", "Select at least one platform");

    }
}
