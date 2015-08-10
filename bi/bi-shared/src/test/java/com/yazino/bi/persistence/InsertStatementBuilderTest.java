package com.yazino.bi.persistence;

import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class InsertStatementBuilderTest {

    @Test(expected = NullPointerException.class)
    public void anInsertStatementCannotBeCreatedWithNoTable() {
        new InsertStatementBuilder(null, "aField");
    }

    @Test(expected = IllegalArgumentException.class)
    public void anInsertStatementCannotBeCreatedWithNoFields() {
        new InsertStatementBuilder("aTable");
    }

    @Test(expected = IllegalStateException.class)
    public void anInsertStatementCannotBeCreatedWithNoValues() {
        new InsertStatementBuilder("aTable", "aField").toSql();
    }

    @Test
    public void anInsertStatementIsGeneratedOnMySQL() {
        final String sql = new InsertStatementBuilder("aTable", "aField", "anotherField")
                .withValues("value1", "value2")
                .withValues("value3", "value4")
                .withValues("value5", "value6")
                .toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField,anotherField) VALUES (value1,value2),(value3,value4),(value5,value6)")));
    }

    @Test
    public void valuesAreWrittenToTheStatementVerbatim() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues("'value''1'").toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('value''1')")));
    }

    @Test
    public void bigDecimalsAreWrittenToTheStatementInAPlainForm() {
        final BigDecimal bigDecimal = new BigDecimal("1.4353453465345435435435");
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlBigDecimal(bigDecimal)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (1.4353453465345435435435)")));
    }

    @Test
    public void nullBigDecimalsAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlBigDecimal(null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void timestampsAreWrittenToTheStatementInThePostgresqlTimestampWithoutTimezoneFormat() {
        final Timestamp timestamp = new Timestamp(new DateTime(2012, 2, 13, 12, 15, 30, 503).getMillis());
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlTimestamp(timestamp)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('2012-02-13 12:15:30.503')")));
    }

    @Test
    public void nullTimestampsAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlTimestamp((Date) null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void dateTimesAreWrittenToTheStatementInThePostgresqlTimestampWithoutTimezoneFormat() {
        final DateTime dateTime = new DateTime(2012, 2, 13, 12, 15, 30, 503);
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlTimestamp(dateTime)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('2012-02-13 12:15:30.503')")));
    }

    @Test
    public void nullDateTimesAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlTimestamp((DateTime) null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void nullCharsInStringsAreReplacedWithEmptyString() {
        final String stringValue = "\u0000TP/1.1";
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlString(stringValue)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('TP/1.1')")));
    }

    @Test
    public void stringsAreWrittenToTheStatementInQuotes() {
        final String stringValue = "Now is the winter";
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlString(stringValue)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('Now is the winter')")));
    }

    @Test
    public void stringsWithQuotesAreWrittenToTheStatementWithEscapedQuotes() {
        final String stringValue = "It's its thing";
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlString(stringValue)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (E'It\\'s its thing')")));
    }

    @Test
    public void stringsWithBacklashesAreWrittenToTheStatementWithEscapedBackslashes() {
        final String stringValue = "\\A farm\\in the\\\\ country\\";
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlString(stringValue)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (E'\\\\A farm\\\\in the\\\\\\\\ country\\\\')")));
    }

    @Test
    public void nullStringAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlString(null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void longAreWrittenToTheStatementInAPlainForm() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlLong(34234L)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (34234)")));
    }

    @Test
    public void nullLongsAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlLong(null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void intAreWrittenToTheStatementInAPlainForm() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlInt(34234)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (34234)")));
    }

    @Test
    public void nullIntegersAreWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlInt(null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

    @Test
    public void dateIsWrittenToTheStatementInAPlainForm() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlDate(new Date(123456))).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES ('1970-01-01')")));
    }

    @Test
    public void nullDateIsWrittenToTheStatementAsNull() {
        final String sql = new InsertStatementBuilder("aTable", "aField").withValues(sqlDate(null)).toSql();

        assertThat(sql, is(equalTo("INSERT INTO aTable (aField) VALUES (NULL)")));
    }

}
