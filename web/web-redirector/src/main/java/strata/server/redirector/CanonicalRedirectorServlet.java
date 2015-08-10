package strata.server.redirector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CanonicalRedirectorServlet extends HttpServlet {
    private static final long serialVersionUID = 3007109317818061843L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CanonicalRedirectorServlet.class);

    private static final String LOCATION = "Location";

    public void doGet(final HttpServletRequest request,
                      final HttpServletResponse response) throws ServletException, IOException {
        final String source;
        if (request.getQueryString() == null) {
            source = request.getRequestURL().append("").toString();
        } else {
            source = request.getRequestURL().append("?").append(request.getQueryString()).toString();
        }
        final String target = source.replaceFirst("http://", "http://www.").replaceFirst("https://", "https://www.");
        LOG.info("Redirecting {} to {}", source, target);
        response.setHeader(LOCATION, target);
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
}
