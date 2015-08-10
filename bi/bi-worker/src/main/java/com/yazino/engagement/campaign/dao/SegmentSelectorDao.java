package com.yazino.engagement.campaign.dao;

import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.joda.time.DateTime;

import java.util.Set;

public interface SegmentSelectorDao {
    int fetchSegment(String segmentSelectionQuery,
                     DateTime reportTime,
                     BatchVisitor<PlayerWithContent> visitor);

    void updateSegmentDelaysForCampaignRuns(Set<Long> campaignIds, final DateTime now);

}
