alter TABLE aggregator_lock
add column locked_ts TIMESTAMP default SYSDATE;
