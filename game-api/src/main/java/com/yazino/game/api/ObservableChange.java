package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public final class ObservableChange implements Serializable {
    private static final long serialVersionUID = 2665999168927387814L;
    private long increment;
    private String[] args;

    public ObservableChange(final long increment,
                            final String[] args) {
        this.increment = increment;
        this.args = args;
    }

    public ObservableChange() {
    }

    public String[] getArgs() {
        return args;
    }

    public long getIncrement() {
        return increment;
    }

    public void setArgs(final String[] args) {
        this.args = args;
    }

    public void setIncrement(final long increment) {
        this.increment = increment;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ObservableChange rhs = (ObservableChange) obj;
        return new EqualsBuilder()
                .append(increment, rhs.increment)
                .append(args, rhs.args)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(increment)
                .append(args)
                .toHashCode();
    }

    @Override
    public String toString() {
        final List<String> argsAsString;
        if (args == null) {
            argsAsString = null;
        } else {
            argsAsString = Arrays.asList(args);
        }
        return "ObservableChange{"
                + "increment=" + increment
                + ", args=" + argsAsString
                + '}';
    }
}
