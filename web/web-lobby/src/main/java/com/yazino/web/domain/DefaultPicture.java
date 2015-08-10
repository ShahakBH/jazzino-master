package com.yazino.web.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service("defaultPicture")
public class DefaultPicture {

    private final String url;

    @Autowired
    public DefaultPicture(@Value("${senet.web.content}") final String contentUrl,
                          @Value("${senet.web.defaultAvatarPath}") final String path) {
        notNull(contentUrl, "contentUrl is null");
        if (path == null) {
            this.url = contentUrl;
        } else {
            this.url = contentUrl + path;
        }
    }

    public String getUrl() {
        return url;
    }
}
