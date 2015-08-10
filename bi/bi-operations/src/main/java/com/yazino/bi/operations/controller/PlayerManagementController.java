package com.yazino.bi.operations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Optional;
import com.yazino.bi.operations.model.*;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.invitation.Invitation;
import com.yazino.platform.invitation.InvitationQueryService;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.PlayerSearchResult;
import com.yazino.platform.player.PlayerSummary;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.table.GameTypeInformation;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.PlayerInformationDao;
import strata.server.operations.repository.GameTypeRepository;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Controller talking to the player search engine and to backoffice services
 */
@Controller
public class PlayerManagementController {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerManagementController.class);

    private static final String PLAYER_DASHBOARD_MODEL_NAME = "model";

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    private final PlayerInformationDao dao;
    private final InvitationQueryService invitationQueryService;
    private final PlayerProfileService playerProfileService;
    private final PlayerService playerService;
    private final WalletService walletService;
    private final GameTypeRepository gameTypeRepository;

    private Long maximumAdjustment = 200000L;
    private String roleForFreeAdjustments = "ROLE_SUPPORT_MANAGER";

    @Autowired
    public PlayerManagementController(final PlayerInformationDao dao,
                                      final InvitationQueryService invitationQueryService,
                                      final PlayerProfileService playerProfileService,
                                      final WalletService walletService,
                                      final PlayerService playerService,
                                      final GameTypeRepository gameTypeRepository) {
        notNull(walletService, "walletService may not be null");
        notNull(gameTypeRepository, "gameTypeRepository may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(invitationQueryService, "invitationQueryService may not be null");

        this.dao = dao;
        this.invitationQueryService = invitationQueryService;
        this.playerProfileService = playerProfileService;
        this.playerService = playerService;
        this.walletService = walletService;
        this.gameTypeRepository = gameTypeRepository;

        jsonObjectMapper.registerModule(new JodaModule());
    }

    /**
     * Filling the "maximum lines in search" select
     *
     * @return Map of key-values to prefill
     */
    @ModelAttribute("pageSize")
    public Map<String, String> pageSizePrefill() {
        final Map<String, String> retval = new LinkedHashMap<>();
        retval.put("10", "10 per page");
        retval.put("20", "20 per page");
        retval.put("50", "50 per page");
        return retval;
    }

    /**
     * Fills the available game types
     *
     * @return List of game types
     */
    @ModelAttribute("gameTypes")
    public Map<String, String> gameTypesPrefill() {
        final Map<String, String> retval = new LinkedHashMap<>();
        retval.put("", "Any type");

        for (final GameTypeInformation gameTypeInformation : gameTypeRepository.getGameTypes().values()) {
            retval.put(gameTypeInformation.getId(), gameTypeInformation.getGameType().getName());
        }
        return retval;
    }

    /**
     * Checks if a particular role is present in the security context
     *
     * @param role Role to look for
     * @return True if the role is present
     */
    private boolean isRolePresent(final String role) {
        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();
        for (final GrantedAuthority auth : authentication.getAuthorities()) {
            if (role.equals(auth.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @ModelAttribute("codec")
    public URLCodec getLinkTool() {
        return new URLCodec();
    }

    @RequestMapping(value = {"/playerSearch", "/player/search"}, method = RequestMethod.GET)
    public ModelAndView searchPage() {
        final DashboardParameters parameters = new DashboardParameters();
        parameters.setPageSize(20);
        return new ModelAndView("playerSearch", "searchRequest", parameters);
    }

    @RequestMapping(value = "/player/{playerId}")
    public ModelAndView viewPlayer(@ModelAttribute("searchRequest") final DashboardParameters parameters,
                                   @PathVariable("playerId") final String query) {
        parameters.setQuery(query);
        return searchPlayers(parameters);
    }

    @RequestMapping(value = "/player/{playerId}/{tabName}")
    public ModelAndView viewPlayer(@ModelAttribute("searchRequest") final DashboardParameters parameters,
                                   @PathVariable("playerId") final String query,
                                   @PathVariable("tabName") final String tabName) {
        parameters.setQuery(query);
        parameters.setDashboardToDisplay(PlayerDashboard.valueOf(tabName));
        return searchPlayers(parameters);
    }

    @RequestMapping(value = {"/searchResults", "/player/search"}, method = RequestMethod.GET, params = "query")
    public ModelAndView searchPlayersByLegacyGet(@ModelAttribute("searchRequest") final DashboardParameters parameters) {
        return searchPlayers(parameters);
    }

    @RequestMapping(value = {"/searchResults", "/player/search"}, method = RequestMethod.POST)
    public ModelAndView searchPlayers(@ModelAttribute("searchRequest") final DashboardParameters parameters) {
        parameters.normalise();

        if (StringUtils.isBlank(parameters.getQuery())) {
            LOG.debug("No query submitted");
            return new ModelAndView("playerSearch", "searchResult", new PlayerSearchListResult(parameters, "No query entered."));
        }

        final BigDecimal playerId = playerIdForQuery(parameters);
        if (playerId != null) {
            final ModelAndView playerModel = showPlayer(parameters, playerId);
            if (playerModel != null) {
                return playerModel;
            }
        }

        final PagedData<PlayerSearchResult> candidates;
        try {
            candidates = searchForPlayers(parameters);
        } catch (IllegalArgumentException e) {
            return new ModelAndView("playerSearch", "searchResult", new PlayerSearchListResult(parameters, "Invalid query: " + e.getMessage()));
        }

        LOG.debug("Found {} candidates for query {}", candidates.getTotalSize(), parameters.getQuery());

        if (candidates.getTotalSize() == 1 && candidates.getSize() == 1) {
            return showPlayer(parameters, candidates.getData().get(0).getPlayerId());

        } else if (candidates.getSize() >= 1) {
            return new ModelAndView("playerSearch", "searchResult", new PlayerSearchListResult(parameters, candidates));
        }

        return new ModelAndView("playerSearch", "searchResult", new PlayerSearchListResult(parameters, "No matching records found."));
    }

    private ModelAndView showPlayer(final DashboardParameters parameters,
                                    final BigDecimal playerId) {
        final PlayerDashboardModel playerDashboardModel = createPlayerDashboardModel(parameters, playerId);
        if (playerDashboardModel == null) {
            return null;
        }

        if (parameters.getSearch() != null) {
            playerDashboardModel.setDashboard(dashboardForSelectedTab(playerId, playerDashboardModel.getPlayer().getAccountId(), parameters));
        }

        final Map<String, Object> models = new HashMap<>();
        models.put("parameters", parameters);
        models.put(PLAYER_DASHBOARD_MODEL_NAME, playerDashboardModel);
        return new ModelAndView("playerSearch", models);
    }

    @RequestMapping(value = "/player/{playerId}/statusHistory")
    public ModelAndView showPlayerStatusHistory(@PathVariable("playerId") final BigDecimal playerId,
                                                final HttpServletResponse response)
            throws IOException {
        if (playerId == null) {
            return writeJsonTo(response, SC_BAD_REQUEST, singletonMap("message", "playerId is required"));
        }

        try {
            return writeJsonTo(response, SC_OK, playerProfileService.findAuditRecordsFor(playerId));

        } catch (final Exception e) {
            LOG.error("Status history query failed for player {}", playerId, e);
            return writeJsonTo(response, SC_INTERNAL_SERVER_ERROR,
                    singletonMap("message", format("Status history query failed with error: %s", e.getMessage())));
        }
    }

    @RequestMapping(value = "/player/{playerId}/changeStatusTo/{newStatusName}")
    public ModelAndView changePlayerStatus(@PathVariable("playerId") final BigDecimal playerId,
                                           @PathVariable("newStatusName") final String newStatusName,
                                           @RequestParam("reason") final String reason,
                                           final HttpServletResponse response)
            throws IOException {
        if (playerId == null || newStatusName == null || reason == null) {
            return writeJsonTo(response, SC_BAD_REQUEST, singletonMap("message", "playerId, newStatusName and reason are required"));
        }

        final PlayerProfileStatus newStatus;
        try {
            newStatus = PlayerProfileStatus.valueOf(newStatusName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return writeJsonTo(response, SC_BAD_REQUEST, singletonMap("message", String.format("%s is an invalid status", newStatusName)));
        }

        final Optional<PlayerSummary> playerSummary = playerProfileService.findSummaryById(playerId);
        if (!playerSummary.isPresent()) {
            return writeJsonTo(response, SC_NOT_FOUND, singletonMap("message", String.format("playerId %s is invalid", playerId)));
        }

        final PlayerProfileStatus currentStatus = playerSummary.get().getStatus();
        if (currentStatus == newStatus) {
            return writeJsonTo(response, SC_OK, "Status updated");
        }

        if (!isValidSuccessor(newStatus, currentStatus)) {
            return writeJsonTo(response, SC_FORBIDDEN, singletonMap("message",
                    String.format("Status %s cannot be applied to a player in state %s", newStatus, currentStatus)));
        }

        try {
            playerProfileService.updateStatus(playerId, newStatus, lookupAuthenticatedUser(), reason);
            final Map<String, Object> successMessage = newHashMap();
            successMessage.put("message", format("Status changed to %s", newStatus));
            return writeJsonTo(response, SC_OK, successMessage);

        } catch (final Exception e) {
            LOG.error("Status change failed for player {}", playerId, e);
            return writeJsonTo(response, SC_INTERNAL_SERVER_ERROR,
                    singletonMap("message", format("Status change failed with error: %s", e.getMessage())));
        }
    }

    private boolean isValidSuccessor(final PlayerProfileStatus newStatus,
                                     final PlayerProfileStatus currentStatus) {
        switch (currentStatus) {
            case ACTIVE:
                if (newStatus == PlayerProfileStatus.BLOCKED) {
                    return true;
                }
                break;
            case BLOCKED:
                if (newStatus == PlayerProfileStatus.ACTIVE || newStatus == PlayerProfileStatus.CLOSED) {
                    return true;
                }
                break;
            default:
                return false;
        }

        return false;
    }

    @RequestMapping(value = "/player/{playerId}/adjust/{adjustmentAmount}")
    public ModelAndView adjustPlayerBalance(@PathVariable("playerId") final BigDecimal playerId,
                                            @PathVariable("adjustmentAmount") final BigDecimal adjustmentAmount,
                                            final HttpServletResponse response)
            throws IOException {
        if (playerId == null || adjustmentAmount == null) {
            return writeJsonTo(response, SC_BAD_REQUEST, singletonMap("message", "Player ID and Amount are required"));

        } else if (adjustmentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return writeJsonTo(response, SC_BAD_REQUEST, singletonMap("message", "Amount may not be zero"));

        } else if (adjustmentAmount.compareTo(BigDecimal.valueOf(maximumAdjustment)) > 0 && !isRolePresent(roleForFreeAdjustments)) {
            return writeJsonTo(response, SC_FORBIDDEN, singletonMap("message", format("Adjustment must be %s or less", maximumAdjustment)));
        }

        try {
            final BigDecimal newBalance = walletService.postTransaction(playerService.getAccountId(playerId), adjustmentAmount,
                    "Adjustment", format("Backoffice topup by %s", lookupAuthenticatedUser()), TransactionContext.EMPTY);
            final Map<String, Object> successMessage = newHashMap();
            successMessage.put("message", format("Balance adjusted by %s", adjustmentAmount));
            successMessage.put("balance", newBalance);
            return writeJsonTo(response, SC_OK, successMessage);

        } catch (final Exception e) {
            LOG.error("Adjustment failed for player {}", playerId, e);
            return writeJsonTo(response, SC_INTERNAL_SERVER_ERROR,
                    singletonMap("message", format("Adjustment failed with error: %s", e.getMessage())));
        }
    }

    private ModelAndView writeJsonTo(final HttpServletResponse response,
                                     final int status,
                                     final Object object) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        jsonObjectMapper.writeValue(response.getWriter(), object);
        return null;
    }

    private PlayerDashboardModel createPlayerDashboardModel(final DashboardParameters parameters,
                                                            final BigDecimal playerId) {
        final Optional<PlayerSummary> player = playerProfileService.findSummaryById(playerId);
        if (!player.isPresent()) {
            return null;
        }

        return new PlayerDashboardModel(player.get(), parameters);
    }

    private Dashboard dashboardForSelectedTab(final BigDecimal playerId,
                                              final BigDecimal accountId,
                                              final DashboardParameters parameters) {
        switch (parameters.getDashboardToDisplay()) {
            case PAYMENT:
                return getPaymentDashboard(accountId, parameters);

            case GAME:
                return getGameDashboard(accountId, parameters);

            case STATEMENT:
                return getStatementDashboard(accountId, parameters);

            case INVITE:
                return getInvitationTab(playerId);

            default:
                return null;
        }
    }

    private Dashboard getStatementDashboard(final BigDecimal accountId, final DashboardParameters parameters) {
        final String sortOrder = parameters.getSortOrder();
        final Integer firstRecord = parameters.getFirstRecord();
        final Integer pageSize = parameters.getPageSize();
        if (parameters.getSelectionDate() == null && parameters.shouldConsolidateStatement()) {
            return dao.getStatementDashboard(accountId, sortOrder, firstRecord, pageSize, parameters.getFromDate(),
                    parameters.getToDate());
        }

        final Date fromDate;
        Date toDate;
        if (parameters.getSelectionDate() != null) {
            fromDate = parameters.getSelectionDate();
            toDate = null;
        } else {
            fromDate = parameters.getFromDate();
            toDate = parameters.getToDate();
        }

        final BigDecimal tableId = bigDecimalFrom(parameters.getTable());
        if (tableId != null) {
            parameters.setTable(tableId.toPlainString());
        }

        return dao.getStatementDetails(accountId, sortOrder, firstRecord, pageSize, fromDate, toDate,
                tableId, parameters.getGameType(), parameters.getTransactionType());
    }

    private BigDecimal bigDecimalFrom(final String numberString) {
        final String filtered = filterNumber(numberString);
        if (filtered == null) {
            return null;
        }

        return new BigDecimal(filtered);
    }

    private String filterNumber(final String inputString) {
        if (inputString == null) {
            return null;
        }

        final StringBuilder numericPart = new StringBuilder();
        for (char c : inputString.trim().toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == '-') {
                numericPart.append(c);
            }
        }
        if (numericPart.length() == 0) {
            return null;
        }
        return numericPart.toString();
    }

    private Dashboard getGameDashboard(final BigDecimal accountId, final DashboardParameters parameters) {
        final String sortOrder = parameters.getSortOrder();
        final Integer firstRecord = parameters.getFirstRecord();
        final Integer pageSize = parameters.getPageSize();
        if (parameters.getTableDetail() == null) {
            final BigDecimal tableId = bigDecimalFrom(parameters.getTable());
            if (tableId != null) {
                parameters.setTable(tableId.toPlainString());
            }

            return dao.getGameDashboard(accountId, sortOrder, firstRecord, pageSize, parameters.getFromDate(),
                    parameters.getToDate(), tableId, parameters.getGameType());
        } else {
            return dao.getGameDetails(accountId, sortOrder, parameters.getTableDetail(), firstRecord, pageSize);
        }
    }

    private Dashboard getInvitationTab(final BigDecimal playerId) {
        final Set<Invitation> invitations = invitationQueryService.findInvitationsByIssuingPlayer(playerId);
        return new InvitationDashboard(buildInvitationTable(invitations));
    }

    private Dashboard getPaymentDashboard(final BigDecimal accountId, final DashboardParameters parameters) {
        final PaymentDashboardSearchCriteria searchCriteria = new PaymentDashboardSearchCriteria(
                accountId, parameters.getFromDate(), parameters.getToDate(), parameters.getProvider(),
                parameters.getReference(), parameters.getExternalTransactionId());
        return dao.getPaymentDashboard(searchCriteria, parameters.getSortOrder(), parameters.getFirstRecord(), parameters.getPageSize());
    }

    private boolean isNumeric(String query) {
        return query.matches("[\\d\\.]+");
    }

    private String lookupAuthenticatedUser() {
        String authenticatingUser;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticatingUser = "ANONYMOUS";
        } else {
            authenticatingUser = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        }
        return authenticatingUser;
    }

    private BigDecimal playerIdForQuery(final DashboardParameters parameters) {
        final String query = StringUtils.trimToEmpty(parameters.getQuery());
        if (isNumeric(query)) {
            return playerIdFrom(query);
        }
        return null;
    }

    private BigDecimal playerIdFrom(final String query) {
        BigDecimal playerId = new BigDecimal(query);
        if (playerId.scale() > 0 && playerId.compareTo(playerId.setScale(0, RoundingMode.HALF_EVEN)) == 0) {
            playerId = playerId.setScale(0);
        }
        return playerId;
    }

    private PagedData<PlayerSearchResult> searchForPlayers(final DashboardParameters parameters) {
        final String query = StringUtils.trimToEmpty(parameters.getQuery());
        final int startPage = parameters.getFirstRecord() / parameters.getPageSize();
        if (isEmailQuery(query)) {
            return playerProfileService.searchByEmailAddress(query, startPage, parameters.getPageSize());

        } else {
            return playerProfileService.searchByRealOrDisplayName(query, startPage, parameters.getPageSize());
        }
    }

    private boolean isEmailQuery(final String query) {
        return StringUtils.isNotBlank(query) && query.contains("@");
    }

    private TableModel buildInvitationTable(final Set<Invitation> invitations) {
        final TableModel model = new TableModel()
                .addColumn(new ColumnModel("Sender"))
                .addColumn(new ColumnModel("Recipient"))
                .addColumn(new ColumnModel("Source"))
                .addColumn(new ColumnModel("Status"))
                .addColumn(new ColumnModel("Reward"))
                .addColumn(new ColumnModel("Created", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")))
                .addColumn(new ColumnModel("Updated", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")));

        final List<Invitation> invitationList = newArrayList(invitations);
        Collections.sort(invitationList, new InvitationCreationComparator());

        for (final Invitation invitation : invitationList) {
            model.addRow(invitation.getIssuingPlayerId(), invitation.getRecipientIdentifier(), invitation.getSource(),
                    invitation.getStatus(), invitation.getChipsEarned(), millisFrom(invitation.getCreated()),
                    millisFrom(invitation.getLastUpdated()));
        }

        return model;
    }

    private Long millisFrom(final DateTime dateTime) {
        if (dateTime != null) {
            return dateTime.getMillis();
        }
        return null;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    @Value("${operations.adjustment.max}")
    public void setMaximumAdjustment(final Long maximumAdjustment) {
        this.maximumAdjustment = maximumAdjustment;
    }

    @Value("${operations.adjustment.unlimited-role}")
    public void setRoleForFreeAdjustments(final String roleForFreeAdjustments) {
        this.roleForFreeAdjustments = roleForFreeAdjustments;
    }

    private static class InvitationCreationComparator implements Comparator<Invitation> {
        @Override
        public int compare(final Invitation o1,
                           final Invitation o2) {
            final DateTime created1 = o1.getCreated();
            final DateTime created2 = o2.getCreated();

            if (created1 == null && created2 == null) {
                return 0;
            } else if (created1 == null) {
                return 1;
            } else if (created2 == null) {
                return -1;
            }
            return o2.getCreated().compareTo(o1.getCreated());
        }
    }
}
