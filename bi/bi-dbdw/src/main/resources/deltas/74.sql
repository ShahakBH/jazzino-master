ALTER TABLE rpt_users_daily 
ADD COLUMN platform VARCHAR(64) NULL DEFAULT ''#

ALTER TABLE rpt_users_monthly 
ADD COLUMN platform VARCHAR(64) NULL DEFAULT ''#

ALTER TABLE rpt_users_weekly 
ADD COLUMN platform VARCHAR(64) NULL DEFAULT ''#