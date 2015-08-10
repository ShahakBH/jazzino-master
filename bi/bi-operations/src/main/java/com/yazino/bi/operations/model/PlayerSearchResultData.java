package com.yazino.bi.operations.model;

import com.yazino.platform.player.Gender;

import java.math.BigDecimal;
import java.util.*;

/**
 * Player data needed to display a full data page
 */
public class PlayerSearchResultData extends DashboardParameters implements PlayerData {
    private BigDecimal playerProfileId;
    private String userName;
    private String displayName;
    private String emailAddress;
    private String pictureLocation;
    private Gender gender;
    private BigDecimal accountId;
    private BigDecimal playerId;
    private BigDecimal balance;
    private String externalId;
    private String countryCode;
    private Date creationDate;
    private boolean blocked;
    private Map<String, String> gameLevels;

    private BigDecimal purchasesUsd;
    private BigDecimal purchasesGbp;
    private BigDecimal purchasesEur;
    private BigDecimal purchasesAUD;
    private BigDecimal purchasesCAD;

    private BigDecimal purchasedChips;
    private Date lastPlayed;
    private String provider;
    private boolean verified = true;
    /**
     * Copies the search result data from the player search request source
     *
     * @param request Source request
     */
    public void copyFromRequest(final DashboardParameters request) {
        setQuery(request.getQuery());
        setDashboardToDisplay(request.getDashboardToDisplay());
        setSortOrder(request.getSortOrder());
        setTableDetail(request.getTableDetail());
        setSelectionDate(request.getSelectionDate());
        setFirstRecord(request.getFirstRecord());
        setPageSize(request.getPageSize());
        setSearchBy(request.getSearchBy());
        setSearchValue(request.getSearchValue());
        setFromDate(request.getFromDate());
        setToDate(request.getToDate());
        setGameType(request.getGameType());
        setTable(request.getTable());
        setStatementConsolidation(request.getStatementConsolidation());
        setTransactionType(request.getTransactionType());
//        setProvider(request.getProvider());
        setReference(request.getReference());
        setExternalTransactionId(request.getExternalTransactionId());
    }

    /**
     * Gets the query string for changing the actual dashboard (reinitializing the default parameters)
     *
     * @param dashboardLink Dashboard to set
     * @return String of query parameters
     */
    public String getDashboardLink(final String dashboardLink) {
        final StringBuilder retval = getDashboardParameterString(dashboardLink);
        return retval.toString();
    }

    /**
     * Extracts the query parameters including the dashboard information
     *
     * @param dashboardLink Dashboard info to add
     * @return URL query parameters string
     */
    private StringBuilder getDashboardParameterString(final String dashboardLink) {
        final StringBuilder retval = getBasicParameterString();
        retval.append("&dashboardToDisplay=");
        retval.append(dashboardLink);
        return retval;
    }

    /**
     * Extracts the basic query parameters string from the actual object
     *
     * @return URL query parameters string
     */
    private StringBuilder getBasicParameterString() {
        final StringBuilder retval = new StringBuilder();
        retval.append("?query=");
        retval.append(getPlayerId());
        retval.append("&pageSize=");
        retval.append(getPageSize());
        return retval;
    }

    /**
     * Extracts the query parameters including the majority of the available information for this object
     *
     * @return URL query parameters string
     */
    private StringBuilder getLongParameterString() {
        final StringBuilder retval = getDashboardParameterString(getDashboardToDisplay().toString());
        if (getSelectionDate() != null) {
            retval.append("&selectionDate=");
            retval.append(getDateFormat().format(getSelectionDate()));
        }
        if (getTableDetail() != null) {
            retval.append("&tableDetail=");
            retval.append(getTableDetail());
        }
        if (getFromDate() != null) {
            retval.append("&fromDate=");
            retval.append(getDateFormat().format(getFromDate()));
        }
        if (getToDate() != null) {
            retval.append("&toDate=");
            retval.append(getDateFormat().format(getToDate()));
        }
        if (getTable() != null) {
            retval.append("&table=");
            retval.append(getTable());
        }
        if (getGameType() != null) {
            retval.append("&gameType=");
            retval.append(getGameType());
        }
        if (getStatementConsolidation() == null || !getStatementConsolidation().contains("1")) {
            retval.append("&_statementConsolidation=on");
        } else {
            retval.append("&statementConsolidation=1&_statementConsolidation=on");
        }
        if (getProvider() != null) {
            retval.append("&provider=");
            retval.append(getProvider());
        }
        if (getReference() != null) {
            retval.append("&reference=");
            retval.append(getReference());
        }
        if (getExternalId() != null) {
            retval.append("&externalId=");
            retval.append(getExternalId());
        }
        return retval;
    }

