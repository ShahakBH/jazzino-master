CREATE VIEW account_activity_view AS
    select distinct ac.player_id,
        date(audit_ts),game_type,p.ACCOUNT_ID,acs.platform
        from AUDIT_COMMAND ac join TABLE_DEFINITION td on ac.table_id = td.table_id
        JOIN GAME_VARIATION_TEMPLATE gvt on td.game_variation_template_id = gvt.game_variation_template_id
        LEFT JOIN PLAYER_DEFINITION p ON ac.PLAYER_ID = p.PLAYER_ID
        LEFT JOIN ACCOUNT_SESSION acs ON p.ACCOUNT_ID = acs.ACCOUNT_ID
        where command_type != 'Leave'
        and command_type != 'GetStatus';

GRANT SELECT ON account_activity_view TO GROUP READ_ONLY;
GRANT ALL ON account_activity_view TO GROUP READ_WRITE;
GRANT ALL ON account_activity_view TO GROUP SCHEMA_MANAGER;
