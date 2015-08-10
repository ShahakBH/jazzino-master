--  WEB-4261 - fix indexing on rpt_recent_registrations

-- manually applied to production at 1030 15/8/13 by jshiell.

ALTER TABLE rpt_recent_registrations DROP PRIMARY KEY#

-- MySQL bug #60886 - we can't ALTER IGNORE on a NOT NULL column
ALTER TABLE rpt_recent_registrations MODIFY PLAYER_ID BIGINT(11) NULL#

ALTER IGNORE TABLE rpt_recent_registrations ADD PRIMARY KEY (PLAYER_ID)#

ALTER TABLE rpt_recent_registrations MODIFY PLAYER_ID BIGINT(11) NOT NULL#
