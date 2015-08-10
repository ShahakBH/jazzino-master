package com.yazino.platform.audit;


import com.yazino.platform.audit.message.*;

import java.util.List;

public interface AuditService {

    void transactionsProcessed(List<Transaction> transactions);

    void auditCommands(List<CommandAudit> commands);

    void auditGame(GameAudit gameAudit);

    void externalTransactionProcessed(ExternalTransaction externalTransaction);

    void auditSessionKey(SessionKey sessionKey);

}
