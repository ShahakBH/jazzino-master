package com.yazino;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.servlet.DispatcherServlet;

public final class StandaloneServer {

    private StandaloneServer() {

    }

    public static void main(final String[] arguments) throws Exception {
        final Server server = new Server(8080);
        final ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        final DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setContextConfigLocation("classpath:standalone-server-springmvc.xml");
        servletHandler.addServletWithMapping(new ServletHolder(dispatcherServlet), "/*");
        server.start();
        server.join();
    }
}
