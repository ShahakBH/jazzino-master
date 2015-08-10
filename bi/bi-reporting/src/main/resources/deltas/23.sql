CREATE VIEW LAST_PLAYED AS
      select player_id, game_type, max(audit_ts) as last_played
  from AUDIT_COMMAND ac join TABLE_INFO ti on ac.table_id = ti.table_id
  group by player_id, game_type;

GRANT SELECT ON LAST_PLAYED TO GROUP READ_ONLY;
GRANT ALL ON LAST_PLAYED TO GROUP READ_WRITE;
GRANT ALL ON LAST_PLAYED TO GROUP SCHEMA_MANAGER;
