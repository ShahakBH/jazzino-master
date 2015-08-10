package com.yazino.bi.operations.model;

import com.yazino.platform.player.PlayerSummary;
import com.yazino.bi.operations.service.InvitationFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.Assert.notNull;

public class DashboardLinkBuilder {

    private PlayerSummary player;
    private DashboardParameters parameters;
    private String path;
    private Map<String, String> queryParameters = new HashMap<String, String>();

    public DashboardLinkBuilder(final PlayerDashboard dashboard) {
        notNull(dashboard);
        this.path = pathFor(dashboard);
        withQueryParameter("dashboardToDisplay", dashboard.name());
    }

    public DashboardLinkBuilder(final PlayerSummary player,
                                final DashboardParameters parameters) {
        this.player = player;
        this.parameters = parameters;
    }

    public String getDashboardLink(final String itemPath,
                                   final String dashboard) {
        return getDashboardParameterString(itemPath, dashboard).toString();
    }

    public String getPageLink(final String itemPath,
                              final Integer page) {
        final StringBuilder retval = getLongParameterString(itemPath);
        retval.append("&search=Search&firstRecord=");
        retval.append(encode(page));
        retval.append("&pagedRequest=true");
        if (isNotBlank(parameters.getSortOrder())) {
            retval.append("&sortOrder=");
            retval.append(encode(parameters.getSortOrder()));
        }
        return retval.toString();
    }

    @Deprecated
    public String getSortLink(final String itemPath,
                              final String sort) {
        final StringBuilder retval = getLongParameterString(rootPathFor(itemPath));
        retval.append("&sortOrder=");
        retval.append(sort);
        if (parameters.getFirstRecord() != null) {
            retval.append("&firstRecord=");
            retval.append(encode(parameters.getFirstRecord()));
        }
        return retval.toString();
    }

    private StringBuilder getLongParameterString(final String itemPath) {
        final StringBuilder retval = getDashboardParameterString(
                itemPath, parameters.getDashboardToDisplay().toString());
        if (parameters.getSelectionDate() != null) {
            retval.append("&selectionDate=");
            retval.append(encode(parameters.getDateFormat().format(parameters.getSelectionDate())));
        }
        addTableDetail(retval);
        addFromToDates(retval);
        addTable(retval);
        addGameType(retval);
        addStatementConsolidation(retval);
        addProvider(retval);
        addReference(retval);
        addPlayerExternalId(retval);
        return retval;
    }

    private String encode(final Object string) {
        try {
            if (string != null) {
                return URLEncoder.encode(string.toString(), "UTF-8");
            }
            return null;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("JVM is fubar and doesn't support UTF-8", e);
        }
    }

    private void addTableDetail(StringBuilder retval) {
        if (parameters.getTableDetail() != null) {
            retval.append("&tableDetail=");
            retval.append(encode(parameters.getTableDetail()));
        }
    }

    private void addFromToDates(StringBuilder retval) {
        if (parameters.getFromDate() != null) {
            retval.append("&fromDate=");
            retval.append(encode(parameters.getDateFormat().format(parameters.getFromDate())));
        }
        if (parameters.getToDate() != null) {
            retval.append("&toDate=");
            retval.append(encode(parameters.getDateFormat().format(parameters.getToDate())));
        }
    }

    private void addTable(StringBuilder retval) {
        if (parameters.getTable() != null) {
            retval.append("&table=");
            retval.append(encode(parameters.getTable()));
        }
    }

    private void addGameType(StringBuilder retval) {
        if (parameters.getGameType() != null) {
            retval.append("&gameType=");
            retval.append(encode(parameters.getGameType()));
        }
    }

    private void addStatementConsolidation(StringBuilder retval) {
        if (parameters.getStatementConsolidation() == null || !parameters.getStatementConsolidation().contains("1")) {
            retval.append("&_statementConsolidation=on");
        } else {
            retval.append("&statementConsolidation=1&_statementConsolidation=on");
        }
    }

    private void addProvider(StringBuilder retval) {
        if (parameters.getProvider() != null) {
            retval.append("&provider=");
            retval.append(encode(parameters.getProvider()));
        }
    }

    private void addReference(StringBuilder retval) {
        if (parameters.getReference() != null) {
            retval.append("&reference=");
            retval.append(encode(parameters.getReference()));
        }
    }

    private void addPlayerExternalId(StringBuilder retval) {
        if (player.getExternalId().isPresent()) {
            retval.append("&externalId=");
            retval.append(encode(player.getExternalId().get()));
        }
    }

    private StringBuilder getDashboardParameterString(final String itemPath,
                                                      final String dashboard) {
        final StringBuilder retval = getBasicParameterString(itemPath);
        retval.append("&dashboardToDisplay=");
        retval.append(encode(dashboard));
        return retval;
    }

