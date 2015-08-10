package com.yazino.spring.security;

import com.yazino.web.security.WhiteListDomain;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import static org.apache.commons.lang3.Validate.notNull;

public class WhitelistBeanPostProcessor implements BeanPostProcessor {

    private final WhiteListDomain whiteListDomain;

    @Autowired
    public WhitelistBeanPostProcessor(WhiteListDomain whiteListDomain) {
        this.whiteListDomain = whiteListDomain;
        notNull(whiteListDomain, "whiteListDomain may not be null");
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        processAnnotation(bean.getClass().getAnnotation(AllowPublicAccess.class), bean.getClass());

        for (Method method : bean.getClass().getMethods()) {
            processAnnotation(method.getAnnotation(AllowPublicAccess.class), method);
        }

        return bean;
    }

    private void processAnnotation(final AllowPublicAccess allowPublicAccess,
                                   final AnnotatedElement element) {
        if (allowPublicAccess == null) {
            return;
        }

        if (allowPublicAccess.value() == null || allowPublicAccess.value().length == 0) {
            final RequestMapping requestMapping = element.getAnnotation(RequestMapping.class);
            if (requestMapping == null) {
                throw new IllegalStateException("AllowPublicAccess annotation has no value "
                        + "and no RequestMapping is present with a value for element " + element);
            }

            for (String requestMappingUrl : requestMapping.value()) {
                whiteListDomain.addWhiteListedUrl(requestMappingUrl);
            }

        } else {
            for (String urlToWhiteList : allowPublicAccess.value()) {          // TODO inject whitelist class
                whiteListDomain.addWhiteListedUrl(urlToWhiteList);
            }
        }
    }
}
