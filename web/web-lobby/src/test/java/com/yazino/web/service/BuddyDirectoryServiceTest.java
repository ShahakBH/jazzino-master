package com.yazino.web.service;

import com.yazino.platform.community.PlayerService;
import com.yazino.web.util.JsonHelper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuddyDirectoryServiceTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final BigDecimal PLAYER_ID_2 = BigDecimal.ONE;
    public static final String DISPLAY_NAME = "PLAYER-NAME";
    public static final String DISPLAY_NAME_2 = "PLAYER-NAME2";
    public static final List<BuddyDirectoryService.Buddy> BUDDIES = asList(new BuddyDirectoryService.Buddy(PLAYER_ID, DISPLAY_NAME), new BuddyDirectoryService.Buddy(PLAYER_ID_2, DISPLAY_NAME_2));
    public static final int PAGE_INDEX = 2;
    public static final int PAGE_SIZE = 10;
    public static final int PAGE_COUNT = 20;
    public static final String FILTER = "filter";

    private BuddyDirectoryService underTest;
    private PlayerService playerService = mock(PlayerService.class);

    @Before
    public void setUp() {
        underTest = new BuddyDirectoryService(playerService);
    }

    @Test
    public void shouldReturnSpecifiedPage() {
        int pageSize = 5;
        int pageCount = 2; // 2 pages is sufficient to test boundaries
        int nFriends = pageSize * pageCount - 1 /* last page not full */;
        String filter = null;
        final Map<BigDecimal, String> friends = new LinkedHashMap<>();
        for(int playerId = 0; playerId < nFriends; playerId++) {
            friends.put(BigDecimal.valueOf(playerId), "Player " + playerId);
        }
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(friends);

        BuddyDirectoryService.PageOfBuddies page0 = underTest.loadPage(PLAYER_ID, filter, pageSize, 0);
        assertThat(page0.getPageCount(), equalTo(2));
        assertThat(page0.getPageSize(), equalTo(5));
        assertThat(page0.getPageIndex(), equalTo(0));
        assertThat(page0.getFilter(), equalTo(filter));
        for(int i = 0; i < pageSize; i++) {
            assertThat(page0.getBuddies().get(i).getDisplayName(), equalTo(friends.get(BigDecimal.valueOf(i))));
        }

        BuddyDirectoryService.PageOfBuddies page1 = underTest.loadPage(PLAYER_ID, filter, pageSize, 1);
        assertThat(page1.getPageCount(), equalTo(2));
        assertThat(page1.getPageSize(), equalTo(5));
        assertThat(page1.getPageIndex(), equalTo(1));
        assertThat(page1.getFilter(), equalTo(filter));
        for(int i = 0; i < pageSize - 1; i++) {
            assertThat(page1.getBuddies().get(i).getDisplayName(), equalTo(friends.get(BigDecimal.valueOf(i + pageSize))));
        }
    }

    @Test
    public void shouldFilterByDisplayNameStart() {
        int pageSize = 5;
        int pageCount = 2;
        int nFriends = pageSize * pageCount;
        String filter = "P";
        final Map<BigDecimal, String> friends = new LinkedHashMap<>();
        for(int playerId = 0; playerId < nFriends; playerId++) {
            String displayName = "Player " + playerId;
            if (playerId < pageSize) {
                displayName = "A " + displayName;
            }
            friends.put(BigDecimal.valueOf(playerId), displayName);
        }
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(friends);

        BuddyDirectoryService.PageOfBuddies page0 = underTest.loadPage(PLAYER_ID, filter, pageSize, 0);
        assertThat(page0.getPageCount(), equalTo(1));
        assertThat(page0.getPageSize(), equalTo(5));
        assertThat(page0.getPageIndex(), equalTo(0));
        assertThat(page0.getFilter(), equalTo(filter));
        for(int i = 0; i < pageSize; i++) {
            assertThat(page0.getBuddies().get(i).getDisplayName(), not(startsWith("A")));
        }
    }

    @Test
    public void shouldHandleNoFriendsCase() {
        int pageSize = 5;
        String filter = null;
        final Map<BigDecimal, String> friends = new LinkedHashMap<>();
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(friends);

        BuddyDirectoryService.PageOfBuddies page0 = underTest.loadPage(PLAYER_ID, filter, pageSize, 0);
        assertThat(page0.getPageCount(), equalTo(1));
        assertThat(page0.getPageSize(), equalTo(5));
        assertThat(page0.getPageIndex(), equalTo(0));
        assertThat(page0.getFilter(), equalTo(filter));
        assertThat(page0.getBuddies().isEmpty(), is(true));
    }

    @Test
    public void shouldThrowIllegalArgumentIfPageIndexLessThan0() {
        int pageSize = 5;
        String filter = null;
        final Map<BigDecimal, String> friends = new LinkedHashMap<>();
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(friends);

        try {
            BuddyDirectoryService.PageOfBuddies page0 = underTest.loadPage(PLAYER_ID, filter, pageSize, -1);
            fail("expected error");
        } catch (Exception e) {
            assertThat(e.getMessage(), Matchers.equalTo("pageIndex out of bounds: -1"));
        }
    }

    @Test
    public void shouldThrowIllegalArgumentIfPageIndexGreaterThanLastPage() {
        int pageSize = 5;
        String filter = null;
        final Map<BigDecimal, String> friends = new LinkedHashMap<>();
        when(playerService.getFriendsOrderedByNickname(PLAYER_ID)).thenReturn(friends);

        try {
            BuddyDirectoryService.PageOfBuddies page0 = underTest.loadPage(PLAYER_ID, filter, pageSize, 2);
            fail("expected error");
        } catch (Exception e) {
            assertThat(e.getMessage(), Matchers.equalTo("pageIndex out of bounds: 2"));
        }
    }

    @Test
    public void pageOfBuddiesShouldBeSerializable() {
        String json = new JsonHelper().serialize(new BuddyDirectoryService.PageOfBuddies(FILTER, PAGE_INDEX, PAGE_SIZE, PAGE_COUNT, BUDDIES));
        BuddyDirectoryService.PageOfBuddies page = new JsonHelper().deserialize(BuddyDirectoryService.PageOfBuddies.class, json);
        assertThat(page.getFilter(), equalTo(FILTER));
        assertThat(page.getPageIndex(), equalTo(PAGE_INDEX));
        assertThat(page.getPageCount(), equalTo(PAGE_COUNT));
        assertThat(page.getPageSize(), equalTo(PAGE_SIZE));
        assertThat(page.getBuddies(), hasItems(BUDDIES.get(0), BUDDIES.get(1)));
    }
}
