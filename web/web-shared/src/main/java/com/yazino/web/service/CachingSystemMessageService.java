package com.yazino.web.service;

import com.yazino.platform.community.CommunityService;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

@Service("systemMessageService")
public class CachingSystemMessageService implements SystemMessageService {
    private static final int THIRTY_SECONDS = 30000;

    private final CommunityService communityService;
    private static String message;
    private long intervalBetweenCaching = THIRTY_SECONDS;
    private ConcurrentHashMap<String, CachedSystemMessage> cache = new ConcurrentHashMap<String, CachedSystemMessage>();

    @Autowired
    public CachingSystemMessageService(final CommunityService communityService) {
        notNull(communityService, "communityService may not be null");

        this.communityService = communityService;
    }

    @Override
    public String getLatestSystemMessage() {
        if (!cache.containsKey("cache")) {
            message = communityService.getLatestSystemMessage();
            cache.put("cache", new CachedSystemMessage(message));
        }

        if (DateTimeUtils.currentTimeMillis() > cache.get("cache").getCreation() + intervalBetweenCaching) {
            message = communityService.getLatestSystemMessage();
            cache.replace("cache", new CachedSystemMessage(message));

        }
        return message;
    }

    public void setIntervalBetweenCaching(final long intervalBetweenCaching) {
        this.intervalBetweenCaching = intervalBetweenCaching;
    }

    public class CachedSystemMessage {
        private String systemMessage;
        private long creation;

        public CachedSystemMessage(final String systemMessage) {
            this.systemMessage = systemMessage;
            this.creation = DateTimeUtils.currentTimeMillis();
        }

        public long getCreation() {
            return creation;
        }
    }


}
