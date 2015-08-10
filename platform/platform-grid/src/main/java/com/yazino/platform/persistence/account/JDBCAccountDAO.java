package com.yazino.platform.persistence.account;

import com.yazino.platform.model.account.Account;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("accountDAO")
public class JDBCAccountDAO implements AccountDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCAccountDAO.class);

    private static final String SELECT_BY_ID = "SELECT * FROM ACCOUNT WHERE ACCOUNT_ID=?";
    private static final String INSERT_ACCOUNT
            = "INSERT INTO ACCOUNT (ACCOUNT_ID, NAME, BALANCE, CREDIT_LIMIT) values (?,?,?,?)";
    private static final String UPDATE_ACCOUNT = "update ACCOUNT set BALANCE= ? where ACCOUNT_ID=?";

    private final RowMapper<Account> accountRowMapper = new AccountRowMapper();

    private final JdbcOperations template;

    @Autowired
    public JDBCAccountDAO(@Qualifier("jdbcTemplate") final JdbcOperations template) {
        notNull(template, "template may not be null");

        this.template = template;
    }

    @Transactional
    public void saveAccount(final Account account) {
        notNull(account, "account may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering saveAccount " + ReflectionToStringBuilder.reflectionToString(account));
        }

        final long rowsUpdated = template.update(UPDATE_ACCOUNT,
                account.getBalance(), account.getAccountId());

        if (rowsUpdated == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Account " + account.getAccountId() + " does not exist: creating...");
            }

            template.update(INSERT_ACCOUNT, account.getAccountId(), account.getName(),
                    account.getBalance(), account.getCreditLimit());
        }
    }

    @Override
    public Account findById(final BigDecimal accountId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding by ID " + accountId);
        }

        final List<Account> accounts = template.query(SELECT_BY_ID, accountRowMapper, accountId);
        if (accounts != null && accounts.size() > 0) {
            return accounts.get(0);
        }

        return null;
    }

    static class AccountRowMapper implements RowMapper<Account> {
        public Account mapRow(final ResultSet rs,
                              final int rowNum) throws SQLException {
            final BigDecimal accountId = BigDecimals.strip(rs.getBigDecimal("ACCOUNT_ID"));
            final String name = rs.getString("NAME");
            final BigDecimal balance = rs.getBigDecimal("BALANCE");
            final BigDecimal creditLimit = rs.getBigDecimal("CREDIT_LIMIT");
            return new Account(accountId, name, balance, creditLimit);
        }
    }

}
