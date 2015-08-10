package com.yazino.web.util;

public class OpenGraphObjectId {

    private String prefix;
    private String middle;
    private Integer suffix;

    public OpenGraphObjectId(final String prefix, final String middle) {
        this.prefix = prefix;
        this.middle = middle;
    }

    public OpenGraphObjectId(final String prefix, final String middle, final Integer suffix) {
        this.prefix = prefix;
        this.middle = middle;
        this.suffix = suffix;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getMiddle() {
        return this.middle;
    }

    public Integer getSuffix() {
        return this.suffix;
    }


}
