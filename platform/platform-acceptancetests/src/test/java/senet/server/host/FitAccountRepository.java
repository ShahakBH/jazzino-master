package senet.server.host;

import com.yazino.platform.model.account.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import com.yazino.platform.persistence.account.AccountLoadType;
import com.yazino.platform.repository.account.AccountRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FitAccountRepository implements AccountRepository {

    private JdbcTemplate template;

    private final AccountRowMapper accountRowMapper = new AccountRowMapper();

    @Autowired(required = true)
    public void setTemplate(@Qualifier("jdbcTemplate") final JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public Account findById(final BigDecimal accountId) {
        final List<?> objects = template.query("SELECT * FROM ACCOUNT WHERE ACCOUNT_ID=?",
                new Object[]{accountId}, accountRowMapper);
        if (objects != null && objects.size() > 0) {
            return (Account) objects.get(0);
        }
        return null;
    }

    @Override
    public Account findById(final BigDecimal accountId,
                            final AccountLoadType loadType) {
        return findById(accountId);
    }

    @Override
    public void save(final Account account) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove(final Account account) {
        template.execute("DELETE FROM ACCOUNT WHERE ACCOUNT_ID=?", new PreparedStatementCallback() {
            @Override
            public Object doInPreparedStatement(final PreparedStatement ps) throws SQLException, DataAccessException {
                ps.setBigDecimal(1, account.getAccountId());
                return ps;
            }
        });
    }

    @Override
    public Account lock(final BigDecimal accountId) {
        return findById(accountId);
    }

    private class AccountRowMapper implements RowMapper {
        @Override
        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Account account = new Account();

            account.setAccountId(rs.getBigDecimal("ACCOUNT_ID"));
            account.setName(rs.getString("NAME"));
            account.setBalance(rs.getBigDecimal("BALANCE"));
            account.setCreditLimit(rs.getBigDecimal("CREDIT_LIMIT"));
            account.setOpen(rs.getBoolean("OPEN"));

            return account;
        }
    }
}
