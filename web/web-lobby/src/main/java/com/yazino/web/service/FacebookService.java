package com.yazino.web.service;

public interface FacebookService {

    String getAccessTokenForGivenCode(String code, String appId, String appSecret, String redirectUri);

}