    /**
     * Gets the query string for switching to another page
     *
     * @param page New page start record number
     * @return String of query parameters
     */
    public String getPageLink(final Integer page) {
        final StringBuilder retval = getLongParameterString();
        retval.append("&firstRecord=");
        retval.append(page);
        retval.append("&pagedRequest=true");
        if (getSortOrder() != null) {
            retval.append("&sortOrder=");
            retval.append(getSortOrder());
        }
        return retval.toString();
    }

    /**
     * Gets the query string for sorting a column
     *
     * @param sort Sort order
     * @return String of query parameters
     */
    public String getSortLink(final String sort) {
        final StringBuilder retval = getLongParameterString();
        retval.append("&sortOrder=");
        retval.append(sort);
        if (getFirstRecord() != null) {
            retval.append("&firstRecord=");
            retval.append(getFirstRecord());
        }
        return retval.toString();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Gets the name of the country matching the actual code
     *
     * @return User-readable country name
     */
    public String getCountryName() {
        if (countryCode == null) {
            return null;
        }
        for (final Locale loc : Locale.getAvailableLocales()) {
            if (countryCode.equalsIgnoreCase(loc.getCountry())) {
                return loc.getDisplayCountry();
            }
        }
        return null;
    }

    public void setCountryCode(final String countryCode) {
        if (countryCode == null) {
            this.countryCode = null;
        } else {
            this.countryCode = countryCode.toUpperCase();
        }
    }

    public Map<String, String> getGameLevels() {
        return gameLevels;
    }

    /**
     * Set the game levels for the player.
     * @param gameLevels  Map of GameType --> Level
     */
    public void setGameLevels(final Map<String, String> gameLevels) {
        this.gameLevels = gameLevels;
    }

    public BigDecimal getPurchasesUsd() {
        return purchasesUsd;
    }

    public void setPurchasesUsd(final BigDecimal purchasesUsd) {
        this.purchasesUsd = purchasesUsd;
    }

    public BigDecimal getPurchasesGbp() {
        return purchasesGbp;
    }

    public void setPurchasesGbp(final BigDecimal purchasesGbp) {
        this.purchasesGbp = purchasesGbp;
    }

    public BigDecimal getPurchasesEur() {
        return purchasesEur;
    }

    public void setPurchasesEur(final BigDecimal purchasesEur) {
        this.purchasesEur = purchasesEur;
    }

    public BigDecimal getPurchasedChips() {
        return purchasedChips;
    }

    public BigDecimal getPurchasesAUD() {
        return purchasesAUD;
    }

    public void setPurchasesAUD(BigDecimal purchasesAUD) {
        this.purchasesAUD = purchasesAUD;
    }

    public BigDecimal getPurchasesCAD() {
        return purchasesCAD;
    }

    public void setPurchasesCAD(BigDecimal purchasesCAD) {
        this.purchasesCAD = purchasesCAD;
    }

    public void setPurchasedChips(final BigDecimal purchasedChips) {
        this.purchasedChips = purchasedChips;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(final Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    @Override
    public BigDecimal getPlayerProfileId() {
        return playerProfileId;
    }

    @Override
    public void setPlayerProfileId(final BigDecimal userId) {
        this.playerProfileId = userId;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String getPictureLocation() {
        return pictureLocation;
    }

    @Override
    public void setPictureLocation(final String pictureLocation) {
        this.pictureLocation = pictureLocation;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public void setProvider(final String provider) {
        this.provider = provider;
    }

    @Override
    public boolean isVerified() {
        return verified;
    }

    @Override
    public void setVerified(final boolean verified) {
        this.verified = verified;
    }

    @Override
    public BigDecimal getPlayerId() {
        return playerId;
    }

    @Override
    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(final boolean blocked) {
        this.blocked = blocked;
    }

}
