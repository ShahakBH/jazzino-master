package com.yazino.host.table;

import com.yazino.platform.model.community.TableInvite;
import org.springframework.stereotype.Component;
import com.yazino.platform.repository.community.TableInviteRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class StandaloneTableInviteRepository implements TableInviteRepository {
    @Override
    public List<TableInvite> findInvitationsByPlayerId(final BigDecimal bigDecimal) {
        return Collections.emptyList();
    }

    @Override
    public List<TableInvite> removeInvitationsByTableId(final BigDecimal bigDecimal) {
        return Collections.emptyList();
    }

    @Override
    public void invitePlayerToTable(final BigDecimal bigDecimal, final BigDecimal bigDecimal1) {
    }

    @Override
    public TableInvite findByTableAndPlayerId(final BigDecimal bigDecimal, final BigDecimal bigDecimal1) {
        return null;
    }
}
