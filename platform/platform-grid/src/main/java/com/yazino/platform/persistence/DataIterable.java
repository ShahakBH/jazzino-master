package com.yazino.platform.persistence;

import com.gigaspaces.datasource.DataIterator;

/**
 * Allows the retrieval of stored data via an iterator.
 *
 * @param <T> the type to retrieve.
 */
public interface DataIterable<T> {

    /**
     * Fetch the data for the given partition via an iterator.
     *
     * @return an iterator for the matching data.
     */
    DataIterator<T> iterateAll();

}
