package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.model.CampaignPlayerUpload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class CampaignPlayerUploadValidator implements Validator {
    @Override
    public boolean supports(final Class<?> clazz) {
        return CampaignPlayerUpload.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        CampaignPlayerUpload campaignPlayerUpload = (CampaignPlayerUpload) target;

        validateCampaignPlayerUpload(errors, campaignPlayerUpload);
    }

    private void validateCampaignPlayerUpload(final Errors errors, final CampaignPlayerUpload campaignPlayerUpload) {
        if (StringUtils.isBlank(campaignPlayerUpload.getFile().getOriginalFilename())) {
            errors.rejectValue("file", "file.empty", "The Filename is blank or empty");
        }
    }
}
