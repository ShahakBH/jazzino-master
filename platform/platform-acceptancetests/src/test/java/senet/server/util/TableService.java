package senet.server.util;

import com.yazino.platform.table.TableStatus;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.table.TableDAO;

import java.math.BigDecimal;
import java.util.Map;

public class TableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableService.class);
    private TableDAO tableDAO;
    private SequenceGenerator sequenceGenerator;

    // For the benefit of CGLIB and Spring as this class has no interface
    public TableService() {
    }

    @Autowired
    public TableService(@Qualifier("tableDAO") TableDAO tableDAO,
                        @Qualifier("sequenceGenerator") SequenceGenerator sequenceGenerator) {
        this.tableDAO = tableDAO;
        this.sequenceGenerator = sequenceGenerator;
    }

    public BigDecimal createTable(BigDecimal ownerAccountId, BigDecimal templateId, String gameType, Map<String, String> properties, String tableName) {
        Table table = new Table();
        table.setTableId(sequenceGenerator.next());
        table.setGameId(0L);
        table.setTemplateId(templateId);
        table.setTableName(tableName);
        table.setGameTypeId(gameType);
        table.setTableStatus(TableStatus.open);
        table.setVariationProperties(properties);

        tableDAO.save(table);
        return table.getTableId();
    }

    public Table getTable(BigDecimal id) {
        LOG.debug("entering getTable " + id);
        return tableDAO.findById(id);
    }

    @Transactional
    public void updateTable(Table t) {
        LOG.info("updating table " + ToStringBuilder.reflectionToString(t));
        tableDAO.save(t);
    }

}
