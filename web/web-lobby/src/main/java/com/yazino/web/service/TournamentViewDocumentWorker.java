package com.yazino.web.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentView;
import com.yazino.platform.tournament.TournamentViewDetails;
import com.yazino.web.domain.TournamentDocument;
import com.yazino.web.domain.TournamentDocumentBuilder;
import com.yazino.web.domain.TournamentDocumentRequest;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.yazino.platform.tournament.TournamentViewDetails.Status.REGISTERING;
import static com.yazino.web.service.TournamentViewDocumentWorker.DocumentType.*;
import static org.apache.commons.lang3.Validate.notNull;

@Service("tournamentViewDocumentWorker")
public class TournamentViewDocumentWorker {

    public enum DocumentType {
        TOURNAMENT_STATUS,
        TOURNAMENT_OVERVIEW,
        TOURNAMENT_RANKS,
        TOURNAMENT_PLAYER,
        TOURNAMENT_PLAYERS
    }

    private interface DocumentStrategy {
        String buildDocument(TournamentView view, TournamentDocumentRequest documentRequest);
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<DocumentType, DocumentStrategy> strategies = new HashMap<>();

    public TournamentViewDocumentWorker() {
        mapper.registerModule(new JodaModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        strategies.put(TOURNAMENT_PLAYER, new PlayerDocumentStrategy());
        strategies.put(TOURNAMENT_OVERVIEW, new OverviewDocumentStrategy());
        strategies.put(TOURNAMENT_RANKS, new RanksDocumentStrategy());
        strategies.put(TOURNAMENT_STATUS, new FullDocumentStrategy());
        strategies.put(TOURNAMENT_PLAYERS, new PlayersDocumentStrategy());
    }

    public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
        notNull(view, "view is null");
        notNull(documentRequest, "documentRequest is null");
        return strategies.get(documentRequest.getRequestType()).buildDocument(view, documentRequest);
    }

    private String serialise(final Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialisation error", e);
        }
    }

    private class PlayerDocumentStrategy implements DocumentStrategy {
        @Override
        public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
            TournamentRankView rank = view.getPlayers().get(documentRequest.getPlayerId());
            if (rank == null) {
                rank = new TournamentRankView.Builder().playerId(documentRequest.getPlayerId()).build();
            }
            return serialise(rank);
        }
    }

    private class PlayersDocumentStrategy implements DocumentStrategy {
        @Override
        public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
            final TournamentDocumentBuilder builder = new TournamentDocumentBuilder();
            buildPlayers(builder, view.getPlayers().values(), documentRequest);
            builder.withSent(formatCreationTime(view.getCreationTime()));
            return serialise(builder.build());
        }
    }

    private class OverviewDocumentStrategy implements DocumentStrategy {

        @Override
        public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
            final TournamentDocument tournamentDocument =
                    new TournamentDocumentBuilder().withOverview(view.getOverview())
                            .withSent(formatCreationTime(view.getCreationTime()))
                            .build();
            return serialise(tournamentDocument);
        }
    }

    private class RanksDocumentStrategy implements DocumentStrategy {

        @Override
        public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
            final TournamentDocumentBuilder builder = new TournamentDocumentBuilder();
            buildRanks(builder, view.getOverview().getStatus(), view.getRanks(), documentRequest);
            builder.withSent(formatCreationTime(view.getCreationTime()));
            return serialise(builder.build());
        }
    }

    private void buildRanks(final TournamentDocumentBuilder builder,
                            final TournamentViewDetails.Status status,
                            final List<TournamentRankView> allRanks,
                            final TournamentDocumentRequest documentRequest) {
        if (REGISTERING == status) {
            builder.withRanks(allRanks);
        } else {
            List<TournamentRankView> ranks = new ArrayList<>();
            final Integer pageNumber = documentRequest.getPageNumber();
            int pages = 0;
            if (allRanks != null) {
                final Integer pageSize = documentRequest.getPageSize();
                pages = (int) Math.ceil(allRanks.size() / pageSize.doubleValue());
                if (pageNumber <= pages && pageNumber > 0) {
                    final int startIndex = (pageNumber - 1) * pageSize;
                    final int endIndexExclusive = Math.min(startIndex + pageSize, allRanks.size());
                    ranks = allRanks.subList(startIndex, endIndexExclusive);
                }
            }
            builder.withRanks(ranks).withRankPage(pageNumber).withRankPages(pages);
        }
    }

    private void buildPlayers(final TournamentDocumentBuilder builder,
                              final Collection<TournamentRankView> values,
                              final TournamentDocumentRequest documentRequest) {
        List<TournamentRankView> players = new ArrayList<>();
        final Integer pageNumber = documentRequest.getPageNumber();
        int pages = 0;
        if (CollectionUtils.isNotEmpty(values)) {
            final List<TournamentRankView> playersOrderedByName = new ArrayList<>(values);
            Collections.sort(playersOrderedByName, new Comparator<TournamentRankView>() {
                @Override
                public int compare(final TournamentRankView player1, final TournamentRankView player2) {
                    return player1.getPlayerName().compareTo(player2.getPlayerName());
                }
            });
            final Integer pageSize = documentRequest.getPageSize();
            pages = (int) Math.ceil(playersOrderedByName.size() / pageSize.doubleValue());
            if (pageNumber <= pages && pageNumber > 0) {
                final int startIndex = (pageNumber - 1) * pageSize;
                final int endIndexExclusive = Math.min(startIndex + pageSize, playersOrderedByName.size());
                players = playersOrderedByName.subList(startIndex, endIndexExclusive);
            }
        }
        builder.withPlayers(players).withPlayerPage(pageNumber).withPlayerPages(pages);
    }

    private class FullDocumentStrategy implements DocumentStrategy {
        @Override
        public String buildDocument(final TournamentView view, final TournamentDocumentRequest documentRequest) {
            final TournamentDocumentBuilder builder = new TournamentDocumentBuilder();
            buildRanks(builder, view.getOverview().getStatus(), view.getRanks(), documentRequest);
            if (REGISTERING == view.getOverview().getStatus()) {
                buildPlayers(builder, view.getPlayers().values(), documentRequest);
            }
            builder.withSent(formatCreationTime(view.getCreationTime()))
                    .withRounds(view.getRounds())
                    .withOverview(view.getOverview());
            return serialise(builder.build());
        }
    }

    private String formatCreationTime(final long creationTime) {
        return new DateTime(creationTime).toString("yyyyMMdd-HHmm-ss-SSS");
    }
}
