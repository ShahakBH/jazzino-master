package com.yazino.platform.model;

import com.google.common.base.Function;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class PagedData<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = -2642905963911187922L;

    private final int startPosition;
    private final int size;
    private final int totalSize;
    private final List<T> data = new ArrayList<T>();

    public PagedData(final int startPosition,
                     final int size,
                     final int totalSize,
                     final List<T> data) {
        this.startPosition = startPosition;
        this.size = size;
        this.totalSize = totalSize;

        if (data != null) {
            this.data.addAll(data);
        }
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getSize() {
        return size;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public List<T> getData() {
        return data;
    }

    public boolean isForwardAllowed() {
        return totalSize > 0 && (startPosition + size) < totalSize;
    }

    public boolean isBackwardAllowed() {
        return startPosition > 0 && totalSize > 0;
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    public static <T> PagedData<T> empty() {
        return new PagedData<T>(0, 0, 0, Collections.<T>emptyList());
    }

    public <V> PagedData<V> transform(final Function<T, V> transformer) {
        notNull(transformer, "transformer may not be null");

        final List<V> transformedList = new ArrayList<V>();

        if (getData() != null) {
            for (T item : getData()) {
                transformedList.add(transformer.apply(item));
            }
        }

        return new PagedData<V>(getStartPosition(), getSize(), getTotalSize(), transformedList);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PagedData rhs = (PagedData) obj;
        return new EqualsBuilder()
                .append(startPosition, rhs.startPosition)
                .append(size, rhs.size)
                .append(totalSize, rhs.totalSize)
                .append(data, rhs.data)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 53)
                .append(startPosition)
                .append(size)
                .append(totalSize)
                .append(data)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
