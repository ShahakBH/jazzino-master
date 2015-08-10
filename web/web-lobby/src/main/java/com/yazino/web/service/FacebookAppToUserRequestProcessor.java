package com.yazino.web.service;

import com.restfb.FacebookClient;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;

import java.util.List;

/*
 * Interface for classes that know how to process different types of FacebookAppRequests
 */
public interface FacebookAppToUserRequestProcessor {
    void processFacebookAppToUserRequest(List<FacebookAppToUserRequest> facebookAppToUserRequests,
                                         FacebookClient fbClient);
}
