package com.yazino.web.interceptor;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class EnvironmentPropertiesInterceptor extends HandlerInterceptorAdapter {

    private final Map<String, String> environmentProperties;

    public EnvironmentPropertiesInterceptor(final Map<String, String> environmentProperties) {
        notNull(environmentProperties, "environmentProperties is null");
        this.environmentProperties = environmentProperties;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            for (Map.Entry<String, String> entry : environmentProperties.entrySet()) {
                modelAndView.addObject(entry.getKey(), entry.getValue());
            }
        }
        super.postHandle(request, response, handler, modelAndView);
    }
}
