package com.yazino.bi.operations.interceptor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.yazino.bi.operations.util.SecurityInformationHelper;
import com.yazino.bi.operations.security.UserInformationCommand;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.apache.commons.lang3.Validate.notNull;

public class UserInformationInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(UserInformationInterceptor.class);

    private final SecurityInformationHelper security;

    @Autowired
    public UserInformationInterceptor(final SecurityInformationHelper security) {
        notNull(security, "security may not be null");
        this.security = security;
    }

    @Override
    public void postHandle(final HttpServletRequest request, final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {

        if (modelAndView != null) {
            modelAndView.addObject("userInfo", userInformation());
        }
    }

    private UserInformationCommand userInformation() {
        final UserInformationCommand command = new UserInformationCommand();
        command.setRoles(security.getAccessRoles());
        command.setUsername(security.getCurrentUser());

        final InputStream pomPropertiesStream = getClass().getResourceAsStream("/version.properties");
        try {
            if (pomPropertiesStream != null) {
                final Properties pomProperties = new Properties();
                pomProperties.load(pomPropertiesStream);
                command.setVersion(pomProperties.getProperty("version"));
            }
        } catch (IOException e) {
            LOG.warn("Failed to read version", e);

        } finally {
            IOUtils.closeQuietly(pomPropertiesStream);
        }
        return command;
    }
}
