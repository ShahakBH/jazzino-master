package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.message.NovomaticGameDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameListBuilder {

    public List<NovomaticGameDefinition> buildFromMap(Map map) {
        final List<Map> games = (List<Map>) map.get("games");
        final List<NovomaticGameDefinition> result = new ArrayList<NovomaticGameDefinition>(games.size());
        for (Map game : games) {
            result.add(new NovomaticGameDefinition((Long) game.get("id"), (String) game.get("name")));
        }
        return result;
    }
}
