package com.yazino.bi.operations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Optional;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.invitation.Invitation;
import com.yazino.platform.invitation.InvitationQueryService;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.player.service.PlayerProfileService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.persistence.PlayerInformationDao;
import com.yazino.bi.operations.model.DashboardParameters;
import com.yazino.bi.operations.model.PlayerDashboard;
import com.yazino.bi.operations.model.PlayerDashboardModel;
import com.yazino.bi.operations.model.PlayerSearchListResult;
import strata.server.operations.repository.GameTypeRepository;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.invitation.InvitationSource.FACEBOOK;
import static com.yazino.platform.invitation.InvitationStatus.WAITING;
import static com.yazino.platform.player.PlayerProfileStatus.ACTIVE;
import static com.yazino.platform.player.PlayerProfileStatus.BLOCKED;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static com.yazino.bi.operations.model.PlayerDashboard.STATEMENT;

@RunWith(MockitoJUnitRunner.class)
public class PlayerManagementControllerTest {
    private static final BigDecimal ACCOUNT_ID = new BigDecimal("888");
    private static final BigDecimal PLAYER_ID = new BigDecimal("777");
    private static final String ROLE_FREE_ADJUSTMENTS = "ROLE_ADMIN";

    @Mock
    private PlayerInformationDao dao;
    @Mock
    private InvitationQueryService invitationQueryService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private WalletService walletService;
    @Mock
    private PlayerService playerService;
    @Mock
    private HttpServletResponse response;

    private final StringWriter responseWriter = new StringWriter();

    private PlayerManagementController underTest;

    @Before
    public void setUp() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        SecurityContextHolder.getContext().setAuthentication(null);

