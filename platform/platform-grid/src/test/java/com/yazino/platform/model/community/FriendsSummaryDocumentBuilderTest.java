package com.yazino.platform.model.community;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static net.sf.json.test.JSONAssert.assertEquals;
import static net.sf.json.test.JSONAssert.assertNotNull;

@SuppressWarnings("unchecked")
public class FriendsSummaryDocumentBuilderTest {

    private final JsonHelper jsonHelper = new JsonHelper();
    private FriendsSummaryDocumentBuilder underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new FriendsSummaryDocumentBuilder();
    }

    @Test
    public void shouldBuildDocument() {
        final List<BigDecimal> requests = asList(valueOf(4), valueOf(5), valueOf(6));
        final List<BigDecimal> onlineFriends = asList(valueOf(1), valueOf(2));
        final List<BigDecimal> offlineFriends = asList(valueOf(1), valueOf(2), valueOf(3));
        final Document document = underTest.build(17, 35, onlineFriends, offlineFriends, requests);
        final Map<String, Object> summary = deserialise(document);

        assertEquals(35, summary.get("friends"));
        assertEquals(17, summary.get("online"));
        checkElementsPresent(summary, "requests", requests);
        checkElementsPresent(summary, "online_ids", onlineFriends);
        checkElementsPresent(summary, "offline_ids", offlineFriends);
    }

    private Map<String, Object> deserialise(Document document) {
        assertNotNull(document.getBody());
        final HashMap map = jsonHelper.deserialize(HashMap.class, document.getBody());
        assertNotNull(map);
        return (Map<String, Object>) map.get("summary");
    }

    private void checkElementsPresent(Map<String, Object> summary, String fieldName, List<BigDecimal> elements) {
        final List<Integer> docRequests = (List<Integer>) summary.get(fieldName);
        assertNotNull(docRequests);
        final List<BigDecimal> actualRequests = new ArrayList<BigDecimal>();
        for (Integer docRequest : docRequests) {
            actualRequests.add(BigDecimal.valueOf(docRequest));
        }
        assertEquals(elements, actualRequests);
    }

}
