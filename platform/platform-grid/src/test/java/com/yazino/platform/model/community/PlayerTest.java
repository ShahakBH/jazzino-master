package com.yazino.platform.model.community;

import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;

public class PlayerTest {

    private Player unit;

    @Before
    public void setUp() {
        unit = new Player(BigDecimal.valueOf(102), "Test", BigDecimal.valueOf(34), "aPicture", null, null, null);
    }

    @Test
    public void onlyMatchingRelatedPlayersReturnedWhenNonNullFilterSupplied() {
        unit.setRelationships(new HashMap<BigDecimal, Relationship>());
        unit.getRelationships().put(BigDecimal.valueOf(10), new Relationship("10", RelationshipType.FRIEND));
        unit.getRelationships().put(BigDecimal.valueOf(11), new Relationship("11", RelationshipType.IGNORED));
        unit.getRelationships().put(BigDecimal.valueOf(12), new Relationship("12", RelationshipType.INVITATION_SENT));

        final Map<BigDecimal, Relationship> relationShips = unit.listRelationships(RelationshipType.FRIEND, RelationshipType.INVITATION_SENT);

        assertEquals(2, relationShips.size());
        assertEquals(RelationshipType.FRIEND, relationShips.get(BigDecimal.valueOf(10)).getType());
        assertEquals(RelationshipType.INVITATION_SENT, relationShips.get(BigDecimal.valueOf(12)).getType());
    }

    @Test
    @SuppressWarnings({"NullArgumentToVariableArgMethod"})
    public void allRelatedPlayersReturnedWhenNullFilterSupplied() {
        unit.setRelationships(new HashMap<BigDecimal, Relationship>());
        unit.getRelationships().put(BigDecimal.valueOf(10), new Relationship("10", RelationshipType.FRIEND));
        unit.getRelationships().put(BigDecimal.valueOf(11), new Relationship("11", RelationshipType.IGNORED));
        unit.getRelationships().put(BigDecimal.valueOf(12), new Relationship("12", RelationshipType.INVITATION_SENT));

        final Map<BigDecimal, Relationship> relationShips = unit.listRelationships();

        assertEquals(3, relationShips.size());
        assertEquals(RelationshipType.FRIEND, relationShips.get(BigDecimal.valueOf(10)).getType());
        assertEquals(RelationshipType.IGNORED, relationShips.get(BigDecimal.valueOf(11)).getType());
        assertEquals(RelationshipType.INVITATION_SENT, relationShips.get(BigDecimal.valueOf(12)).getType());
    }

    @Test
    public void shouldListFriends() {
        final Player player = new Player();
        player.setRelationship(BigDecimal.valueOf(2), new Relationship("player 2", RelationshipType.FRIEND));
        player.setRelationship(BigDecimal.valueOf(3), new Relationship("player 3", RelationshipType.IGNORED));
        player.setRelationship(BigDecimal.valueOf(4), new Relationship("player 4", RelationshipType.FRIEND));
        final HashMap<BigDecimal, String> expectedFriends = newHashMap();
        expectedFriends.put(BigDecimal.valueOf(2), "player 2");
        expectedFriends.put(BigDecimal.valueOf(4), "player 4");
        final Map<BigDecimal, String> actualFriends = player.retrieveFriends();
        assertEquals(expectedFriends, actualFriends);
    }

}
