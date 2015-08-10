package com.yazino.bi.operations.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

public class RequestDefinition {
    private String query;
    private String header;
    private List<Map<String, Object>> result;
    private List<String> unionQueries = new ArrayList<String>();

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("query", query).append("header", header)
                .append("result", result).append("unionQueries", unionQueries).toString();
    }

    public void setUnionQueries(final List<String> unionQueries) {
        this.unionQueries = unionQueries;
    }

    public List<String> getUnionQueries() {
        return unionQueries;
    }

    public List<Map<String, Object>> getResult() {
        return result;
    }

    public void setResult(final List<Map<String, Object>> result) {
        this.result = result;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(final String header) {
        this.header = header;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }
}
