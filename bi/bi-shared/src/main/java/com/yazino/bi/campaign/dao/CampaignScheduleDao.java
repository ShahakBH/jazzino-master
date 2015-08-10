package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignSchedule;
import org.joda.time.DateTime;

import java.util.List;

public interface CampaignScheduleDao {
    void updateNextRunTs(Long campaignId, DateTime nextRunTs);

    List<CampaignSchedule> getDueCampaigns(DateTime currentTimestamp);

    void save(CampaignSchedule campaignSchedule);

    void update(CampaignSchedule campaignSchedule);

    CampaignSchedule getCampaignSchedule(Long campaignId);
}
