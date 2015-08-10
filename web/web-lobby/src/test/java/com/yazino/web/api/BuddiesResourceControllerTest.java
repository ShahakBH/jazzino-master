package com.yazino.web.api;

import com.yazino.platform.community.PlayerService;
import com.yazino.web.service.BuddyDirectoryService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuddiesResourceControllerTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(199);
    public static final int PAGE_SIZE = 40;
    public static final int PAGE_INDEX = 0;
    public static final String FILTER = "filter";

    @Mock
    private PlayerService playerService;
    @Mock
    private BuddyDirectoryService buddyDirectoryService;
    @Mock
    private LobbySessionCache sessionCache;
    @Mock
    private WebApiResponses responseWriter;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private PrintWriter writer;

    private final JsonHelper jsonHelper = new JsonHelper();

    private BuddiesResourceController underTest;

    @Before
    public void setup() throws IOException {
        underTest = new BuddiesResourceController(playerService, sessionCache, responseWriter, buddyDirectoryService);

        LobbySession session = mock(LobbySession.class);
        when(session.getPlayerId()).thenReturn(PLAYER_ID);
        when(sessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    public void shouldReturnEmptyBuddiesModelWhenPlayerIsALoner() throws Exception {
        Map<BigDecimal, String> expected = Collections.emptyMap();
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(expected);

        underTest.allBuddies(request, response);

        assertThat(jsonFromResponse().get("buddies"), is(equalTo((Object) newArrayList())));
    }

    @Test
    public void shouldReturnModelWithBuddiesWhenPlayerIsPopular() throws Exception {
        List<BigDecimal> expected = Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(102));
        Map<BigDecimal, String> results = newLinkedHashMap();
        results.put(BigDecimal.ONE, "1");
        results.put(BigDecimal.TEN, "10");
        results.put(new BigDecimal(102), "102");
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(results);

        underTest.allBuddies(request, response);

        assertThat(jsonFromResponse().get("buddies"), is(equalTo((Object) expected)));
    }

    @Test
    public void getBuddiesWithNamesShouldReturnJsonWithOrderedNamesOfFriends() {
        Map<BigDecimal, String> results = newLinkedHashMap();
        results.put(BigDecimal.ONE, "1");
        results.put(BigDecimal.TEN, "10");
        results.put(new BigDecimal(102), "102");
        List<Object[]> listOfBuddies = newArrayList();
        listOfBuddies.add(new Object[]{BigDecimal.ONE, "1"});
        listOfBuddies.add(new Object[]{BigDecimal.TEN, "10"});
        listOfBuddies.add(new Object[]{new BigDecimal(102), "102"});


        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(results);
        underTest.allBuddiesWithNames(mock(HttpServletRequest.class), response);
        System.out.println(jsonHelper.serialize(listOfBuddies));
        verify(writer).write(String.format("{\"result\":\"ok\",\"buddies\":%s}", jsonHelper.serialize(listOfBuddies)));
    }

    @Test
    public void shouldReturnEmptyRequestsModelWhenPlayerIsALoner() throws Exception {
        List<BigDecimal> expected = Collections.emptyList();
        when(playerService.getFriendRequestsOrderedByNickname(PLAYER_ID)).thenReturn(expected);

        underTest.allBuddyRequests(request, response);

        assertThat(jsonFromResponse().get("buddyRequests"), is(equalTo((Object) expected)));
    }

    @Test
    public void shouldReturnModelWithRequestsWhenPlayerIsPopular() throws Exception {
        List<BigDecimal> expected = Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(102));
        when(playerService.getFriendRequestsOrderedByNickname(PLAYER_ID)).thenReturn(expected);

        underTest.allBuddyRequests(request, response);

        assertThat(jsonFromResponse().get("buddyRequests"), is(equalTo((Object) expected)));
    }

    @Test
    public void convertToArrayShouldConvertNicelyOrderedBuddiesToArrayOfStrings() {
        Map<BigDecimal, String> buddies = newLinkedHashMap();
        buddies.put(BigDecimal.valueOf(999), "aaa");
        buddies.put(BigDecimal.valueOf(111), "bbb");
        buddies.put(BigDecimal.valueOf(555), "ccc");
        final List<Object[]> orderedBuddies = underTest.convertToList(buddies);

        int i = 0;
        for (BigDecimal id : buddies.keySet()) {
            assertThat(id, equalTo((BigDecimal) orderedBuddies.get(i)[0]));
            assertThat(buddies.get(id), equalTo((String) orderedBuddies.get(i)[1]));
            i++;
        }
    }

    @Test
    public void loadPageOfBuddiesShouldReturn400WithErrorMessageWhenPageSizeIsMissing() throws IOException {
        underTest.loadPageOfBuddies(request, response, "filter", null, 0);

        verify(responseWriter).writeError(same(response), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'pageSize' is missing"));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReturn403WhenNoSession() throws IOException {
        when(sessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);

        underTest.loadPageOfBuddies(request, response, FILTER, PAGE_SIZE, PAGE_INDEX);

        verify(responseWriter).writeError(same(response), eq(org.apache.http.HttpStatus.SC_FORBIDDEN), eq("no session"));
    }

    @Test
    public void loadPageOfBuddiesShouldDelegateToBuddyDirectoryService() throws IOException {
        BuddyDirectoryService.PageOfBuddies page = mock(BuddyDirectoryService.PageOfBuddies.class);
        when(buddyDirectoryService.loadPage(PLAYER_ID, FILTER, PAGE_SIZE, PAGE_INDEX)).thenReturn(page);

        underTest.loadPageOfBuddies(request, response, FILTER, PAGE_SIZE, PAGE_INDEX);

        verify(responseWriter).writeOk(same(response), same(page));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonFromResponse() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseWriter).writeOk(eq(response), jsonCaptor.capture());
        return jsonCaptor.getValue();
    }

}
