package com.yazino.bi.operations.model;

import java.text.Format;

public class CellModel {

    private Object value;
    private Format formatter;


    public CellModel(final Object value,
                     final Format formatter) {
        this.value = value;
        this.formatter = formatter;
    }

    public String getFormattedValue() {
        if (this.value == null) {
            return "";
        }
        return this.formatter.format(this.value);
    }
}
