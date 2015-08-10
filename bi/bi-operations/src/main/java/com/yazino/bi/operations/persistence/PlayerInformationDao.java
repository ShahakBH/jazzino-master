package com.yazino.bi.operations.persistence;

import com.yazino.bi.operations.model.Dashboard;
import com.yazino.bi.operations.model.PaymentDashboardSearchCriteria;

import java.math.BigDecimal;
import java.util.Date;

/**
 * DAO used to search for player information
 */
public interface PlayerInformationDao {

    /**
     * Retrieves the payment dashboard from the database
     *
     * @param searchCriteria the search criteria.
     * @param sortOrder      Sort order to apply to this dashboard
     * @param firstRecord    First record to display (for pagination)
     * @param pageSize       Display page size
     */
    Dashboard getPaymentDashboard(PaymentDashboardSearchCriteria searchCriteria, String sortOrder,
                                  Integer firstRecord,
                                  Integer pageSize);

    /**
     * Retrieves the game dashboard from the database
     *
     * @param accountId   account to get the dashboard for
     * @param sortOrder   Sort order to apply to this dashboard
     * @param firstRecord First record to display (for pagination)
     * @param pageSize    Display page size
     * @param dateFrom    Lower limit for date search
     * @param dateTo      Higher (inclusive) limite for date search
     * @param tableId       Table to look for
     * @param gameType    Game to limit the search to
     */
    Dashboard getGameDashboard(BigDecimal accountId, String sortOrder, Integer firstRecord, Integer pageSize,
                               Date dateFrom, Date dateTo, BigDecimal tableId, String gameType);

    /**
     * Retrieves the game dashboard from the database
     *
     * @param accountId     account to get the dashboard for
     * @param sortOrder     Sort order to apply to this dashboard
     * @param tableSelected Table for which we want to see the details
     * @param firstRecord   First record to display (for pagination)
     * @param pageSize      Display page size
     */
    Dashboard getGameDetails(BigDecimal accountId, String sortOrder, String tableSelected,
                             Integer firstRecord, Integer pageSize);

    /**
     * Retrieves the statement dashboard from the database
     *
     * @param accountId   account to get the dashboard for
     * @param sortOrder   Sort order to apply to this dashboard
     * @param firstRecord First record to display (for pagination)
     * @param pageSize    Display page size
     * @param dateFrom    Lower limit for date search
     * @param dateTo      Higher (inclusive) limite for date search
     */
    Dashboard getStatementDashboard(BigDecimal accountId, String sortOrder, Integer firstRecord,
                                    Integer pageSize, Date dateFrom, Date dateTo);

    /**
     * Retrieves the statement dashboard from the database
     *
     * @param accountId       account to get the dashboard for
     * @param sortOrder       Sort order to apply to this dashboard
     * @param firstRecord     First record to display (for pagination)
     * @param pageSize        Display page size
     * @param dateFrom        Lower limit for date search
     * @param dateTo          Higher (inclusive) limite for date search
     * @param tableId           Table to look for
     * @param gameType        Game to limit the search to
     * @param transactionType Transaction type to search for
     */
    Dashboard getStatementDetails(BigDecimal accountId,
                                  String sortOrder,
                                  Integer firstRecord,
                                  Integer pageSize,
                                  Date dateFrom,
                                  Date dateTo,
                                  BigDecimal tableId,
                                  String gameType,
                                  String transactionType);

    /**
     * Returns the player ID matching the rpx/ext pair, ignoring players with registration errors.
     *
     * @param rpx        RPX ID
     * @param externalId External ID or user ID for Yazino
     * @return Player ID matching
     * @throws TooManyPlayersMatchedToExternalIdException
     *          if the externalId maps onto several players
     */
    BigDecimal getPlayerId(String rpx, String externalId);
}
