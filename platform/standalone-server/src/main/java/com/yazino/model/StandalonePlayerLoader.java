package com.yazino.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("unchecked")
public class StandalonePlayerLoader {
    private final ObjectMapper mapper = new ObjectMapper();

    public StandalonePlayerLoader() {
        mapper.registerModule(new JodaModule());
    }

    private final Map<BigDecimal, Set<BigDecimal>> relationships = new HashMap<>();

    public List<StandalonePlayer> loadPlayers() throws IOException {
        final List<StandalonePlayer> result = new ArrayList<>();
        final File src = new File("/etc/yazino/players.json");
        if (!src.exists()) {
            return Collections.emptyList();
        }
        final Map[] json = mapper.readValue(src, Map[].class);
        for (Map<String, Object> map : json) {
            final BigDecimal playerId = BigDecimal.valueOf((Integer) map.get("id"));
            final String name = (String) map.get("name");
            final BigDecimal balance = BigDecimal.valueOf(1000);
            final StandalonePlayer player = new StandalonePlayer(playerId, name, balance);
            final Set<BigDecimal> friendIds = new HashSet<>();
            final List<Integer> friends = (List<Integer>) map.get("friends");
            for (Integer friendId : friends) {
                friendIds.add(BigDecimal.valueOf(friendId));
            }
            this.relationships.put(playerId, friendIds);
            result.add(player);
        }
        return result;
    }

    public Map<BigDecimal, Set<BigDecimal>> getRelationships() {
        return relationships;
    }
}