        underTest = new PlayerManagementController(dao, invitationQueryService, playerProfileService,
                walletService, playerService, mock(GameTypeRepository.class));
        underTest.setMaximumAdjustment(10000L);
        underTest.setRoleForFreeAdjustments("ROLE_ADMIN");
    }

    @Test
    public void testFillingInfo() {
        final Map<String, String> prefill = underTest.pageSizePrefill();
        assertTrue(prefill instanceof LinkedHashMap);
        assertTrue(prefill.size() > 0);
    }

    @Test
    public void testFillingTypes() {
        final Map<String, String> prefill = underTest.gameTypesPrefill();
        assertTrue(prefill instanceof LinkedHashMap);
        assertTrue(prefill.size() > 0);
    }

    @Test
    public void playerSearch_initializesModels() {
        final ModelAndView searchModel = underTest.searchPage();
        assertTrue(searchModel.getModel().get("searchRequest") instanceof DashboardParameters);
    }

    @Test
    public void playerSearch_emptyQuery() {
        final DashboardParameters request = new DashboardParameters();
        request.setQuery(null);
        final ModelAndView searchResult = underTest.searchPlayers(request);
        assertNull(searchResult.getModel().get("player"));
        assertEquals("playerSearch", searchResult.getViewName());
    }

    @Test
    public void playerSearch_mailQuery_invalidEmail() {
        final String INVALID_EMAIL_ADDRESS = "nobody@example.com";
        final String VALID_EMAIL_ADDRESS = "somebody@example.com";

        when(playerProfileService.searchByEmailAddress(INVALID_EMAIL_ADDRESS, 0, 30))
                .thenReturn(PagedData.<PlayerSearchResult>empty());
        final PlayerSearchResult player1 = new PlayerSearchResult(playerId(15), VALID_EMAIL_ADDRESS,
                "aRealName", "aDisplayName", "aProviderName", "anAvatarUrl", ACTIVE, PlayerProfileRole.CUSTOMER);
        final PlayerSearchResult player2 = new PlayerSearchResult(playerId(16), VALID_EMAIL_ADDRESS,
                "aRealName", "aDisplayName", "aProviderName", "anAvatarUrl", ACTIVE, PlayerProfileRole.CUSTOMER);
        final PagedData<PlayerSearchResult> matches = new PagedData<>(0, 1, 2, newArrayList(player1, player2));
        when(playerProfileService.searchByEmailAddress(VALID_EMAIL_ADDRESS, 0, 20)).thenReturn(matches);

        final DashboardParameters parameters = new DashboardParameters();
        parameters.setQuery(INVALID_EMAIL_ADDRESS);
        parameters.setPageSize(30);
        ModelAndView modelAndView = underTest.searchPlayers(parameters);
        assertEquals("playerSearch", modelAndView.getViewName());
        assertTrue(modelAndView.getModel().get("searchResult") instanceof PlayerSearchListResult);
        assertThat(((PlayerSearchListResult) modelAndView.getModel().get("searchResult")).getPlayersList(), is(equalTo(PagedData.<PlayerSearchResult>empty())));

        parameters.setQuery(VALID_EMAIL_ADDRESS);
        parameters.setPageSize(20);
        parameters.setDashboardToDisplay(STATEMENT);
        modelAndView = underTest.searchPlayers(parameters);
        assertEquals("playerSearch", modelAndView.getViewName());
        assertThat(((PlayerSearchListResult) modelAndView.getModel().get("searchResult")).getPlayersList(), is(equalTo(matches)));
    }

    @Test
    public void playerSearchParsesTheQueryForAUserWithAWholeId() {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalance(valueOf(10000))));
        final DashboardParameters parameters = new DashboardParameters();
        parameters.setQuery(PLAYER_ID.toPlainString());
        parameters.setPageSize(30);

        underTest.searchPlayers(parameters);

        verify(playerProfileService).findSummaryById(PLAYER_ID);
    }

    @Test
    public void playerSearchParsesTheQueryForAUserWithADecimalId() {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalance(valueOf(10000))));
        final DashboardParameters parameters = new DashboardParameters();
        parameters.setQuery(PLAYER_ID.toPlainString() + ".00");
        parameters.setPageSize(30);

        underTest.searchPlayers(parameters);

        verify(playerProfileService).findSummaryById(PLAYER_ID);
    }

    private BigDecimal playerId(final int playerId) {
        return new BigDecimal(Integer.toString(playerId));
    }

    @Test
    public void statementDashboard_loads_player_and_loads_statements_and_returns_playerSearch_view_and_player_model() {
        final String MATCHING_NAME = "Player name";
        final String SORT_ORDER = "sort";
        final PlayerSearchResult playerSearchResult = new PlayerSearchResult(PLAYER_ID, "anEmailAddress",
                "aRealName", "aDisplayName", "aProviderName", "anAvatarUrl", ACTIVE, PlayerProfileRole.CUSTOMER);
        final PagedData<PlayerSearchResult> candidates = new PagedData<>(0, 1, 1, newArrayList(playerSearchResult));
        final DashboardParameters parameters = new DashboardParameters();
        parameters.setSearch("Search"); // informs controller that this is a tab search
        parameters.setDashboardToDisplay(PlayerDashboard.STATEMENT);
        parameters.setFirstRecord(0);
        parameters.setQuery(MATCHING_NAME);
        parameters.setPageSize(30);
        parameters.setSortOrder(SORT_ORDER);
        parameters.setSelectionDate(null);
        final Set<String> s = new HashSet<>();
        s.add("consolidate");
        parameters.setStatementConsolidation(s);

        when(playerProfileService.searchByRealOrDisplayName(MATCHING_NAME, 0, 30)).thenReturn(candidates);
        final PlayerSummary player = aPlayerWithBalance(valueOf(10000));
        when(playerProfileService.findSummaryById(player.getPlayerId())).thenReturn(fromNullable(player));

        final ModelAndView modelAndView = underTest.searchPlayers(parameters);

        verify(dao).getStatementDashboard(eq(ACCOUNT_ID), eq(SORT_ORDER), eq(0), eq(30), isA(Date.class), isA(Date.class));
        assertThat(modelAndView.getModel().get("model"), is(instanceOf(PlayerDashboardModel.class)));
        assertThat(((PlayerDashboardModel) modelAndView.getModel().get("model")).getPlayer(), is(equalTo(player)));
        assertThat(modelAndView.getViewName(), is(equalTo("playerSearch")));
    }

    @Test
    public void testDataBinder() throws NoSuchMethodException {
        final WebDataBinder binder = mock(WebDataBinder.class);

        underTest.initBinder(binder);

        verify(binder).registerCustomEditor(eq(Date.class), isA(CustomDateEditor.class));
    }

    @Test
    public void adjustingAPlayersBalanceSendBadRequestWhenThePlayerIdIsNull() throws WalletServiceException, IOException {
        final ModelAndView modelAndView = underTest.adjustPlayerBalance(null, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Player ID and Amount are required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceSendBadRequestWhenTheAdjustmentAmountIsNull() throws WalletServiceException, IOException {

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, null, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Player ID and Amount are required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceSendBadRequestWhenTheAdjustmentAmountIsZero() throws WalletServiceException, IOException {
        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.ZERO, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Amount may not be zero"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceUpdatesTheBalance() throws IOException, WalletServiceException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(walletService.postTransaction(ACCOUNT_ID, new BigDecimal(10000), "Adjustment", "Backoffice topup by ANONYMOUS", TransactionContext.EMPTY))
                .thenReturn(BigDecimal.valueOf(20000));

        underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        verify(walletService).postTransaction(ACCOUNT_ID, new BigDecimal(10000), "Adjustment", "Backoffice topup by ANONYMOUS", TransactionContext.EMPTY);
    }

    @Test
    public void adjustingAPlayersBalanceReturnsAServerErrorWhenAccountIdLookupFails() throws IOException, WalletServiceException {
        when(playerService.getAccountId(PLAYER_ID)).thenThrow(new RuntimeException("aTestException"));

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Adjustment failed with error: aTestException"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceReturnsAServerErrorWhenTheTransactionFails() throws IOException, WalletServiceException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(walletService.postTransaction(eq(ACCOUNT_ID), Mockito.any(BigDecimal.class), anyString(), anyString(), Mockito.any(TransactionContext.class)))
                .thenThrow(new RuntimeException("aTestException"));

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Adjustment failed with error: aTestException"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceReturnsAJSONResponseWithTheNewBalance() throws IOException, WalletServiceException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(walletService.postTransaction(ACCOUNT_ID, new BigDecimal(10000), "Adjustment", "Backoffice topup by ANONYMOUS", TransactionContext.EMPTY)).thenReturn(BigDecimal.valueOf(20000));

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        final HashMap<String, Object> expectedMessageMap = newHashMap();
        expectedMessageMap.put("message", "Balance adjusted by 10000");
        expectedMessageMap.put("balance", 20000);
        final String expectedMessage = objectMapper().writeValueAsString(expectedMessageMap);
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceDoesNotAllowAUserToExceedTheAdjustmentLimit() throws WalletServiceException, IOException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        setAuthenticatedUser(userWithoutRoles(), "ROLE_MANAGEMENT");
        underTest.setMaximumAdjustment(9999L);
        underTest.setRoleForFreeAdjustments("ROLE_ADMIN");
        when(walletService.postTransaction(ACCOUNT_ID, new BigDecimal(10000), "Adjustment", "Backoffice topup by ANONYMOUS", TransactionContext.EMPTY)).thenReturn(BigDecimal.valueOf(20000));

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Adjustment must be 9999 or less"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void adjustingAPlayersBalanceIgnoresTheAdjustmentLimitWhereTheUserHasTheFreeAdjustmentRole() throws WalletServiceException, IOException {
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        setAuthenticatedUser(userWithoutRoles(), "ROLE_MANAGEMENT");
        underTest.setMaximumAdjustment(9999L);
        setAuthenticatedUser(userWithNameAndRole("root", ROLE_FREE_ADJUSTMENTS), ROLE_FREE_ADJUSTMENTS);
        when(walletService.postTransaction(ACCOUNT_ID, new BigDecimal(10000), "Adjustment", "Backoffice topup by root", TransactionContext.EMPTY)).thenReturn(BigDecimal.valueOf(20000));

        final ModelAndView modelAndView = underTest.adjustPlayerBalance(PLAYER_ID, BigDecimal.valueOf(10000), response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        final HashMap<String, Object> expectedMessageMap = newHashMap();
        expectedMessageMap.put("message", "Balance adjusted by 10000");
        expectedMessageMap.put("balance", 20000);
        final String expectedMessage = objectMapper().writeValueAsString(expectedMessageMap);
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    private void setAuthenticatedUser(final User authenticatedUser, final String authorityRole) {
        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(authenticatedUser, "",
                asList(new SimpleGrantedAuthority(authorityRole))));
    }

    private User userWithNameAndRole(final String userName, final String roleName) {
        return new User(userName, "", true, true, true, true,
                asList(new SimpleGrantedAuthority(roleName)));
    }

    private User userWithoutRoles() {
        return new User("ROOT", "", true, true, true, true, Collections.<GrantedAuthority>emptyList());
    }

    @Test
    public void testLinkTool() {
        assertNotNull(underTest.getLinkTool());
    }

    @Test
    public void invitesDashboard_returns_playerSearch_view_with_invites_model() {

        final Set<Invitation> invitations = newHashSet(
                new Invitation(PLAYER_ID, "recipient1", FACEBOOK, WAITING, new DateTime(), new DateTime(), BigDecimal.valueOf(10)),
                new Invitation(PLAYER_ID, "recipient2", FACEBOOK, WAITING, new DateTime(), new DateTime(), BigDecimal.valueOf(10)),
                new Invitation(PLAYER_ID, "recipient3", FACEBOOK, WAITING, new DateTime(), new DateTime(), BigDecimal.valueOf(10)));

        final DashboardParameters parameters = new DashboardParameters();
        parameters.setQuery(PLAYER_ID.toString());
        parameters.setSearch("Search");
        parameters.setDashboardToDisplay(PlayerDashboard.INVITE);
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalance(valueOf(10000))));
        when(invitationQueryService.findInvitationsByIssuingPlayer(PLAYER_ID)).thenReturn(invitations);

        final ModelAndView modelAndView = underTest.searchPlayers(parameters);

        assertTrue(modelAndView.getModel().get("model") instanceof PlayerDashboardModel);
        assertEquals("playerSearch", modelAndView.getViewName());
    }

    @Test
    public void aStatusHistoryRequestReturnsAListOfStatusAuditRecords() throws IOException {
        final List<PlayerProfileAudit> expectedHistory = asList(
                new PlayerProfileAudit(PLAYER_ID, ACTIVE, BLOCKED, "test1", "testReason1", new DateTime()),
                new PlayerProfileAudit(PLAYER_ID, BLOCKED, ACTIVE, "test2", "testReason2", new DateTime()),
                new PlayerProfileAudit(PLAYER_ID, ACTIVE, BLOCKED, "test3", "testReason3", new DateTime()));
        when(playerProfileService.findAuditRecordsFor(PLAYER_ID)).thenReturn(expectedHistory);

        final ModelAndView modelAndView = underTest.showPlayerStatusHistory(PLAYER_ID, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_OK);
        final String expectedMessage = objectMapper().writeValueAsString(expectedHistory);
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aFailedStatusHistoryRequestReturnsTheExceptionMessageInTheResponse() throws IOException {
        when(playerProfileService.findAuditRecordsFor(PLAYER_ID)).thenThrow(new RuntimeException("testException"));

        final ModelAndView modelAndView = underTest.showPlayerStatusHistory(PLAYER_ID, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Status history query failed with error: testException"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStatusHistoryRequestMustIncludeAPlayerId() throws IOException {
        final ModelAndView modelAndView = underTest.showPlayerStatusHistory(null, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "playerId is required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStateChangeMustIncludeAPlayerId() throws IOException {
        final ModelAndView modelAndView = underTest.changePlayerStatus(null, "BLOCKED", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "playerId, newStatusName and reason are required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStateChangeMustIncludeANewState() throws IOException {
        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, null, "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "playerId, newStatusName and reason are required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStateChangeMustIncludeAReason() throws IOException {
        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", null, response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "playerId, newStatusName and reason are required"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStateChangeForAnInvalidPlayerReturnsNotFound() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(Optional.<PlayerSummary>absent());

        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "playerId 777 is invalid"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aFailedStateChangeReturnsTheExceptionMessage() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), ACTIVE)));
        doThrow(new RuntimeException("testException")).when(playerProfileService).updateStatus(PLAYER_ID, BLOCKED, "ANONYMOUS", "testChange");

        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Status change failed with error: testException"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aStateChangeMustIncludeAValidState() throws IOException {
        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "fred", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "fred is an invalid status"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void anActivePlayerCanBeBlocked() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), ACTIVE)));

        underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", "testChange", response);

        verify(playerProfileService).updateStatus(PLAYER_ID, BLOCKED, "ANONYMOUS", "testChange");
    }

    @Test
    public void aPlayersNewStateIsReturnedInTheResponseMessage() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), ACTIVE)));

        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_OK);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Status changed to BLOCKED"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    @Test
    public void aBlockedPlayerCanBeClosed() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), BLOCKED)));

        underTest.changePlayerStatus(PLAYER_ID, "CLOSED", "testChange", response);

        verify(playerProfileService).updateStatus(PLAYER_ID, PlayerProfileStatus.CLOSED, "ANONYMOUS", "testChange");
    }

    @Test
    public void aBlockedPlayerCanBeReactivated() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), BLOCKED)));

        underTest.changePlayerStatus(PLAYER_ID, "ACTIVE", "testChange", response);

        verify(playerProfileService).updateStatus(PLAYER_ID, ACTIVE, "ANONYMOUS", "testChange");
    }

    @Test
    public void aClosedPlayerCannotBeReactivated() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), PlayerProfileStatus.CLOSED)));

        underTest.changePlayerStatus(PLAYER_ID, "ACTIVE", "testChange", response);

        verify(playerProfileService, times(0)).updateStatus(eq(PLAYER_ID), Mockito.any(PlayerProfileStatus.class), anyString(), anyString());
    }

    @Test
    public void aClosedPlayerCannotBeBlocked() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), PlayerProfileStatus.CLOSED)));

        underTest.changePlayerStatus(PLAYER_ID, "BLOCKED", "testChange", response);

        verify(playerProfileService, times(0)).updateStatus(eq(PLAYER_ID), Mockito.any(PlayerProfileStatus.class), anyString(), anyString());
    }

    @Test
    public void anActivePlayerCannotBeClosed() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), ACTIVE)));

        underTest.changePlayerStatus(PLAYER_ID, "CLOSED", "testChange", response);

        verify(playerProfileService, times(0)).updateStatus(eq(PLAYER_ID), Mockito.any(PlayerProfileStatus.class), anyString(), anyString());
    }

    @Test
    public void anInvalidStateChangeReturnsForbidden() throws IOException {
        when(playerProfileService.findSummaryById(PLAYER_ID)).thenReturn(fromNullable(aPlayerWithBalanceAndStatus(valueOf(10000), PlayerProfileStatus.CLOSED)));

        final ModelAndView modelAndView = underTest.changePlayerStatus(PLAYER_ID, "ACTIVE", "testChange", response);

        assertThat(modelAndView, is(nullValue()));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        final String expectedMessage = objectMapper().writeValueAsString(singletonMap("message", "Status ACTIVE cannot be applied to a player in state CLOSED"));
        assertThat(responseWriter.getBuffer().toString(), is(equalTo(expectedMessage)));
    }

    private ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    private PlayerSummary aPlayerWithBalance(final BigDecimal balance) {
        return aPlayerWithBalanceAndStatus(balance, ACTIVE);
    }

    private PlayerSummary aPlayerWithBalanceAndStatus(final BigDecimal balance,
                                                      final PlayerProfileStatus status) {
        return new PlayerSummary(PLAYER_ID, ACCOUNT_ID, "anAvatarUrl",
                new DateTime(3141592L), new DateTime(98765432100L), balance, "aRealName",
                "aDisplayName", "anEmailAddress", "aProviderName", "anExternalId", "aCountryCode",
                Gender.OTHER, status, PlayerProfileRole.CUSTOMER, valueOf(20000),
                Collections.<String, BigDecimal>emptyMap(), Collections.<String, Integer>emptyMap(), Collections.<String>emptySet());
    }

}
