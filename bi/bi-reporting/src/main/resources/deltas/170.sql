alter table segment_selection add column valid_from timestamp null;
CREATE INDEX seg_select_vaid_from_ts_idx ON segment_selection USING btree (valid_from);

create or replace view LAST_LOGIN as
 SELECT pd.player_id,
    max(acc.start_ts) AS last_login
   FROM account_session acc
   JOIN player_definition pd ON acc.account_id = pd.account_id
  GROUP BY pd.player_id;

grant select on LAST_LOGIN to group read_only;
grant all on LAST_LOGIN to group read_write;
grant all on LAST_LOGIN to group schema_manager;
