package com.yazino.platform.util.community;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

@Component("avatarTokeniser")
public class AvatarTokeniser {
    private static final String TOKEN_AVATAR = "%AVATARS%";
    private static final String TOKEN_CONTENT = "%CONTENT%";

    private final List<Pattern> avatarPatterns = new ArrayList<Pattern>();
    private final List<Pattern> contentPatterns = new ArrayList<Pattern>();

    private final String contentUrl;
    private final String avatarUrl;

    @Autowired
    public AvatarTokeniser(@Value("${strata.server.lobby.ssl.content}") final String contentUrl,
                           final List<String> contentPatterns,
                           @Value("${senet.web.avatars}") final String avatarUrl,
                           final List<String> avatarPatterns) {
        notNull(contentUrl, "contentUrl may not be null");
        notNull(avatarUrl, "avatarUrl may not be null");

        this.contentUrl = contentUrl;
        this.avatarUrl = avatarUrl;

        if (avatarPatterns != null) {
            for (String avatarPattern : avatarPatterns) {
                this.avatarPatterns.add(Pattern.compile(avatarPattern));
            }
        }
        if (contentPatterns != null) {
            for (String contentPattern : contentPatterns) {
                this.contentPatterns.add(Pattern.compile(contentPattern));
            }
        }
    }

    public String tokenise(final String untokenisedAvatarUrl) {
        if (untokenisedAvatarUrl == null) {
            return null;
        }

        String tokenisedAvatarUrl = untokenisedAvatarUrl;
        for (Pattern avatarPattern : avatarPatterns) {
            tokenisedAvatarUrl = avatarPattern.matcher(tokenisedAvatarUrl).replaceAll(TOKEN_AVATAR);
        }
        for (Pattern contentPattern : contentPatterns) {
            tokenisedAvatarUrl = contentPattern.matcher(tokenisedAvatarUrl).replaceAll(TOKEN_CONTENT);
        }
        return tokenisedAvatarUrl;
    }

    public String detokenise(final String tokenisedAvatarUrl) {
        if (tokenisedAvatarUrl == null) {
            return null;
        }

        return tokenisedAvatarUrl
                .replace(TOKEN_AVATAR, avatarUrl)
                .replace(TOKEN_CONTENT, contentUrl);
    }

}
