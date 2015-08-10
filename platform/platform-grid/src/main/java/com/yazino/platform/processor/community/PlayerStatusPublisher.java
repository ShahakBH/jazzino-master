package com.yazino.platform.processor.community;


import com.yazino.platform.model.community.PublishStatusRequest;

public interface PlayerStatusPublisher {
    void savePublishStatusRequest(PublishStatusRequest request);
}
