-- WEB-4579 - Clean up obsolete tables/view only present in prod

# DELIMITER #

DROP VIEW IF EXISTS rpt_registrations_per_source_and_month#
DROP VIEW IF EXISTS rpt_registrations_per_source_and_week#
DROP VIEW IF EXISTS mm_accepted_invites#
DROP VIEW IF EXISTS facebook_notifications_1#
DROP VIEW IF EXISTS facebook_notifications_2#
DROP VIEW IF EXISTS facebook_requests#
DROP VIEW IF EXISTS android_push#

DROP TABLE IF EXISTS bi_verification_status_copy#
