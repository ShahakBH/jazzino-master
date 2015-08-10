package com.yazino.platform.model.table;


import java.math.BigDecimal;

public interface TableRequest {

    BigDecimal getTableId();


    TableRequestType getRequestType();

}
