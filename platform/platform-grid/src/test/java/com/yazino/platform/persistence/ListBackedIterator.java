package com.yazino.platform.persistence;

import com.gigaspaces.datasource.DataIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ListBackedIterator<T> implements DataIterator<T> {
    private final Iterator<T> backingIterator;
    private boolean closed = false;

    public ListBackedIterator(final T... objects) {
        final List<T> container = new ArrayList<T>(Arrays.asList(objects));
        backingIterator = container.iterator();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean hasNext() {
        return backingIterator.hasNext();
    }

    @Override
    public T next() {
        return backingIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isClosed() {
        return closed;
    }
}
