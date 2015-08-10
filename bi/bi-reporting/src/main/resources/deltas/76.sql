CREATE TABLE client_log (
   log_ts timestamp without time zone not null distkey sortkey,
   payload character varying(max) not null,
   game_type character varying(32),
   version  character varying(32),
   platform character varying(32),
   message character varying(255),
   error_code integer,
   stacktrace character varying(max),
   player_id decimal(16,2),
   table_id decimal(16, 2)
);

GRANT SELECT ON client_log TO GROUP READ_ONLY;
GRANT ALL ON client_log TO GROUP READ_WRITE;
GRANT ALL ON client_log TO GROUP SCHEMA_MANAGER;