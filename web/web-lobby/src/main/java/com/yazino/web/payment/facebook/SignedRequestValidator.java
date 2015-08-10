package com.yazino.web.payment.facebook;

import com.yazino.web.domain.facebook.SignedRequest;
import org.springframework.stereotype.Component;

@Component
public class SignedRequestValidator {
    public Boolean validate(String signedRequest, String secretKey) {
        final SignedRequest request = new SignedRequest(signedRequest, secretKey);
        return (request.size() > 0);
    }
}
