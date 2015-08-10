package com.yazino.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.metrics.aggregation.MetricAggregator;
import com.yazino.metrics.parser.MetricParser;
import com.yazino.metrics.repository.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;
import static org.joda.time.DateTimeUtils.currentTimeMillis;

@Controller
@RequestMapping("/metrics")
public class MetricController {

    private final MetricParser parser;
    private final MetricRepository repository;
    private final MetricAggregator aggregator;
    private final ObjectMapper objectMapper;

    @Autowired
    public MetricController(final MetricParser parser,
                            final MetricRepository repository,
                            final MetricAggregator aggregator,
                            final ObjectMapper objectMapper) {
        notNull(parser, "parser may not be null");
        notNull(repository, "repository may not be null");
        notNull(aggregator, "aggregator may not be null");
        notNull(objectMapper, "objectMapper may not be null");

        this.parser = parser;
        this.repository = repository;
        this.aggregator = aggregator;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(method = RequestMethod.POST)
    public void post(final HttpServletRequest request,
                     final HttpServletResponse response)
            throws IOException {
        parser.parse(request.getInputStream());

        writeTo(response, null);
    }

    @RequestMapping(value = "/{name:.+}", method = RequestMethod.GET)
    public void getByName(final HttpServletResponse response,
                          @PathVariable("name") final String name)
            throws IOException {
        writeTo(response, aggregator.aggregate(currentTimeMillis(), repository.byName(name)));
    }

    @RequestMapping(value = "/{name:.+}/{source:.+}", method = RequestMethod.GET)
    public void getByNameAndSource(final HttpServletResponse response,
                                   @PathVariable("name") final String name,
                                   @PathVariable("source") final String source)
            throws IOException {
        writeTo(response, repository.byNameAndSource(name, source));
    }

    private void writeTo(final HttpServletResponse response,
                         final Object value)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (value == null) {
            response.getWriter().write("{}");
        } else {
            objectMapper.writeValue(response.getWriter(), value);
        }
    }

}
