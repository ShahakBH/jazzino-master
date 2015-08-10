package com.yazino.web.util;

import com.yazino.web.interceptor.ReloadableEnvironmentPropertiesInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang3.Validate.notNull;

@Service("exceptionHandler")
public class LobbyExceptionHandler implements HandlerExceptionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(LobbyExceptionHandler.class);

    private enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static final ErrorMetaData DEFAULT_META_DATA = new ErrorMetaData(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LogLevel.ERROR, true);
    private static final String NONE = "<none>";

    private final CommonPropertiesHelper commonPropertiesHelper;
    private final ReloadableEnvironmentPropertiesInterceptor reloadableEnvironmentPropertiesInterceptor;
    private final String errorHandlingView;
    private final WebApiResponses webApiResponses;

    private final Map<String, ErrorMetaData> exceptionMetaData = new HashMap<>();
    private final Map<Pattern, ErrorMetaData> regexpMetaData = new HashMap<>();

    {
        exceptionMetaData.put("org.eclipse.jetty.io.EofException", statusLevelTrace(SC_INTERNAL_SERVER_ERROR, LogLevel.WARN, false));
        exceptionMetaData.put(NoSuchRequestHandlingMethodException.class.getName(), statusLevelTrace(SC_NOT_FOUND, LogLevel.ERROR, true));
        exceptionMetaData.put(HttpRequestMethodNotSupportedException.class.getName(), statusLevelTrace(SC_METHOD_NOT_ALLOWED, LogLevel.WARN, true));
        exceptionMetaData.put(HttpMediaTypeNotSupportedException.class.getName(), statusLevelTrace(SC_UNSUPPORTED_MEDIA_TYPE, LogLevel.WARN, true));
        exceptionMetaData.put(HttpMediaTypeNotAcceptableException.class.getName(), statusLevelTrace(SC_NOT_ACCEPTABLE, LogLevel.WARN, true));
        exceptionMetaData.put(MissingServletRequestParameterException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(ServletRequestBindingException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(TypeMismatchException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(HttpMessageNotReadableException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(MethodArgumentNotValidException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(MissingServletRequestPartException.class.getName(), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, true));
        exceptionMetaData.put(ConversionNotSupportedException.class.getName(), statusLevelTrace(SC_INTERNAL_SERVER_ERROR, LogLevel.ERROR, true));
        exceptionMetaData.put(HttpMessageNotWritableException.class.getName(), statusLevelTrace(SC_INTERNAL_SERVER_ERROR, LogLevel.ERROR, true));

        regexpMetaData.put(Pattern.compile("\"text; charset=utf-8\" does not contain '/'"), statusLevelTrace(SC_BAD_REQUEST, LogLevel.WARN, false));
    }

    @Autowired(required = true)
    public LobbyExceptionHandler(
            @Qualifier("commonPropertiesHelper") final CommonPropertiesHelper commonPropertiesHelper,
            final ReloadableEnvironmentPropertiesInterceptor reloadableEnvironmentPropertiesInterceptor,
            @Value("${strata.lobby.errorHandling}") final String errorHandlingView,
            final WebApiResponses webApiResponses) {
        notNull(commonPropertiesHelper, "commonPropertiesHelper may not be null");
        notNull(reloadableEnvironmentPropertiesInterceptor, "reloadableEnvironmentPropertiesInterceptor may not be null");
        notNull(errorHandlingView, "errorHandlingView may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.commonPropertiesHelper = commonPropertiesHelper;
        this.reloadableEnvironmentPropertiesInterceptor = reloadableEnvironmentPropertiesInterceptor;
        this.errorHandlingView = errorHandlingView;
        this.webApiResponses = webApiResponses;
    }

    public ModelAndView resolveException(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Object handler,
                                         final Exception e) {
        log(e, handler, request);

        response.setStatus(metaDataFor(e).getStatusCode());

        if (prefersJson(request)) {
            final Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("handler", nameOf(handler));
            errorResult.put("requestUrl", pathOf(request));
            errorResult.put("userAgent", userAgentOf(request));
            errorResult.put("stackTrace", asString(e));
            writeAsJson(response, errorResult);
            return null;

        } else {
            final ModelAndView modelAndView = new ModelAndView(errorHandlingView);
            setCommonPropertiesInModelAndView(request, response, modelAndView);

            modelAndView.addObject("message", asString(e));
            modelAndView.addObject("handler", nameOf(handler));
            modelAndView.addObject("requestUrl", pathOf(request));
            modelAndView.addObject("userAgent", userAgentOf(request));

            if (LOG.isDebugEnabled()) {
                modelAndView.addObject("debug", "true");
            }

            LOG.debug("Returning {}", modelAndView);

            return modelAndView;
        }
    }

    private void writeAsJson(final HttpServletResponse response, final Map<String, Object> errorResult) {
        try {
            webApiResponses.write(response, errorResult);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private boolean prefersJson(final HttpServletRequest request) {
        final String acceptHeader = request.getHeader("Accept");
        LOG.debug("Accept header is {}", acceptHeader);
        return acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html");
    }

    private void log(final Exception e,
                     final Object handler,
                     final HttpServletRequest request) {
        final ErrorMetaData metaData = metaDataFor(e);
        if (metaData.isStackTraceIncluded()) {
            log(metaData.getLogLevel(), "Lobby catch-all triggered for handler {}; Path: {}; User-Agent: {}",
                    nameOf(handler), pathOf(request), userAgentOf(request), e);
        } else {
            log(metaData.getLogLevel(), "Lobby catch-all triggered for handler {}; Path: {}; User-Agent: {}; exception message: {}",
                    nameOf(handler), pathOf(request), userAgentOf(request), e.getMessage());
        }
    }

    private String pathOf(final HttpServletRequest request) {
        final StringBuffer requestUrl = request.getRequestURL();
        if (request.getQueryString() != null) {
            requestUrl.append("?").append(request.getQueryString());
        }
        return requestUrl.toString();
    }

    private void log(final LogLevel level,
                     final String message,
                     final Object... args) {
        switch (level) {
            case DEBUG:
                LOG.debug(message, args);
                break;
            case INFO:
                LOG.info(message, args);
                break;
            case WARN:
                LOG.warn(message, args);
                break;
            default:
                LOG.error(message, args);
                break;
        }
    }

    private String userAgentOf(final HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            return userAgent;
        }
        return NONE;
    }

    private String nameOf(final Object handler) {
        if (handler != null) {
            return handler.getClass().getName();
        }
        return NONE;
    }

    private String asString(final Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private ErrorMetaData metaDataFor(final Throwable t) {
        ErrorMetaData metaData;
        if (t != null) {
            metaData = exceptionMetaData.get(t.getClass().getName());
            if (metaData != null) {
                return metaData;
            }

            for (Pattern pattern : regexpMetaData.keySet()) {
                if (t.getMessage() != null && pattern.matcher(t.getMessage()).find()) {
                    return regexpMetaData.get(pattern);
                }
            }
        }

        return DEFAULT_META_DATA;
    }

    public void setCommonPropertiesInModelAndView(final HttpServletRequest request,
                                                  final HttpServletResponse response,
                                                  final ModelAndView modelAndView) {
        try {
            LOG.debug("Setting common properties");

            commonPropertiesHelper.setupCommonProperties(request, response, modelAndView);
            reloadableEnvironmentPropertiesInterceptor.postHandle(request, response, this, modelAndView);

        } catch (Throwable t) {
            LOG.warn("Error setting common properties", t);
        }
    }

    private ErrorMetaData statusLevelTrace(final int statusCode,
                                           final LogLevel level,
                                           final boolean stackTraceIncluded) {
        return new ErrorMetaData(statusCode, level, stackTraceIncluded);
    }

    private static final class ErrorMetaData {
        private final int statusCode;
        private final LogLevel logLevel;
        private boolean stackTraceIncluded;

        private ErrorMetaData(final int statusCode, final LogLevel logLevel, final boolean stackTraceIncluded) {
            notNull(logLevel, "logLevel may not be null");

            this.statusCode = statusCode;
            this.logLevel = logLevel;
            this.stackTraceIncluded = stackTraceIncluded;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public LogLevel getLogLevel() {
            return logLevel;
        }

        public boolean isStackTraceIncluded() {
            return stackTraceIncluded;
        }
    }
}
