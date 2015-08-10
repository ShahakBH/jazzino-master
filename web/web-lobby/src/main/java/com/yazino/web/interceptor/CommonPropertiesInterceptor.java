package com.yazino.web.interceptor;

import com.yazino.web.util.CommonPropertiesHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

public class CommonPropertiesInterceptor extends HandlerInterceptorAdapter {
    private final CommonPropertiesHelper commonPropertiesHelper;

    @Autowired(required = true)
    public CommonPropertiesInterceptor(
            @Qualifier("commonPropertiesHelper") final CommonPropertiesHelper commonPropertiesHelper) {
        notNull(commonPropertiesHelper, "commonPropertiesHelper may not be null");

        this.commonPropertiesHelper = commonPropertiesHelper;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        commonPropertiesHelper.setupCommonProperties(request, response, modelAndView);
        super.postHandle(request, response, handler, modelAndView);
    }
}
