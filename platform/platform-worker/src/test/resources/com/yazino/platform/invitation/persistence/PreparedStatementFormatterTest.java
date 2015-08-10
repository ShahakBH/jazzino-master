package com.yazino.platform.invitation.persistence;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class PreparedStatementFormatterTest {

    private PreparedStatementFormatter underTest;

    @Before
    public void setUp() {
        underTest = new PreparedStatementFormatter();
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void formatThrowsANullPointerExceptionForANullArgument() {
        underTest.format(null);
    }

    @Test
    public void aSqlStatementWithNoArgumentsIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable");

        assertThat(output, is(equalTo("SELECT * FROM aTable")));
    }

    @Test
    public void aSqlStatementWithAStringArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE name=?", "bob");

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE name='bob'")));
    }

    @Test
    public void aSqlStatementWithACharacterArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE name=?", 'c');

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE name='c'")));
    }

    @Test
    public void aSqlStatementWithAStringBuilderArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE name=?", new StringBuilder("fred"));

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE name='fred'")));
    }

    @Test
    public void aSqlStatementWithADateArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE ts=?", new Date(10000));

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE ts=timestamp('1970-01-01 01:00:10')")));
    }

    @Test
    public void aSqlStatementWithADateTimeArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE ts=?", new DateTime(2010, 12, 30, 15, 34, 23, 10));

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE ts=timestamp('2010-12-30 15:34:23')")));
    }

    @Test
    public void aSqlStatementWithABigDecimalArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE id=?", BigDecimal.valueOf(20));

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE id=20")));
    }

    @Test
    public void aSqlStatementWithALongArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE value=?", 34L);

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE value=34")));
    }

    @Test
    public void aSqlStatementWithADoubleArgumentIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE amount=?", 74.55D);

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE amount=74.55")));
    }

    @Test
    public void aSqlStatementWithAStringArgumentRequiringEscapingIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE name=?", "o'connor");

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE name='o''connor'")));
    }

    @Test
    public void aSqlStatementWithMultipleArgumentsIsFormattedCorrectly() {
        final String output = underTest.format("SELECT * FROM aTable WHERE first_name=? and last_name=? and age=?",
                "bob", "o'connor", 17);

        assertThat(output, is(equalTo("SELECT * FROM aTable WHERE first_name='bob' "
                + "and last_name='o''connor' and age=17")));
    }

    @Test
    public void anInsertSqlStatementWithMultipleArgumentsIsFormattedCorrectly() {
        final String output = underTest.format("INSERT INTO aTable VALUES (?,?,?)", "bob", "o'connor", 17);

        assertThat(output, is(equalTo("INSERT INTO aTable VALUES ('bob','o''connor',17)")));
    }

}
