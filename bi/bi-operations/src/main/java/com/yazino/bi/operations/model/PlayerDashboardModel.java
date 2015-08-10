package com.yazino.bi.operations.model;

import com.yazino.platform.player.PlayerSummary;
import com.yazino.bi.operations.service.InvitationFilter;

public class PlayerDashboardModel extends DashboardParameters {

    private final DashboardParameters parameters;
    private final PlayerSummary player;

    private Dashboard dashboard;

    public PlayerDashboardModel(final PlayerSummary player,
                                final DashboardParameters parameters) {
        this.player = player;
        this.parameters = parameters;
    }

    public PlayerSummary getPlayer() {
        return player;
    }

    public DashboardParameters getParameters() {
        return parameters;
    }

    public DashboardLinkBuilder getLinkBuilder() {
        return new DashboardLinkBuilder(player, parameters);
    }

    public Dashboard getDashboard() {
        if (dashboard == null) {
            if (parameters.getDashboardToDisplay() != null) {
                return new Dashboard(parameters.getDashboardToDisplay());
            }
            return new Dashboard(PlayerDashboard.PAYMENT);
        }
        return dashboard;
    }

    public void setDashboard(final Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public String getSortLink(final PlayerDashboard dashboard, final String sortKey) {
        final boolean newSort = (getDashboard().getName() != dashboard)
                || (!parameters.getSortOrder().equals(sortKey));
        final InvitationFilter.Order newOrder;
        if (newSort) {
            newOrder = InvitationFilter.Order.DESC;
        } else {
            newOrder = toggleOrder(parameters.getOrder());
        }

        final DashboardLinkBuilder builder =
                forDashboard(dashboard, parameters)
                        .withSearchMode(true)
                        .withSortKey(sortKey)
                        .withSortOrder(newOrder);

        if (parameters.getFirstRecord() != null) {
            builder.withFirstRecord(parameters.getFirstRecord());
        }

        builder
                .withQuery(parameters.getQuery())
                .withPageSize(parameters.getPageSize())
                .withFromDate(parameters.getFromDate())
                .withToDate(parameters.getToDate());

        switch (dashboard) {
            case GAME:
                builder
                        .withTable(parameters.getTable())
                        .withGameType(parameters.getGameType())
                        .withTableDetail(parameters.getTableDetail());
                break;
            case STATEMENT:
                builder
                        .withStatementConsolidation(true)
                        .withTable(parameters.getTable())
                        .withGameType(parameters.getGameType())
                        .withTableDetail(parameters.getTableDetail())
                        .withSelectionDate(parameters.getSelectionDate());
                break;
            case PAYMENT:
                builder
                        .withProvider(parameters.getProvider())
                        .withReference(parameters.getReference())
                        .withExternalId(parameters.getExternalTransactionId())
                        .withTableDetail(parameters.getTableDetail());
                break;
            default:
                // ignored
                break;
        }

        return builder.build();
    }

    public DashboardLinkBuilder forDashboard(final PlayerDashboard dashboard,
                                             final DashboardParameters dashboardParameters) {
        switch (dashboard) {
            case STATEMENT:
                return forStatements(dashboardParameters);
            case GAME:
                return forGame(dashboardParameters);
            case PAYMENT:
                return forPayment(dashboardParameters);
            case INVITE:
                return forInvites(dashboardParameters);
            default:
                throw new IllegalArgumentException();
        }
    }

    public DashboardLinkBuilder forStatements(final DashboardParameters statementParameters) {
        return new DashboardLinkBuilder(PlayerDashboard.STATEMENT)
                .withSortKey(statementParameters.getSortOrder())
                .withSortOrder(statementParameters.getOrder())
                .withSearchMode(true);
    }

    public DashboardLinkBuilder forGame(final DashboardParameters gameParameters) {
        return new DashboardLinkBuilder(PlayerDashboard.GAME)
                .withSortKey(gameParameters.getSortOrder())
                .withSortOrder(gameParameters.getOrder())
                .withSearchMode(true);
    }

    public DashboardLinkBuilder forPayment(final DashboardParameters paymentParameters) {
        return new DashboardLinkBuilder(PlayerDashboard.PAYMENT)
                .withSortKey(paymentParameters.getSortOrder())
                .withSortOrder(paymentParameters.getOrder())
                .withSearchMode(true);
    }

    public DashboardLinkBuilder forInvites(final DashboardParameters inviteParameters) {
        return new DashboardLinkBuilder(PlayerDashboard.INVITE)
                .withQuery(inviteParameters.getQuery())
                .withFromDate(inviteParameters.getFromDate())
                .withToDate(inviteParameters.getToDate())
                .withPageSize(inviteParameters.getPageSize())
                .withPageNumber(inviteParameters.getPageNumber())
                .withSortKey(inviteParameters.getSortOrder())
                .withSortOrder(inviteParameters.getOrder())
                .withSearchMode(true);
    }

    private InvitationFilter.Order toggleOrder(final InvitationFilter.Order current) {
        if (current == InvitationFilter.Order.ASC) {
            return InvitationFilter.Order.DESC;
        } else {
            return InvitationFilter.Order.ASC;
        }
    }

}
