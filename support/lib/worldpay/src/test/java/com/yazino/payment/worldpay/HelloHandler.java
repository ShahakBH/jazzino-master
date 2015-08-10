package com.yazino.payment.worldpay;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

public class HelloHandler extends AbstractHandler {

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        final Map<String,String[]> parameterMap = request.getParameterMap();
//        for (String param : parameterMap.keySet()) {
//            System.out.println(String.format("%s - %s", param, Arrays.toString(parameterMap.get(param))));
//        }
        String ott = parameterMap.get("OTT")[0];

//        Enumeration headerNames = request.getHeaderNames();
//        while(headerNames.hasMoreElements()) {
//            String headerName = (String)headerNames.nextElement();
//            System.out.println("<TR><TD>" + headerName);
//            System.out.println("    <TD>" + request.getHeader(headerName));
//        }
//        BufferedReader buff = request.getReader();
//        String sCurrentLine;
//        while ((sCurrentLine = buff.readLine()) != null) {
//            System.out.println(sCurrentLine);
//        }
        response.getWriter().println(ott);
    }
}
