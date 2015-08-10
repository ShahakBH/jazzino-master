package com.yazino.web.domain;

import com.yazino.web.util.JsonHelper;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TournamentDataFormatter {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
    private final JsonHelper jsonHelper = new JsonHelper();


    public String formatDate(final long milliSec) {
        return dateFormat.format(milliSec);

    }

    public String formatNumber(final BigInteger number) {
        return numberFormat.format(number);
    }

    public String formatTime(final long milliSec) {
        return timeFormat.format(milliSec).toLowerCase();
    }

    public String formatAsJson(final Object o) {
        if (o != null) {
            return jsonHelper.serialize(o);
        }
        return "[]";
    }
}
