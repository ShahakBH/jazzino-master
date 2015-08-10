package com.yazino.platform.processor.community;

import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.dispatcher.MemoryDocumentDispatcher;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.RelationshipActionRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RelationshipRequestProcessorIntegrationTest {

    @Autowired
    private GigaSpace gigaSpace;

    @Autowired
    private RelationshipRequestProcessor processor;

    @Autowired
    private MemoryDocumentDispatcher documentDispatcher;

    private BigDecimal playerId = new BigDecimal("12345");
    private BigDecimal relatedPlayerId = new BigDecimal("888");

    @Before
    @After
    public void clearSpace() {
        gigaSpace.clear(null);
        documentDispatcher.clear();
    }

    @Test
    @Transactional
    public void verifyProcessing() throws Exception {
        // write the data to be processed to the Space
        Player player1 = new Player(playerId);
        gigaSpace.write(player1);
        Player p2 = processor.processRelationshipRequest(
                new RelationshipActionRequest(playerId, relatedPlayerId, "mike", RelationshipAction.SET_EXTERNAL_FRIEND, false));
        assertNotNull("No data object was processed", p2);
        Assert.assertEquals(RelationshipType.FRIEND, p2.getRelationshipTo(new BigDecimal("888")).getType());
    }

}
