package strata.server.operations.repository;

import com.yazino.game.api.GameType;
import com.yazino.platform.table.GameTypeInformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GameTypeRepository {

    // Remember that this is only used by test. Look for real class inside bi-shared
    public Map<String, GameTypeInformation> getGameTypes() {
        final Map<String, GameTypeInformation> gameTypesToInformation = new HashMap<String, GameTypeInformation>();

        gameTypesToInformation.put("SLOTS", new GameTypeInformation(new GameType("SLOTS", "Wheel Deal", new HashSet<String>()), true));
        gameTypesToInformation.put("HIGH_STAKES", new GameTypeInformation(new GameType("HIGH_STAKES", "High Stakes", new HashSet<String>()), true));

        return gameTypesToInformation;
    }

}
