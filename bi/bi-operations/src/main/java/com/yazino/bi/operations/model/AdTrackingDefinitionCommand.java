package com.yazino.bi.operations.model;

import static com.yazino.bi.operations.util.DateIntervalHelper.*;

import java.util.Date;

/**
 * Bean defining the ad tracking report's source data
 */
public class AdTrackingDefinitionCommand extends CommandWithPlatformAndFormat {
    private Date playerRegistrationStart;
    private Date playerRegistrationEnd;
    private Date purchasesStart;
    private Date purchasesEnd;

    public Date getPlayerRegistrationStart() {
        return getDateStart(playerRegistrationStart);
    }

    public void setPlayerRegistrationStart(final Date playerRegistrationStart) {
        this.playerRegistrationStart = playerRegistrationStart;
    }

    public Date getPlayerRegistrationEnd() {
        return getDateEnd(playerRegistrationEnd);
    }

    public void setPlayerRegistrationEnd(final Date playerRegistrationEnd) {
        this.playerRegistrationEnd = playerRegistrationEnd;
    }

    public Date getPurchasesStart() {
        return getDateStart(purchasesStart);
    }

    public void setPurchasesStart(final Date purchasesStart) {
        this.purchasesStart = purchasesStart;
    }

    public Date getPurchasesEnd() {
        return getDateEnd(purchasesEnd);
    }

    public void setPurchasesEnd(final Date purchasesEnd) {
        this.purchasesEnd = purchasesEnd;
    }
}
