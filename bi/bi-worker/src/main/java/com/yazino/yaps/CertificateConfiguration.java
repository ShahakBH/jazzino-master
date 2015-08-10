package com.yazino.yaps;

import com.yazino.mobile.yaps.config.TypedMapBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import java.util.Map;


@Configuration
public class CertificateConfiguration {

    @Autowired
    @Qualifier("bundleCertificates")
    private TypedMapBean<String, String> bundleCertificates;

    public Map<String, String> certificates() {
        return bundleCertificates.getSource();
    }

}
