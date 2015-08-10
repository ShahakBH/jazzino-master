package com.yazino.web.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.yazino.web.interceptor.ReloadableEnvironmentPropertiesInterceptor;
import org.eclipse.jetty.io.EofException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.ServerException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LobbyExceptionHandlerTest {
    private static final String VIEW_NAME = "anErrorView";
    private static final Object HANDLER = new TestHandler();
    private static final String REQUEST_URL = "http://a.request/url?key=value&key2=value";
    private static final String USER_AGENT = "anAgent";

    @Mock
    private CommonPropertiesHelper commonPropertiesHelper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ReloadableEnvironmentPropertiesInterceptor reloadableEnvironmentPropertiesInterceptor;
    @Mock
    private WebApiResponses webApiResponses;

    private ListAppender listAppender;
    private LobbyExceptionHandler underTest;

    @Before
    public void setUp() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LobbyExceptionHandler.class);
        logger.setLevel(Level.WARN);

        when(request.getRequestURL()).thenAnswer(new Answer<StringBuffer>() {
            @Override
            public StringBuffer answer(final InvocationOnMock invocation) throws Throwable {
                return new StringBuffer("http://a.request/url");
            }
        });
        when(request.getQueryString()).thenReturn("key=value&key2=value");
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);

        underTest = new LobbyExceptionHandler(commonPropertiesHelper, reloadableEnvironmentPropertiesInterceptor, VIEW_NAME, webApiResponses);
        listAppender = addAppenderTo(LobbyExceptionHandler.class);
    }

    @Test
    public void theErrorHandlerReturnsTheViewNameForARequestWithNoAcceptHeaders() {
        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat(modelAndView.getViewName(), is(equalTo(VIEW_NAME)));
    }

    @Test
    public void theErrorHandlerReturnsTheHtmlViewNameForARequestWithThatAcceptsBothHtmlAndJson() {
        when(request.getHeader("Accept")).thenReturn("text/html; q=0.8,application/json; q=0.5,*.*");

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat(modelAndView.getViewName(), is(equalTo(VIEW_NAME)));
    }

    @Test
    public void theErrorHandlerReturnsJsonForARequestWithThatAcceptsOnlyJson() throws IOException {
        final ServerException anException = new ServerException("anException");
        final Map<String, Object> expectedError = new HashMap<>();
        expectedError.put("handler", TestHandler.class.getName());
        expectedError.put("requestUrl", REQUEST_URL);
        expectedError.put("userAgent", USER_AGENT);
        expectedError.put("stackTrace", traceOf(anException));
        when(request.getHeader("Accept")).thenReturn("application/json; q=0.5,*.*");

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, anException);

        assertThat(modelAndView, is(nullValue()));
        verify(webApiResponses).write(response, expectedError);
    }

    @Test
    public void theErrorHandlerReturnsTheHtmlViewNameForARequestThatAcceptsAnythingElse() {
        when(request.getHeader("Accept")).thenReturn("text/plain");

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat(modelAndView.getViewName(), is(equalTo(VIEW_NAME)));
    }

    @Test
    public void theErrorHandlerSetsTheStatusForAnUnmappedException() {
        underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("RedundantCast")
    @Test
    public void theErrorHandlerLogsAnUnmappedException() {
        final ServerException anException = new ServerException("anException");

        underTest.resolveException(request, response, HANDLER, anException);

        // the cast is intentional, as generics are shite
        assertThat((List<String>) listAppender.getMessages(), hasItem("ERROR: Lobby catch-all triggered for handler "
                + TestHandler.class.getName() + "; Path: " + REQUEST_URL + "; User-Agent: anAgent; anException; <trace>"));
    }

    @Test
    public void theErrorHandlerAddsTheRequestUrlToTheModelWhenPresent() {
        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("requestUrl"), is(equalTo(REQUEST_URL)));
    }

    @Test
    public void theErrorHandlerAddsTheRequestUrlToTheModelWhenPresentWithNoQueryString() {
        reset(request);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://a.request/url"));
        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("requestUrl"), is(equalTo("http://a.request/url")));
    }

    @Test
    public void theErrorHandlerAddsTheUserAgentToTheModelWhenPresent() {
        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("userAgent"), is(equalTo(USER_AGENT)));
    }

    @Test
    public void theErrorHandlerAddsTheUserAgentOfNoneToTheModelWhenNoHeaderIsPresent() {
        reset(request);
        when(request.getRequestURL()).thenAnswer(new Answer<StringBuffer>() {
            @Override
            public StringBuffer answer(final InvocationOnMock invocation) throws Throwable {
                return new StringBuffer("http://a.request/url");
            }
        });

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("userAgent"), is(equalTo("<none>")));
    }

    @Test
    public void theErrorHandlerAddsTheHandlerToTheModelWhenPresent() {
        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("handler"), is(equalTo(TestHandler.class.getName())));
    }

    @Test
    public void theErrorHandlerAddANoneHandlerToTheModelWhenTheHandlerIsNull() {
        final ModelAndView modelAndView = underTest.resolveException(request, response, null, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("handler"), is(equalTo("<none>")));
    }

    @Test
    public void theErrorHandlerAddsTheDebugFlagIsLoggingIsSetToDebug() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LobbyExceptionHandler.class);
        logger.setLevel(Level.DEBUG);

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat((String) modelAndView.getModel().get("debug"), is(equalTo("true")));
    }

    @Test
    public void theErrorHandlerDoesNotAddTheDebugFlagIsLoggingIsSetToInfoOrAbove() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LobbyExceptionHandler.class);
        logger.setLevel(Level.INFO);

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, new ServerException("anException"));

        assertThat(modelAndView.getModel().get("debug"), is(nullValue()));
    }

    @Test
    public void theErrorHandlerReturnsTheStackTraceAsMessageInTheModel() {
        final ServerException anException = new ServerException("anException");

        final ModelAndView modelAndView = underTest.resolveException(request, response, HANDLER, anException);

        assertThat((String) modelAndView.getModel().get("message"), is(equalTo(traceOf(anException))));
    }

    @Test
    public void theErrorHandlerMapsAnExceptionMappedByRegexpCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new IllegalArgumentException("\"text; charset=utf-8\" does not contain '/'"),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "exception message: \"text; charset=utf-8\" does not contain '/'");
    }

    @Test
    public void theErrorHandlerMapsHttpRequestMethodNotSupportedExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new HttpRequestMethodNotSupportedException("GET"),
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                Level.WARN,
                "Request method 'GET' not supported; <trace>");
    }

    @Test
    public void theErrorHandlerMapsHttpMediaTypeNotSupportedExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new HttpMediaTypeNotSupportedException("aTest"),
                HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                Level.WARN,
                "aTest; <trace>");
    }

    @Test
    public void theErrorHandlerMapsMissingServletRequestParameterExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new MissingServletRequestParameterException("aParam", "aType"),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "Required aType parameter 'aParam' is not present; <trace>");
    }

    @Test
    public void theErrorHandlerMapsServletRequestBindingExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new ServletRequestBindingException("aMessage"),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "aMessage; <trace>");
    }

    @Test
    public void theErrorHandlerMapsTypeMismatchExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new TypeMismatchException("anObject", Integer.class),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; <trace>");
    }

    @Test
    public void theErrorHandlerMapsHttpMessageNotReadableExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new HttpMessageNotReadableException("aMessage"),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "aMessage; <trace>");
    }

    @Test
    public void theErrorHandlerMapsMethodArgumentNotValidExceptionCorrectly() throws NoSuchMethodException {
        assertThatExceptionReturnsStatusAndMessage(new MethodArgumentNotValidException(
                new MethodParameter(getClass().getMethod("setUp"), 0), new MapBindingResult(Collections.singletonMap("key", "value"), "anObject")),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "Validation failed for argument at index 0 in method: public void com.yazino.web.util.LobbyExceptionHandlerTest.setUp(), with 0 error(s): ; <trace>");
    }

    @Test
    public void theErrorHandlerMapsMissingServletRequestPartExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new MissingServletRequestPartException("aMessage"),
                HttpServletResponse.SC_BAD_REQUEST,
                Level.WARN,
                "Required request part 'aMessage' is not present.; <trace>");
    }

    @Test
    public void theErrorHandlerMapsConversionNotSupportedExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new ConversionNotSupportedException("aValue", Integer.class, new IllegalArgumentException("anException")),
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                Level.ERROR,
                "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; nested exception is java.lang.IllegalArgumentException: anException; <trace>");
    }

    @Test
    public void theErrorHandlerMapsHttpMessageNotWritableExceptionCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new HttpMessageNotWritableException("aMessage"),
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                Level.ERROR,
                "aMessage; <trace>");
    }

    @Test
    public void theErrorHandlerMapsJettyEofExceptionsCorrectly() {
        assertThatExceptionReturnsStatusAndMessage(new EofException("anEofException"),
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                Level.WARN,
                "exception message: anEofException");
    }

    @SuppressWarnings("RedundantCast")
    private void assertThatExceptionReturnsStatusAndMessage(final Exception e,
                                                            final int statusCode,
                                                            final Level level,
                                                            final String message) {
        underTest.resolveException(request, response, HANDLER, e);

        verify(response).setStatus(statusCode);
        // the cast is intentional, as generics are shite
        assertThat((List<String>) listAppender.getMessages(), hasItem(level + ": Lobby catch-all triggered for handler "
                + TestHandler.class.getName() + "; Path: " + REQUEST_URL + "; User-Agent: " + USER_AGENT + "; " + message));
    }

    private String traceOf(final ServerException anException) {
        final StringWriter exceptionTrace = new StringWriter();
        anException.printStackTrace(new PrintWriter(exceptionTrace));
        return exceptionTrace.toString();
    }


    private ListAppender addAppenderTo(final Class loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);
        final ListAppender<ILoggingEvent> logAppender = new ListAppender<ILoggingEvent>();
        logAppender.setContext(logger.getLoggerContext());
        logger.addAppender(logAppender);

        logAppender.start();

        return logAppender;
    }

    private static class TestHandler {

    }

    private class ListAppender<E> extends AppenderBase<E> {

        private final List<String> messages = new ArrayList<String>();

        @Override
        protected void append(final E eventObject) {
            if (eventObject instanceof ILoggingEvent) {
                final ILoggingEvent loggingEvent = (ILoggingEvent) eventObject;
                if (loggingEvent.getThrowableProxy() != null) {
                    messages.add(loggingEvent.getLevel() + ": " + loggingEvent.getFormattedMessage() + "; "
                            + loggingEvent.getThrowableProxy().getMessage() + "; <trace>");
                } else {
                    messages.add(loggingEvent.getLevel() + ": " + loggingEvent.getFormattedMessage());
                }
            } else {
                System.err.println("Cannot handle type: "
                        + eventObject.getClass().getName() + ": " + eventObject);
            }
        }

        public synchronized List<String> getMessages() {
            return Collections.unmodifiableList(messages);
        }

        public synchronized void clear() {
            messages.clear();
        }
    }

}
