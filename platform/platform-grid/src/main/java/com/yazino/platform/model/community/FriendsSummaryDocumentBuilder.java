package com.yazino.platform.model.community;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Service
public class FriendsSummaryDocumentBuilder {

    public static final Map<String, String> EMPTY_HEADERS = Collections.emptyMap();
    public static final String BODY_FORMAT = "{\"summary\":"
            + "{\"friends\":%s,\"online\":%s,\"online_ids\":[%s],\"offline_ids\":[%s],\"requests\":[%s]}}";

    public Document build(final int totalOnlineFriends,
                          final int totalFriends,
                          final Collection<BigDecimal> onlineFriends,
                          final Collection<BigDecimal> offlineFriends,
                          final Collection<BigDecimal> requests) {
        return new Document(DocumentType.FRIENDS_SUMMARY.name(),
                String.format(BODY_FORMAT,
                        totalFriends,
                        totalOnlineFriends,
                        StringUtils.join(onlineFriends, ","),
                        StringUtils.join(offlineFriends, ","),
                        StringUtils.join(requests, ",")),
                EMPTY_HEADERS);
    }
}
