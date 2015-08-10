package com.yazino.bi.operations.persistence;

import com.yazino.bi.operations.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

/**
 * Postgres implementation of the player information queries
 */
@Repository
public class PostgresPlayerInformationDao implements PlayerInformationDao {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresPlayerInformationDao.class);

    private static final Map<String, String> PAYMENT_DASHBOARD_FIELDS = new LinkedHashMap<String, String>();
    private static final Map<String, String> PAYMENT_DASHBOARD_FIELD_TYPES = new LinkedHashMap<String, String>();

    protected static final String SLOTS_BONUS_FIELD = "";
    private static final String EXTERNAL_PLAYER_ID_SEARCH = "SELECT PLAYER_ID FROM LOBBY_USER WHERE "
            + "UPPER(PROVIDER_NAME) = UPPER(?) AND UPPER(EXTERNAL_ID) =UPPER(?)";
    private static final String INTERNAL_PLAYER_ID_SEARCH = "SELECT PLAYER_ID FROM LOBBY_USER WHERE PLAYER_ID = ?";
    protected static final String TABLE = "Table";
    protected static final String GAME = "Game";
    protected static final String GAME_TYPE = "Game Type";
    protected static final String TABLE_NAME = "Table Name";
    protected static final String STAKE = "Stake";
    protected static final String RETURN = "Return";
    protected static final String WIN_LOSS = "Win/Loss";
    protected static final String APPROX_LAST_BALANCE = "Approx Last Balance";
    protected static final String DATE_TIME = "Date/Time";

    private final JdbcTemplate jdbcTemplate;

    static {
        PAYMENT_DASHBOARD_FIELDS.put(DATE_TIME, "MESSAGE_TS");
        PAYMENT_DASHBOARD_FIELDS.put("Provider", "TRANSACTION_TYPE");
        PAYMENT_DASHBOARD_FIELDS.put("Payment method", "CREDIT_CARD_NUMBER");
        PAYMENT_DASHBOARD_FIELDS.put("Status", "EXTERNAL_TRANSACTION_STATUS");
        PAYMENT_DASHBOARD_FIELDS.put("Amount", "AMOUNT");
        PAYMENT_DASHBOARD_FIELDS.put("Currency", "CURRENCY_CODE");
        PAYMENT_DASHBOARD_FIELDS.put("Chips", "AMOUNT_CHIPS");
        PAYMENT_DASHBOARD_FIELDS.put("Reference number", "INTERNAL_TRANSACTION_ID");
        PAYMENT_DASHBOARD_FIELDS.put("External ID", "EXTERNAL_TRANSACTION_ID");

        PAYMENT_DASHBOARD_FIELD_TYPES.put(DATE_TIME, "Date");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Provider", "String");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Payment method", "String");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Status", "StringWithPopup");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Amount", "Number");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Currency", "String");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Chips", "Number");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("Reference number", "String");
        PAYMENT_DASHBOARD_FIELD_TYPES.put("External ID", "String");
    }

    /**
     * Create a DAO connected to a JDBC template
     *
     * @param jdbcTemplate Template to connect to
     */
    @Autowired(required = true)
    public PostgresPlayerInformationDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Dashboard getPaymentDashboard(final PaymentDashboardSearchCriteria searchCriteria,
                                         final String sortOrder,
                                         final Integer firstRecord,
                                         final Integer pageSize) {
        return new Dashboard(PlayerDashboard.PAYMENT, searchPaymentsBy(searchCriteria, firstRecord, pageSize, sortOrder),
                PAYMENT_DASHBOARD_FIELDS, PAYMENT_DASHBOARD_FIELD_TYPES);
    }

    private List<Map<String, Object>> searchPaymentsBy(final DashboardSearchCriteria searchCriteria,
                                                       final Integer firstRecord,
                                                       final Integer pageSize,
                                                       final String sortOrder) {
        final String request = ""
                + "SELECT "
                + getCommaSeparatedList(PAYMENT_DASHBOARD_FIELDS)
                + ",FAILURE_REASON"
                + " FROM EXTERNAL_TRANSACTION WHERE EXTERNAL_TRANSACTION_STATUS<>'REQUEST' AND ACCOUNT_ID = ?"
                + searchCriteria.getSearchString() + searchCriteria.getHavingString() + orderBy(sortOrder)
                + limitBy(firstRecord, pageSize);
        return jdbcTemplate.query(request, searchCriteria.getSearchParameters(), new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet resultSet, final int arg1) throws SQLException {
                final Map<String, Object> row = readMappedValuesFrom(resultSet, PAYMENT_DASHBOARD_FIELDS);

                final String status = (String) row.get("Status");
                String failureReason = resultSet.getString("FAILURE_REASON");
                if ("FAILURE".equals(status) && StringUtils.isEmpty(failureReason)) {
                    failureReason = "Reason for failure is unknown";
                }
                row.put("Status", asList(status, failureReason));
                return row;
            }
        });
    }

    private String limitBy(final Integer firstRecord,
                           final Integer pageSize) {
        final StringBuilder limitClause = new StringBuilder();
        if (pageSize != null) {
            limitClause.append(" LIMIT ").append(pageSize);
        }
        if (firstRecord != null) {
            limitClause.append(" OFFSET ").append(firstRecord);
        }
        return limitClause.toString();
    }

    private String orderBy(final String sortOrder) {
        final String orderBy;
        if (sortOrder == null || "".equals(sortOrder)) {
            orderBy = "";
        } else {
            orderBy = " ORDER BY " + sortOrder;
        }
        return orderBy;
    }

    private Map<String, Object> readMappedValuesFrom(final ResultSet resultSet,
                                                     final Map<String, String> fieldMap)
            throws SQLException {
        final Map<String, Object> record = new LinkedHashMap<String, Object>();
        int resultIndex = 1;
        for (String paymentDashboardFieldName : fieldMap.keySet()) {
            record.put(paymentDashboardFieldName, resultSet.getObject(resultIndex));
            resultIndex += 1;
        }
        return record;
    }

    /**
     * Fills the list with the field names
     *
     * @param fields Map of fields to concatenate
     * @return Comma-separated list of fields
     */
    private String getCommaSeparatedList(final Map<String, String> fields) {
        final StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (final Entry<String, String> entry : fields.entrySet()) {
            if (started) {
                sb.append(",");
            } else {
                started = true;
            }
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public Dashboard getGameDashboard(final BigDecimal accountId,
                                      final String sortOrder,
                                      final Integer firstRecord,
                                      final Integer pageSize,
                                      final Date dateFrom,
                                      final Date dateTo,
                                      final BigDecimal tableId,
                                      final String gameType) {
        final Map<String, String> dashboardFields = new LinkedHashMap<String, String>();
        dashboardFields.put(DATE_TIME, "MAX(tl.TRANSACTION_TS) as transaction_ts");
        dashboardFields.put(TABLE, "tl.table_id");
        dashboardFields.put(GAME, "tl.game_id");
        dashboardFields.put(GAME_TYPE, "ti.GAME_TYPE");
        dashboardFields.put(TABLE_NAME, "ti.TABLE_NAME");
        dashboardFields.put(STAKE, "-SUM(tl.AMOUNT*((tl.TRANSACTION_TYPE='Stake')::integer)) as stake");
        dashboardFields.put(RETURN, "SUM(tl.AMOUNT*((tl.TRANSACTION_TYPE='Return')::integer))as return");
        dashboardFields.put(WIN_LOSS, "SUM(tl.AMOUNT) as winloss");
        dashboardFields.put(SLOTS_BONUS_FIELD, "''");
        dashboardFields.put(APPROX_LAST_BALANCE, "(SELECT a.RUNNING_BALANCE FROM TRANSACTION_LOG a "
                + "WHERE a.ACCOUNT_ID = ? AND a.TRANSACTION_TS=MAX(tl.TRANSACTION_TS) LIMIT 1) as bal");
        final Map<String, String> dashboardFieldTypes = new LinkedHashMap<String, String>();
        dashboardFieldTypes.put(DATE_TIME, "Date");
        dashboardFieldTypes.put(TABLE, "Table");
        dashboardFieldTypes.put(GAME, "Table");
        dashboardFieldTypes.put(GAME_TYPE, "String");
        dashboardFieldTypes.put(TABLE_NAME, "String");
        dashboardFieldTypes.put(STAKE, "Number");
        dashboardFieldTypes.put(RETURN, "Number");
        dashboardFieldTypes.put(WIN_LOSS, "Number");
        dashboardFieldTypes.put(SLOTS_BONUS_FIELD, "String");
        dashboardFieldTypes.put(APPROX_LAST_BALANCE, "Number");

        final DashboardSearchCriteria searchCriteria = new DashboardSearchCriteria();
        searchCriteria.setGameSearch("MAX(tl.TRANSACTION_TS)", dateFrom, dateTo, "ti.GAME_TYPE", gameType,
                "tl.TABLE_ID", tableId, null, null, accountId, accountId);

        final String orderBy;
        if (sortOrder == null || "".equals(sortOrder)) {
            orderBy = "MAX(tl.TRANSACTION_TS) DESC";
        } else {
            orderBy = sortOrder;
        }
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> dashboard =
                jdbcTemplate
                        .query("SELECT "
                                + getCommaSeparatedList(dashboardFields)
                                + ",MAX(tl.REFERENCE) REFERENCE"
                                + " FROM TRANSACTION_LOG tl LEFT JOIN TABLE_INFO ti "
                                + "ON tl.TABLE_ID =ti.TABLE_ID "
                                + "WHERE ACCOUNT_ID = ? AND (TRANSACTION_TYPE='Stake' OR TRANSACTION_TYPE='Return') "
                                + searchCriteria.getSearchString()
                                + "GROUP BY tl.TABLE_ID, tl.GAME_ID,ti.GAME_TYPE, ti.table_name"
                                + searchCriteria.getHavingString() + " ORDER BY " + orderBy + " LIMIT "
                                + pageSize + " OFFSET " + firstRecord, searchCriteria.getSearchParameters(),
                                new RowMapper() {
                                    @Override
                                    public Object mapRow(final ResultSet resultSet, final int arg1) throws SQLException {
                                        final Map<String, Object> dataRow = new LinkedHashMap<String, Object>();

                                        dataRow.put(DATE_TIME, resultSet.getTimestamp("transaction_ts"));
                                        dataRow.put(TABLE, resultSet.getBigDecimal("table_id"));
                                        dataRow.put(GAME, resultSet.getBigDecimal("game_id"));
                                        dataRow.put(GAME_TYPE, resultSet.getString("game_type"));
                                        dataRow.put(TABLE_NAME, resultSet.getString("table_name"));
                                        dataRow.put(STAKE, resultSet.getString("stake"));
                                        dataRow.put(RETURN, resultSet.getString("return"));
                                        dataRow.put(WIN_LOSS, resultSet.getString("winloss"));
                                        final Reference ref = new Reference(resultSet.getString("REFERENCE"));
                                        if ("SLOTS".equals(resultSet.getString("game_type"))
                                                && !"".equals(ref.getReference())) {
                                            dataRow.put(SLOTS_BONUS_FIELD, "BB");
                                        } else {
                                            dataRow.put(SLOTS_BONUS_FIELD, "");
                                        }

                                        dataRow.put(APPROX_LAST_BALANCE, resultSet.getString("bal"));
                                        return dataRow;
                                    }
                                });

        return new Dashboard(PlayerDashboard.GAME, dashboard, sanitizeDashboardFieldsForCorrectSortType(dashboardFields), dashboardFieldTypes);
    }

    protected Map<String, String> sanitizeDashboardFieldsForCorrectSortType(Map<String, String> dashboardFields) {
        final Map<String, String> newDashboardFields = new LinkedHashMap<String, String>(dashboardFields);
        newDashboardFields.put(DATE_TIME, "transaction_ts");
        newDashboardFields.put(STAKE, "stake");
        newDashboardFields.put(RETURN, "return");
        newDashboardFields.put(WIN_LOSS, "winloss");
        newDashboardFields.put(APPROX_LAST_BALANCE, "bal");
        return newDashboardFields;
    }


    private Map<String, Object> mapRowWithReference(final ResultSet resultSet,
                                                    final Map<String, String> dashboardFields) throws SQLException {
        final Map<String, Object> row = new LinkedHashMap<String, Object>();
        final Iterator<Entry<String, String>> iterator = dashboardFields.entrySet().iterator();
        for (int i = 1; i <= dashboardFields.size(); i++) {
            final Entry<String, String> entry = iterator.next();
            row.put(entry.getKey(), resultSet.getObject(i));
        }
        return row;
    }

    @Override
    public Dashboard getGameDetails(final BigDecimal accountId,
                                    final String sortOrder,
                                    final String tableSelected,
                                    final Integer firstRecord,
                                    final Integer pageSize) {
        final Reference reference = new Reference(tableSelected);

        final Map<String, String> dashboardFields = new LinkedHashMap<String, String>();
        dashboardFields.put(DATE_TIME, "tl.TRANSACTION_TS");
        dashboardFields.put(TABLE, "tl.table_id");
        dashboardFields.put(GAME, "tl.game_id");
        dashboardFields.put(GAME_TYPE, "ti.GAME_TYPE");
        dashboardFields.put(TABLE_NAME, "ti.TABLE_NAME");
        dashboardFields.put(STAKE, "-tl.AMOUNT*((tl.TRANSACTION_TYPE='Stake')::integer) as stake");
        dashboardFields.put(RETURN, "tl.AMOUNT*((tl.TRANSACTION_TYPE='Return')::integer) as return");
        dashboardFields.put(WIN_LOSS, "tl.AMOUNT");
        dashboardFields.put("Running balance", "tl.RUNNING_BALANCE");

        final Map<String, String> dashboardFieldTypes = new LinkedHashMap<String, String>();
        dashboardFieldTypes.put(DATE_TIME, "Date");
        dashboardFieldTypes.put(TABLE, "Table");
        dashboardFieldTypes.put(GAME, "Table");
        dashboardFieldTypes.put(GAME_TYPE, "String");
        dashboardFieldTypes.put(TABLE_NAME, "String");
        dashboardFieldTypes.put(STAKE, "Number");
        dashboardFieldTypes.put(RETURN, "Number");
        dashboardFieldTypes.put(WIN_LOSS, "Number");
        dashboardFieldTypes.put("Running balance", "Number");

        final DashboardSearchCriteria searchCriteria = new DashboardSearchCriteria();
        searchCriteria.setSearchParameters(new Object[]{accountId,
                new BigDecimal(reference.getTableId()), new BigDecimal(reference.getGameId())});

        final String orderBy;
        if (sortOrder == null || "".equals(sortOrder)) {
            orderBy = "tl.TRANSACTION_TS DESC";
        } else {
            orderBy = sortOrder;
        }

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> dashboard =
                jdbcTemplate.query("SELECT "
                        + getCommaSeparatedList(dashboardFields)
                        + ",tl.REFERENCE"
                        + " FROM TRANSACTION_LOG tl LEFT JOIN TABLE_INFO ti "
                        + "ON tl.table_id =ti.TABLE_ID "
                        + "WHERE ACCOUNT_ID = ? AND (TRANSACTION_TYPE='Stake' OR TRANSACTION_TYPE='Return') "
                        + "AND (tl.table_id= ? and tl.game_id = ?) "
                        + searchCriteria.getSearchString()
                        + searchCriteria.getHavingString()
                        + "ORDER BY " + orderBy
                        + " LIMIT " + pageSize
                        + " OFFSET " + firstRecord,
                        searchCriteria.getSearchParameters(), new RowMapper() {
                    @Override
                    public Object mapRow(final ResultSet resultSet, final int arg1) throws SQLException {
                        final Reference ref = new Reference(resultSet.getString("REFERENCE"));
                        return mapRowWithReference(resultSet, dashboardFields);
                    }
                });

        final List<String> gameDetails = new ArrayList<String>();
        if (reference.hasTableId()) {
            try {
                final String details =
                        jdbcTemplate.queryForObject(
                                "SELECT OBSERVABLE_STATUS FROM AUDIT_CLOSED_GAME WHERE TABLE_ID = ? AND GAME_ID = ? "
                                        + "ORDER BY audit_ts DESC LIMIT 1",
                                new Object[]{new BigDecimal(reference.getTableId()),
                                        new BigDecimal(reference.getGameId())}, String.class);
                final String detailsParsed;
                final int historyIndex = details.indexOf("\nHistory");
                if (historyIndex >= 0) {
                    detailsParsed = details.substring(historyIndex + 8);
                } else {
                    detailsParsed = "";
                }

                final String[] refs = detailsParsed.split("\n");
                for (final String ref : refs) {
                    if (!"".equals(ref.trim())) {
                        gameDetails.add(ref);
                    }
                }
            } catch (final EmptyResultDataAccessException x) {
                // No action if the game is not found
            }
        }

        return new GameDashboard(dashboard, dashboardFields, dashboardFieldTypes, gameDetails);
    }

    @Override
    public Dashboard getStatementDashboard(final BigDecimal accountId,
                                           final String sortOrder,
                                           final Integer firstRecord,
                                           final Integer pageSize,
                                           final Date dateFrom,
                                           final Date dateTo) {
        final Map<String, String> dashboardFields = new LinkedHashMap<String, String>();
        dashboardFields.put("Date", "DATE(tl.TRANSACTION_TS)");
        dashboardFields.put(WIN_LOSS, "SUM(tl.AMOUNT)");
        dashboardFields.put("Last balance", "(SELECT a.RUNNING_BALANCE FROM TRANSACTION_LOG a "
                + "WHERE a.ACCOUNT_ID = ? AND a.TRANSACTION_TS=MAX(tl.TRANSACTION_TS) LIMIT 1)");
        final Map<String, String> dashboardFieldTypes = new LinkedHashMap<String, String>();
        dashboardFieldTypes.put("Date", "SDate");
        dashboardFieldTypes.put(WIN_LOSS, "Number");
        dashboardFieldTypes.put("Last balance", "Number");
        final String orderBy;
        if (sortOrder == null || "".equals(sortOrder)) {
            orderBy = "DATE(TRANSACTION_TS) DESC";
        } else {
            orderBy = sortOrder;
        }

        final DashboardSearchCriteria searchCriteria = new DashboardSearchCriteria();
        searchCriteria.setDatesSearch("DATE(tl.TRANSACTION_TS)", dateFrom, dateTo, accountId, accountId);

        final String request =
                "SELECT " + getCommaSeparatedList(dashboardFields)
                        + " FROM TRANSACTION_LOG tl WHERE tl.ACCOUNT_ID = ? "
                        + searchCriteria.getSearchString() + " GROUP BY DATE(tl.TRANSACTION_TS)"
                        + searchCriteria.getHavingString() + " ORDER BY " + orderBy + " LIMIT " + pageSize
                        + " OFFSET " + firstRecord;
        final Object[] param = searchCriteria.getSearchParameters();
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> dashboard = jdbcTemplate.query(request, param, new RowMapper() {
            @Override
            public Object mapRow(final ResultSet resultSet, final int arg1) throws SQLException {
                final Map<String, Object> row = new LinkedHashMap<String, Object>();
                final Iterator<Entry<String, String>> iterator = dashboardFields.entrySet().iterator();
                for (int i = 1; i <= dashboardFields.size(); i++) {
                    final Entry<String, String> entry = iterator.next();
                    row.put(entry.getKey(), resultSet.getObject(i));
                }
                return row;
            }
        });

        return new Dashboard(PlayerDashboard.STATEMENT, dashboard, dashboardFields, dashboardFieldTypes);
    }

    @Override
    public Dashboard getStatementDetails(final BigDecimal accountId,
                                         final String sortOrder,
                                         final Integer firstRecord,
                                         final Integer pageSize,
                                         final Date dateFrom,
                                         final Date dateTo,
                                         final BigDecimal tableId,
                                         final String gameType,
                                         final String transactionType) {

        final Map<String, String> dashboardFields = new LinkedHashMap<String, String>();
        dashboardFields.put(DATE_TIME, "tl.TRANSACTION_TS");
        dashboardFields.put(TABLE, "''");
        dashboardFields.put(GAME, "''");
        dashboardFields.put("Transaction Type", "tl.TRANSACTION_TYPE");
        dashboardFields.put(GAME_TYPE, "'to be fixed' AS GAME_TYPE");
        dashboardFields.put("Reference", "''");
        dashboardFields.put(WIN_LOSS, "tl.AMOUNT");
        dashboardFields.put("Running balance", "tl.RUNNING_BALANCE");

        final Map<String, String> dashboardFieldTypes = new LinkedHashMap<String, String>();
        dashboardFieldTypes.put(DATE_TIME, "Date");
        dashboardFieldTypes.put(TABLE, TABLE);
        dashboardFieldTypes.put(GAME, TABLE);
        dashboardFieldTypes.put("Transaction Type", "String");
        dashboardFieldTypes.put(GAME_TYPE, "String");
        dashboardFieldTypes.put("Reference", "String");
        dashboardFieldTypes.put(WIN_LOSS, "Number");
        dashboardFieldTypes.put("Running balance", "Number");

        final DashboardSearchCriteria searchCriteria = new DashboardSearchCriteria();
        searchCriteria.setGameSearch("tl.TRANSACTION_TS", dateFrom, dateTo, "gvt.GAME_TYPE", gameType,
                "tl.TABLE_ID", tableId, "tl.TRANSACTION_TYPE", transactionType, accountId);

        LOG.debug("Search string is {}, having string is {}, parameters are {}", searchCriteria.getSearchString(),
                searchCriteria.getHavingString(), searchCriteria.getSearchParameters());

        final List<Object> parameters = newArrayList();
        parameters.addAll(asList(searchCriteria.getSearchParameters()));
        parameters.add(pageSize);
        parameters.add(firstRecord);

        List<Map<String, Object>> results;
        results = jdbcTemplate.query(
                "SELECT TRANSACTION_TS, TRANSACTION_TYPE, gvt.GAME_TYPE, AMOUNT, RUNNING_BALANCE, REFERENCE, "
                        + "tl.TABLE_ID, tl.GAME_ID FROM TRANSACTION_LOG tl "
                        + "LEFT JOIN TABLE_DEFINITION td ON td.table_id = tl.table_id "
                        + "LEFT JOIN GAME_VARIATION_TEMPLATE gvt ON "
                        + "gvt.game_variation_template_id = td.game_variation_template_id "
                        + "WHERE tl.ACCOUNT_ID = ? "
                        + searchCriteria.getSearchString() + " "
                        + searchCriteria.getHavingString() + " "
                        + "ORDER BY tl.TRANSACTION_TS DESC LIMIT ? "
                        + "OFFSET ? ", new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet resultSet, final int i) throws SQLException {
                final Map<String, Object> dataRow = new LinkedHashMap<String, Object>();

                dataRow.put(DATE_TIME, resultSet.getTimestamp("TRANSACTION_TS"));
                dataRow.put(TABLE, resultSet.getBigDecimal("TABLE_ID"));
                dataRow.put(GAME, resultSet.getLong("GAME_ID"));
                dataRow.put("Transaction Type", resultSet.getString("TRANSACTION_TYPE"));
                dataRow.put(GAME_TYPE, resultSet.getString("GAME_TYPE"));
                dataRow.put("Reference", resultSet.getString("REFERENCE"));
                dataRow.put(WIN_LOSS, resultSet.getBigDecimal("AMOUNT"));
                dataRow.put("Running Balance", resultSet.getBigDecimal("RUNNING_BALANCE"));

                return dataRow;
            }
        }, parameters.toArray());

        return new Dashboard(PlayerDashboard.STATEMENT, results, dashboardFields, dashboardFieldTypes);
    }


    @Override
    public BigDecimal getPlayerId(final String rpx, final String externalId) {
        List<BigDecimal> ids;
        if ("YAZINO".equalsIgnoreCase(rpx)) {
            BigDecimal playerId = new BigDecimal(externalId);
            ids = jdbcTemplate.queryForList(INTERNAL_PLAYER_ID_SEARCH, BigDecimal.class, playerId);
        } else {
            ids = jdbcTemplate.queryForList(EXTERNAL_PLAYER_ID_SEARCH, BigDecimal.class, rpx, externalId);
        }
        if (ids.size() > 1) {
            throw new TooManyPlayersMatchedToExternalIdException(rpx, externalId, ids);
        }
        if (ids.size() == 1) {
            return ids.get(0);
        }
        return null;
    }

}
