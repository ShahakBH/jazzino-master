package com.yazino.web.domain.social;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service("playersInformationService")
public class PlayersInformationService {
    private final Map<PlayerInformationType, PlayerInformationRetriever> retrievers = new HashMap<>();

    @Autowired
    public PlayersInformationService(@Qualifier("playerInformationRetriever") final Set<PlayerInformationRetriever> retrievers) {
        for (PlayerInformationRetriever retriever : retrievers) {
            this.retrievers.put(retriever.getType(), retriever);
        }
    }

    public List<PlayerInformation> retrieve(final List<BigDecimal> playerIds,
                                            final String gameType,
                                            final PlayerInformationType... fields) {
        final List<PlayerInformation> result = new ArrayList<PlayerInformation>();
        for (BigDecimal playerId : playerIds) {
            final PlayerInformation.Builder builder = new PlayerInformation.Builder(playerId);
            addInformationToBuilder(playerId, gameType, builder, fields);
            result.add(builder.build());
        }
        return result;
    }

    private void addInformationToBuilder(final BigDecimal playerId,
                                         final String gameType,
                                         final PlayerInformation.Builder builder,
                                         final PlayerInformationType... fields) {
        for (PlayerInformationType field : fields) {
            final PlayerInformationRetriever retriever = retrievers.get(field);
            builder.withField(field, retriever.retrieveInformation(playerId, gameType));
        }
    }
}
