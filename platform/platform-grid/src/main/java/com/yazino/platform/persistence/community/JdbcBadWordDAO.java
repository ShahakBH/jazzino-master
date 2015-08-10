package com.yazino.platform.persistence.community;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JdbcBadWordDAO implements BadWordDAO {
    private final JdbcTemplate template;

    @Autowired
    public JdbcBadWordDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.template = jdbcTemplate;
    }

    @Override
    public Set<String> findAllBadWords() {
        return new HashSet<String>(template.queryForList(
                "select WORD from BAD_WORD where FIND_PART_WORD = 0", String.class));
    }

    @Override
    public Set<String> findAllPartBadWords() {
        return new HashSet<String>(template.queryForList(
                "select WORD from BAD_WORD where FIND_PART_WORD = 1", String.class));
    }
}
