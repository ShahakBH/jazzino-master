package com.yazino.engagement.campaign.reporting.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

public class CampaignRunAuditMessageTest {
    @Test
    public void CampaignRunAuditMessageShouldSerialize(){
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(mapper.canSerialize(CampaignRunAuditMessage.class));
    }

    @Test
    public void CampaignRunAuditMessageShouldSerializeAndDeserialise() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        final CampaignRunAuditMessage underTest = new CampaignRunAuditMessage(1l, 2l, "name", 10, new DateTime(), 345l, "status", "message");
        assertTrue(mapper.canSerialize(CampaignRunAuditMessage.class));

        final CampaignRunAuditMessage campaignRunAuditMessage = mapper.readValue(mapper.writeValueAsString(underTest), CampaignRunAuditMessage.class);
        assertThat(campaignRunAuditMessage.getCampaignRunId(), equalTo(underTest.getCampaignRunId()));
        assertThat(campaignRunAuditMessage.getCampaignId(), equalTo(underTest.getCampaignId()));
        assertThat(campaignRunAuditMessage.getName(), equalTo(underTest.getName()));
        assertThat(campaignRunAuditMessage.getSize(), equalTo(underTest.getSize()));
        assertThat(campaignRunAuditMessage.getStatus(), equalTo(underTest.getStatus()));
        assertThat(campaignRunAuditMessage.getMessage(), equalTo(underTest.getMessage()));
        assertThat(campaignRunAuditMessage.getPromoId(), equalTo(underTest.getPromoId()));
    }
}
