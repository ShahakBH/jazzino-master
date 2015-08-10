package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerReaderResult implements Serializable {
    private static final long serialVersionUID = 1;

    public static class RpxCredential {

        private final String provider;
        private final String externalId;
        private final List<BigDecimal> matchingPlayerIds;

        public RpxCredential(final String provider, final String externalId) {
            this.provider = provider;
            this.externalId = externalId;
            this.matchingPlayerIds = null;
        }

        public RpxCredential(final String provider,
                             final String externalId,
                             final List<BigDecimal> matchingPlayerIds) {
            this.provider = provider;
            this.externalId = externalId;
            this.matchingPlayerIds = matchingPlayerIds;
        }

        public List<BigDecimal> getMatchingPlayerIds() {
            return matchingPlayerIds;
        }

        public String getProvider() {
            return provider;
        }

        public String getExternalId() {
            return externalId;
        }

        public boolean hasPlayerIds() {
            return matchingPlayerIds != null && !matchingPlayerIds.isEmpty();
        }

        @Override
        // DONT auto generate this equals - require match on matchingPlayerIds
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final RpxCredential that = (RpxCredential) o;

            if (externalId != null) {
                if (!externalId.equals(that.externalId)) {
                    return false;
                }
            } else {
                if (that.externalId != null) {
                    return false;
                }
            }
            if (matchingPlayerIds != null) {
                if (matchingPlayerIds.size() != that.matchingPlayerIds.size()
                        || !matchingPlayerIds.containsAll(that.matchingPlayerIds)) {
                    return false;
                }
            } else {
                if (that.matchingPlayerIds != null) {
                    return false;
                }
            }
            if (provider != null) {
                if (!provider.equals(that.provider)) {
                    return false;
                }
            } else {
                if (that.provider != null) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(provider)
                    .append(externalId)
                    .append(matchingPlayerIds)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return "RpxCredential{"
                    + "provider='" + provider + '\''
                    + ", externalId='" + externalId + '\''
                    + ", matchingPlayerIds=" + matchingPlayerIds
                    + '}';
        }
    }

    private Set<BigDecimal> playerIds;

    private List<RpxCredential> notMatched;
    private List<RpxCredential> multipleMatches;
    private List<List<String>> invalidInputLines;

    public PlayerReaderResult() {
        playerIds = new HashSet<BigDecimal>();
        notMatched = new ArrayList<RpxCredential>();
        multipleMatches = new ArrayList<RpxCredential>();
        invalidInputLines = new ArrayList<List<String>>();
    }

    public void addMatched(final BigDecimal playerId) {
        playerIds.add(playerId);
    }

    public void addNotMatched(final String provider,
                              final String externalId) {
        notMatched.add(new RpxCredential(provider, externalId));
    }

    public void addMultipleMatch(final String provider,
                                 final String externalId,
                                 final List<BigDecimal> matchedPlayerIds) {
        multipleMatches.add(new RpxCredential(provider, externalId, matchedPlayerIds));
    }

    public void addInvalidInputLine(final List<String> lineParts) {
        invalidInputLines.add(new ArrayList<String>(lineParts));
    }

    public Set<BigDecimal> getPlayerIds() {
        return playerIds;
    }

    public List<RpxCredential> getNotMatched() {
        return notMatched;
    }

    public List<RpxCredential> getMultipleMatches() {
        return multipleMatches;
    }

    public List<List<String>> getInvalidInputLines() {
        return invalidInputLines;
    }

    public boolean hasRpxErrors() {
        return !notMatched.isEmpty() || !multipleMatches.isEmpty();
    }

    public boolean hasInputLineErrors() {
        return !invalidInputLines.isEmpty();
    }

    @Override
    public String toString() {
        return "PlayerReaderResult{"
                + "playerIds=" + playerIds
                + ", notMatched=" + notMatched
                + ", multipleMatches=" + multipleMatches
                + ", invalidInputLines=" + invalidInputLines
                + '}';
    }
}

