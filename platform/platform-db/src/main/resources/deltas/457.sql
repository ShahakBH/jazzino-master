-- WEB-4217 - add ability to mark game configurations as disabled.

ALTER TABLE GAME_CONFIGURATION ADD COLUMN ENABLED BIT(1) DEFAULT 1#
