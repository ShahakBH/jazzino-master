package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PushMessageWorkerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageWorkerConfiguration.class);

    @Value("${strata.worker.yaps.config.pushservice.host}")
    private String host;
    @Value("${strata.worker.yaps.config.apple.connections}")
    private int maxActive;
    @Value("${yaps.config.cert-dir}")
    private String certificateDirectory;

    @Autowired
    private CertificateConfiguration certificateConfiguration;

    @Autowired
    private MobileDeviceService mobileDeviceDao;

    public AppleConnectionFactory connectionFactoryWithSocketFactory(AppleSocketFactory socketFactory) {
        return new AppleConnectionFactory(socketFactory);
    }

    public SecurityConfig securityConfigWithCertificate(String certificate) throws Exception {
        LOG.debug("yaps.config.cert-dir : %s", certificateDirectory);
        SecurityConfig securityConfig = new SecurityConfig(certificateDirectory, certificate);
        securityConfig.initialise();
        return securityConfig;
    }

    public GenericObjectPoolConfigBean poolConfig() {
        GenericObjectPoolConfigBean config = new GenericObjectPoolConfigBean();
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setMaxActive(maxActive);
        config.setLifo(false);
        return config;
    }

    public AppleSocketFactory socketFactoryWithSecurity(SecurityConfig config, int port) {
        return new AppleSocketFactory(host, port, config);
    }

    public Map<String, PushService> pushServices() throws Exception {
        final GenericObjectPoolConfigBean poolConfig = poolConfig();

        return buildServices(new ServiceBuilder<PushService>() {
            @Override
            public PushService buildService(String bundle, AppleSocketFactory socketFactory) {
                AppleConnectionFactory connectionFactory = connectionFactoryWithSocketFactory(socketFactory);
                GenericObjectPool<AppleConnection> pool = new GenericObjectPool<AppleConnection>(connectionFactory, poolConfig);
                return new PushService(pool);
            }
        }, 2195);
    }

    public Map<String, FeedbackService> feedbackServices() throws Exception {
        return buildServices(new ServiceBuilder<FeedbackService>() {
            @Override
            public FeedbackService buildService(String bundle, AppleSocketFactory factory) {
                return new FeedbackService(bundle, factory, mobileDeviceDao);
            }
        }, 2196);
    }

    private <V> Map<String, V> buildServices(ServiceBuilder<V> builder, int port) throws Exception {
        Map<String, String> certificates = certificateConfiguration.certificates();
        Map<String, V> services = new HashMap<String, V>(certificates.size());

        for (String bundle : certificates.keySet()) {
            String certificate = certificates.get(bundle);
            SecurityConfig securityConfig = securityConfigWithCertificate(certificate);
            AppleSocketFactory socketFactory = socketFactoryWithSecurity(securityConfig, port);

            V service = builder.buildService(bundle, socketFactory);
            LOG.info("Built {} for bundle {}", service.getClass().getSimpleName(), bundle);
            services.put(bundle, service);
        }
        return services;

    }

    private interface ServiceBuilder<V> {
        V buildService(String bundle, AppleSocketFactory factory);
    }

    @Bean
    public ScheduledFeedbackTask feedbackTask() throws Exception {
        return new ScheduledFeedbackTask(feedbackServices());
    }


}
