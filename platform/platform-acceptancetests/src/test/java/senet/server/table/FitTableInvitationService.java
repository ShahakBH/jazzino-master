package senet.server.table;

import org.openspaces.remoting.Routing;
import com.yazino.game.api.GameType;
import com.yazino.platform.community.TableInviteService;
import com.yazino.platform.community.TableInviteSummary;

import java.math.BigDecimal;
import java.util.List;

public class FitTableInvitationService implements TableInviteService {

    @Override
    public List<BigDecimal> findTableInvitesByPlayerId(BigDecimal playerId) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void tableClosed(BigDecimal arg0) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void invitePlayerToTable(@Routing final BigDecimal playerId,
                                    final String playerName,
                                    final BigDecimal tableId,
                                    final GameType gameType) {
    }

    @Override
    public void sendInvitations(final BigDecimal playerId, final List<TableInviteSummary> allInvites) {
    }
}
