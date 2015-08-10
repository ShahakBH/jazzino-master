package strata.server.redirector;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

/**
 * Tests the {@link CanonicalRedirectorServlet} class.
 */
public class CanonicalRedirectorServletTest {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

    private final CanonicalRedirectorServlet servlet = new CanonicalRedirectorServlet();

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void http_host() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 80, "/", null);
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com/");
    }

    @Test
    public void http_host_port() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 8082, "/", null);
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com:8082/");
    }

    @Test
    public void http_host_port_path() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 8082, "/foo/bar", null);
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com:8082/foo/bar");
    }

    @Test
    public void http_host_port_query() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 8082, "/", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com:8082/?a=b");
    }

    @Test
    public void http_host_port_path_query() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 8082, "/foo/bar", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com:8082/foo/bar?a=b");
    }

    @Test
    public void http_host_query() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 80, "/", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com/?a=b");
    }

    @Test
    public void http_host_path() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 80, "/foo/bar", null);
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com/foo/bar");
    }

    @Test
    public void http_host_path_query() throws Exception {
        mockifyRequest(request, "http", "yazino.com", 80, "/foo/bar", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "http://www.yazino.com/foo/bar?a=b");
    }

    @Test
    public void https_host() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 443, "/", null);
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com/");
    }

    @Test
    public void https_host_port() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 8082, "/", null);
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com:8082/");
    }

    @Test
    public void https_host_port_path() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 8082, "/foo/bar", null);
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com:8082/foo/bar");
    }

    @Test
    public void https_host_port_query() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 8082, "/", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com:8082/?a=b");
    }

    @Test
    public void https_host_port_path_query() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 8082, "/foo/bar", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com:8082/foo/bar?a=b");
    }

    @Test
    public void https_host_query() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 443, "/", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com/?a=b");
    }

    @Test
    public void https_host_path() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 443, "/foo/bar", null);
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com/foo/bar");
    }

    @Test
    public void https_host_path_query() throws Exception {
        mockifyRequest(request, "https", "yazino.com", 443, "/foo/bar", "a=b");
        servlet.doGet(request, response);
        verifyResponse(response, "https://www.yazino.com/foo/bar?a=b");
    }


    private static void mockifyRequest(HttpServletRequest request, String scheme, String serverName, int port, String requestUri, String query) {
        when(request.getQueryString()).thenReturn(query);
        if ("https".equals(scheme) && port == 443 || "http".equals(scheme) && port == 80) {
            when(request.getRequestURL()).thenReturn(new StringBuffer(String.format("%s://%s%s", scheme, serverName, requestUri)));
        } else {
            when(request.getRequestURL()).thenReturn(new StringBuffer(String.format("%s://%s:%d%s", scheme, serverName, port, requestUri)));
        }
    }

    private static void verifyResponse(HttpServletResponse response, String location) {
        verify(response).setHeader("Location", location);
        verify(response).setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }

}
