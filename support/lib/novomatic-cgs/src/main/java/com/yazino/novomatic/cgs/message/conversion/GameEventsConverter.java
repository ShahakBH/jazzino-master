package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticEventType;
import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameEventsConverter {

    public ArrayList<NovomaticEvent> convert(List<Map<String, Object>> record) {
        final ArrayList<NovomaticEvent> result = new ArrayList<NovomaticEvent>();
        for (Map<String, Object> item : record) {
            final String type = (String) item.get("type");
            final NovomaticEventType recordType = NovomaticEventType.fromNovomaticType(type);
            result.add(recordType.getConverter().convert(item));
        }
        return result;

    }
}
