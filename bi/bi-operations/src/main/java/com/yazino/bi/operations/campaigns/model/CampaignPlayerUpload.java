package com.yazino.bi.operations.campaigns.model;

import org.springframework.web.multipart.MultipartFile;

public class CampaignPlayerUpload {
    private Long campaignId;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(final Long campaignId) {
        this.campaignId = campaignId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    private MultipartFile file;
}
