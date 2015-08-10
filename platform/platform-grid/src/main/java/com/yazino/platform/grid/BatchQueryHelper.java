package com.yazino.platform.grid;

import com.gigaspaces.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class BatchQueryHelper<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchQueryHelper.class);

    private static final int BATCH_SIZE = 500;

    private final GigaSpace space;
    private final String idFieldName;

    private String additionalCriteria;

    public BatchQueryHelper(final GigaSpace space, final String idFieldName) {
        notNull(space, "Space is required");
        notNull(idFieldName, "ID field name is required");

        this.space = space;
        this.idFieldName = idFieldName;
    }

    public BatchQueryHelper(final GigaSpace space, final String idFieldName, final String additionalCriteria) {
        this(space, idFieldName);
        notNull(additionalCriteria, "Additional criteria is required");

        this.additionalCriteria = additionalCriteria;
    }

    public Set<T> findByIds(final Class<T> clazz, final Collection<Object> ids) {
        notNull(clazz, "Class is required");

        LOG.debug("Get by IDs: {}", ids);

        if (ids == null || ids.size() == 0) {
            return Collections.emptySet();
        }

        final List<Object> idList = new ArrayList<>(ids);
        final Set<T> results = new HashSet<>();
        int fromIndex = 0;
        while (fromIndex < idList.size()) {
            int toIndex = fromIndex + BATCH_SIZE;
            if (toIndex > idList.size()) {
                toIndex = idList.size();
            }
            results.addAll(findByIdsBatch(clazz, idList.subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }

        LOG.debug("Find by IDs for {} is {}", ids, results);

        return results;
    }

    /*
     * Yes OR is ugly but check bug: http://forum.openspaces.org/message.jspa?messageID=11246
     */
    private String constructSQLOrString(final int numberOfIds) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (additionalCriteria != null) {
            stringBuilder.append(additionalCriteria).append(" AND (");
        }
        for (int count = 0; count < numberOfIds - 1; ++count) {
            stringBuilder.append(idFieldName).append(" = ? OR ");
        }
        stringBuilder.append(idFieldName).append(" = ?");
        if (additionalCriteria != null) {
            stringBuilder.append(")");
        }
        return stringBuilder.toString();
    }

    private Collection<T> findByIdsBatch(final Class<T> clazz, final Collection<Object> ids) {
        LOG.debug("Find by IDs for Batch: {}", ids);

        if (ids.size() > BATCH_SIZE) {
            throw new UnsupportedOperationException("Cannot process batch size " + ids.size());
        }

        final String query = constructSQLOrString(ids.size());
        final SQLQuery<T> sqlQuery = new SQLQuery<>(clazz, query,
                ids.toArray(new Object[ids.size()]));

        final T[] matches = space.readMultiple(sqlQuery, BATCH_SIZE, ReadModifiers.DIRTY_READ);
        if (matches == null) {
            LOG.debug("Querying elements by IDs: \"{}\" with parameters: \"{}\" returned null results", query, ids);
            return Collections.emptySet();
        }

        LOG.debug("Querying elements by IDs: \"{}\" with parameters: \"{}\" found {} results", query, ids, matches.length);

        return Arrays.asList(matches);
    }
}
