package ch.qos.logback.classic.servlet;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link ServletContextListener} that can be used in web applications to define
 * the location of the logback configuration.
 * <p/>
 * <p>
 * Should be the first listener to configure logback before using it. Location
 * is defined in the <code>logbackConfigLocation</code> context param.
 * Placeholders (ex: ${user.home}) are supported. Location examples:<br />
 * /WEB-INF/log-sc.xml -> loaded from servlet context<br />
 * classpath:foo/log-cp.xml -> loaded from classpath<br />
 * file:/D:/log-absfile.xml -> loaded as url<br />
 * D:/log-absfile.xml -> loaded as absolute file<br />
 * log-relfile.xml -> loaded as file relative to the servlet container working
 * directory<br />
 * </p>
 *
 * @see "http://jira.qos.ch/browse/LBCLASSIC-202"
 */
public class LogbackConfigListener implements ServletContextListener {

    private static final String CONFIG_LOCATION_PARAM = "logbackConfigLocation";
    private static final String CONFIG_NAME_PARAM = "logbackApplicationName";
    private static final String LOCATION_PREFIX_CLASSPATH = "classpath:";
    private static final String CONTEXT_APPLICATION_NAME = "applicationName";
    private static final String GENERIC_APPLICATION_NAME = "catchall";

    public void contextInitialized(final ServletContextEvent sce) {
        final ServletContext servletContext = sce.getServletContext();

        final LoggerContext loggerContext = loggerContextFor(servletContext);
        if (loggerContext == null) {
            return;
        }

        final String location = locationFrom(servletContext, loggerContext);
        if (location == null) {
            servletContext.log("Can not configure logback. Location is null."
                    + " Maybe context param \"" + CONFIG_LOCATION_PARAM
                    + "\" is not set or is not correct.");
            return;
        }

        final URL url = toUrl(servletContext, location);
        if (url == null) {
            servletContext.log("Can not configure logback. Could not find logback"
                    + " config neither as servlet context-, nor as"
                    + " classpath-, nor as url-, nor as file system"
                    + " resource. Config location = \"" + location + "\".");
            return;
        }

        servletContext.log("Configuring logback. Config location = \"" + location + "\", full url = \"" + url + "\".");

        configure(servletContext, url, loggerContext);
    }

    private void setApplicationNameInContext(final ServletContext servletContext, final LoggerContext loggerContext) {
        final String applicationName = servletContext.getInitParameter(CONFIG_NAME_PARAM);
        if (applicationName != null) {
            loggerContext.putProperty(CONTEXT_APPLICATION_NAME, applicationName);
        } else {
            loggerContext.putProperty(CONTEXT_APPLICATION_NAME, GENERIC_APPLICATION_NAME);
        }
    }

    private LoggerContext loggerContextFor(final ServletContext servletContext) {
        final ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext)) {
            servletContext.log("Can not configure logback. " + LoggerFactory.class
                    + " is using " + loggerFactory + " which is not an instance of "
                    + LoggerContext.class);
            return null;
        }

        return (LoggerContext) loggerFactory;
    }

    private String locationFrom(final ServletContext servletContext,
                                final LoggerContext loggerContext) {
        String location = servletContext.getInitParameter(CONFIG_LOCATION_PARAM);

        if (location != null) {
            location = OptionHelper.substVars(location, loggerContext);
        }
        return location;
    }

    private void configure(final ServletContext servletContext,
                           final URL location,
                           final LoggerContext loggerContext) {
        if (location.getFile().endsWith(".groovy")) {
            servletContext.log("Failed to configure logback: Groovy configuration is not supported due to dependencies");

        } else {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            loggerContext.stop();
            setApplicationNameInContext(servletContext, loggerContext);
            try {
                configurator.doConfigure(location);
            } catch (JoranException e) {
                servletContext.log("Failed to configure logback.", e);
            }
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    private URL toUrl(final ServletContext servletContext,
                      final String location) {
        URL url = null;

        if (location.startsWith("/")) {
            url = parseLocationAsWebAppResource(servletContext, location);
        }

        if (url == null && location.startsWith(LOCATION_PREFIX_CLASSPATH)) {
            url = parseLocationAsClasspathResource(location);
        }

        if (url == null) {
            url = parseLocationAsURL(location);
        }

        if (url == null) {
            url = parseLocationAsFile(location);
        }

        if (url == null) {
            url = parseLocationAsWebAppRelativePath(servletContext, location);
        }

        return url;
    }

    private URL parseLocationAsClasspathResource(final String location) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(location.substring(LOCATION_PREFIX_CLASSPATH.length()));
    }

    private URL parseLocationAsWebAppResource(final ServletContext servletContext, final String location) {
        try {
            return servletContext.getResource(location);
        } catch (MalformedURLException e1) {
            // NO-OP
        }
        return null;
    }

    private URL parseLocationAsWebAppRelativePath(final ServletContext servletContext,
                                                  final String location) {
        try {
            final File file = new File(servletContext.getRealPath("/") + location);
            if (file.isFile()) {
                return file.toURI().normalize().toURL();
            }
        } catch (MalformedURLException e) {
            // NO-OP
        }
        return null;
    }

    private URL parseLocationAsFile(final String location) {
        File file = new File(location);
        if (!file.isAbsolute()) {
            file = file.getAbsoluteFile();
        }
        if (file.isFile()) {
            try {
                return file.toURI().normalize().toURL();
            } catch (MalformedURLException e) {
                // NO-OP
            }
        }
        return null;
    }

    private URL parseLocationAsURL(final String location) {
        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            // NO-OP
        }
        return null;
    }

    public void contextDestroyed(final ServletContextEvent sce) {
        final ILoggerFactory ilc = LoggerFactory.getILoggerFactory();

        if (!(ilc instanceof LoggerContext)) {
            return;
        }

        final LoggerContext lc = (LoggerContext) ilc;
        lc.stop();
    }
}
