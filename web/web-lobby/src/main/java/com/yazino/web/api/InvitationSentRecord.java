package com.yazino.web.api;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Encapsulates the required details for recording sent invitations.
 * NB. This class's methods *must* not be renamed.
 */
public class InvitationSentRecord {

    private String gameType;
    private String platform;
    private String sourceIds;

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(String sourceIds) {
        this.sourceIds = sourceIds;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    public static InvitationSentRecord toInvitationSentRecord(String game, String platform, String sourceIds) {
        InvitationSentRecord sentRecord = new InvitationSentRecord();
        sentRecord.setGameType(game);
        sentRecord.setPlatform(platform);
        sentRecord.setSourceIds(sourceIds);
        return sentRecord;
    }

}
