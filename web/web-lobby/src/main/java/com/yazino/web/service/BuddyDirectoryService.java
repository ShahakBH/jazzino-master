package com.yazino.web.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.yazino.platform.community.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class BuddyDirectoryService {

    private final PlayerService playerService;

    @Autowired
    public BuddyDirectoryService(final PlayerService playerService) {
        this.playerService = playerService;
    }

    public PageOfBuddies loadPage(BigDecimal playerId, String filter, int pageSize, int pageIndex) {
        checkArgument(pageIndex >= 0, "pageIndex out of bounds: " + pageIndex);
        Map<BigDecimal, String> sortedFriends = playerService.getFriendsOrderedByNickname(playerId);
        List<Buddy> sortedBuddies = toBuddyList(sortedFriends);
        List<Buddy> filteredAndSortedBuddies = ImmutableList.copyOf(Iterables.filter(sortedBuddies, new CaseInsensitiveStartsWith(filter)));
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredAndSortedBuddies.size() / pageSize));
        checkArgument(pageIndex < pageCount, "pageIndex out of bounds: " + pageIndex);
        int firstBuddyIndexInclusive = pageIndex * pageSize;
        int lastBuddyIndexExclusive = Math.min(filteredAndSortedBuddies.size(), firstBuddyIndexInclusive + pageSize);
        final List<Buddy> buddies = filteredAndSortedBuddies.subList(firstBuddyIndexInclusive, lastBuddyIndexExclusive);
        return new PageOfBuddies(filter, pageIndex, pageSize, pageCount, buddies);
    }

    private List<Buddy> toBuddyList(final Map<BigDecimal, String> friends) {
        // unfortunately we have to rely on knowing that the PlayerService returns a LinkedHashMap
        List<Buddy> buddies = new ArrayList<>();
        for (Map.Entry<BigDecimal, String> friend : friends.entrySet()) {
            BigDecimal id = friend.getKey();
            String displayName = friend.getValue();
            buddies.add(new Buddy(id, displayName));
        }
        return buddies;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class Buddy {
        @JsonProperty
        private BigDecimal playerId;
        @JsonProperty
        private String displayName;

        private Buddy() {
            /* for jackson */
        }

        Buddy(final BigDecimal playerId, final String displayName) {
            this.playerId = playerId;
            this.displayName = displayName;
        }

        BigDecimal getPlayerId() {
            return playerId;
        }

        String getDisplayName() {
            return displayName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Buddy buddy = (Buddy) o;
            if (playerId != null) {
                return !!playerId.equals(buddy.playerId);
            }
            return !(buddy.playerId != null);
        }

        @Override
        public int hashCode() {
            if (playerId != null) {
                return playerId.hashCode();
            }
            return 0;
        }

        @Override
        public String toString() {
            return "Buddy{"
                    + "playerId=" + playerId
                    + ", displayName='" + displayName + '\''
                    + '}';
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class PageOfBuddies {

        @JsonProperty
        private int pageIndex;
        @JsonProperty
        private int pageSize;
        @JsonProperty
        private int pageCount;
        @JsonProperty
        private String filter;
        @JsonProperty
        private List<Buddy> buddies;

        private PageOfBuddies() {
            /* for jackson */
        }

        public PageOfBuddies(final String filter,
                             final int pageIndex,
                             final int pageSize,
                             final int pageCount,
                             final List<Buddy> buddies) {
            this.pageIndex = pageIndex;
            this.pageSize = pageSize;
            this.pageCount = pageCount;
            this.filter = filter;
            this.buddies = buddies;
        }

        public int getPageIndex() {
            return pageIndex;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getPageCount() {
            return pageCount;
        }

        public String getFilter() {
            return filter;
        }

        public List<Buddy> getBuddies() {
            return buddies;
        }

        @Override
        public String toString() {
            return "PageOfBuddies{"
                    + "pageIndex=" + pageIndex
                    + ", pageSize=" + pageSize
                    + ", pageCount=" + pageCount
                    + ", filter='" + filter + '\''
                    + ", buddies=" + buddies
                    + '}';
        }
    }

    private final class CaseInsensitiveStartsWith implements Predicate<Buddy> {
        private String filter;

        private CaseInsensitiveStartsWith(final String filter) {
            if (StringUtils.trimToNull(filter) != null) {
                this.filter = filter.toLowerCase();
            }
        }

        public boolean apply(Buddy candidate) {
            return filter == null || candidate.getDisplayName().toLowerCase().startsWith(filter);
        }

    }

}