    private StringBuilder getBasicParameterString(final String itemPath) {
        final StringBuilder retval = new StringBuilder();
        retval.append(itemPath);
        retval.append("?query=");
        retval.append(encode(player.getPlayerId()));
        retval.append("&pageSize=");
        retval.append(encode(parameters.getPageSize()));
        return retval;
    }

    private String rootPathFor(final String basePath) {
        final StringBuilder rootPath = new StringBuilder(basePath)
                .append("/")
                .append(player.getPlayerId());
        if (parameters.getDashboardToDisplay() != null) {
            rootPath.append("/")
                    .append(encode(parameters.getDashboardToDisplay().toString()));
        }
        return rootPath.toString();
    }

    private String pathFor(final PlayerDashboard dashboard) {
        switch (dashboard) {
            case STATEMENT:
            case GAME:
            case PAYMENT:
            case INVITE:
                return "player/search";
            default:
                throw new IllegalArgumentException();
        }
    }

    public DashboardLinkBuilder withSortKey(final String sortKey) {
        if (isNotBlank(sortKey)) {
            return withQueryParameter("sort", sortKey) // TODO remove
                    .withQueryParameter("sortKey", sortKey);
        }
        return this;
    }

    public DashboardLinkBuilder withSortOrder(final InvitationFilter.Order sortOrder) {
        if (sortOrder == null) {
            return withQueryParameter("sortOrder", null);
        } else {
            return withQueryParameter("sortOrder", sortOrder.name());
        }
    }

    public DashboardLinkBuilder withQuery(final String query) {
        return withQueryParameter("query", query);
    }

    public DashboardLinkBuilder withFromDate(final Date from) {
        final String value;
        if (from == null) {
            value = null;
        } else {
            value = from.toString();
        }
        return withQueryParameter("from", value);
    }

    public DashboardLinkBuilder withToDate(final Date to) {
        final String value;
        if (to == null) {
            value = null;
        } else {
            value = to.toString();
        }
        return withQueryParameter("to", value);
    }

    public DashboardLinkBuilder withPageSize(final Integer pageSize) {
        final String value;
        if (pageSize == null) {
            value = null;
        } else {
            value = pageSize.toString();
        }
        return withQueryParameter("pageSize", value);
    }

    public DashboardLinkBuilder withPageNumber(final Integer pageNumber) {
        final String value;
        if (pageNumber == null) {
            value = null;
        } else {
            value = pageNumber.toString();
        }
        return withQueryParameter("pageNumber", value);
    }

    private DashboardLinkBuilder withQueryParameter(final String key, final String value) {
        queryParameters.put(key, value);
        return this;
    }

    public String build() {
        final StringBuilder builder = new StringBuilder();
        builder.append(path);

        boolean firstParam = true;
        for (String key : queryParameters.keySet()) {
            if (queryParameters.get(key) != null) {
                if (firstParam) {
                    firstParam = false;
                    builder.append("?");
                } else {
                    builder.append("&");
                }

                builder.append(key);
                builder.append("=");
                builder.append(encode(queryParameters.get(key)));
            }
        }
        return builder.toString();
    }

    public DashboardLinkBuilder withSearchMode(final boolean on) {
        if (on) {
            return withQueryParameter("search", "Search");
        } else {
            return withQueryParameter("search", null);
        }
    }

    public DashboardLinkBuilder withFirstRecord(final Integer firstRecord) {
        final String value;
        if (firstRecord == null) {
            value = null;
        } else {
            value = firstRecord.toString();
        }
        return withQueryParameter("firstRecord", value);
    }

    public DashboardLinkBuilder withTable(final String table) {
        return withQueryParameter("table", table);
    }

    public DashboardLinkBuilder withGameType(final String gameType) {
        return withQueryParameter("gameType", gameType);
    }

    public DashboardLinkBuilder withTableDetail(final String tableDetail) {
        return withQueryParameter("tableDetail", tableDetail);
    }

    public DashboardLinkBuilder withStatementConsolidation(final boolean isOn) {
        final String value;
        if (isOn) {
            value = "0";
        } else {
            value = "1";
        }
        return withQueryParameter("statementConsolidation", value);
    }

    public DashboardLinkBuilder withSelectionDate(final Date selectionDate) {
        final String value;
        if (selectionDate == null) {
            value = null;
        } else {
            value = parameters.getDateFormat().format(parameters.getSelectionDate());
        }
        return withQueryParameter("selectionDate", value);
    }

    public DashboardLinkBuilder withProvider(final String provider) {
        return withQueryParameter("provider", provider);
    }

    public DashboardLinkBuilder withExternalId(final String externalId) {
        return withQueryParameter("externalId", externalId);
    }

    public DashboardLinkBuilder withReference(final String reference) {
        return withQueryParameter("reference", reference);
    }
}
