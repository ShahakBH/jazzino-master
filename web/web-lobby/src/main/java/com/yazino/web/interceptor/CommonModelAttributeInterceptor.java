package com.yazino.web.interceptor;

import com.yazino.web.service.SystemMessageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CommonModelAttributeInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CommonModelAttributeInterceptor.class);

    @Resource
    private final SystemMessageService systemMessageService;

    @Autowired(required = true)
    public CommonModelAttributeInterceptor(
            @Qualifier("systemMessageService") final SystemMessageService systemMessageService) {
        this.systemMessageService = systemMessageService;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        LOG.debug("executing postHandle");
        if (modelAndView != null) {
            modelAndView.addObject("systemMessage", StringUtils.trim(systemMessageService.getLatestSystemMessage()));
        }
        super.postHandle(request, response, handler, modelAndView);
    }
}
