package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableTransactionRequest;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.junit.Assert.assertEquals;

public class TableTransactionTest {
    @Test
    public void testSerializationRoundTrip() throws IOException, ClassNotFoundException {
        BigDecimal tableId = BigDecimal.valueOf(20);
        PostTransactionAtTable transferFundsAtTable = new PostTransactionAtTable(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                "transactionType", "auditLabel", "uniqueId", transactionContext().withGameId(30L).withTableId(tableId).withSessionId(BigDecimal.ONE).build());
        TableTransactionRequest underTest = new TableTransactionRequest(tableId, Arrays.asList(transferFundsAtTable), Collections.<BigDecimal>emptySet(), "auditLabel");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(underTest);
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        @SuppressWarnings({"unchecked"}) TableTransactionRequest after = (TableTransactionRequest) oin.readObject();
        assertEquals(underTest, after);
    }
}
