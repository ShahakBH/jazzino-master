-- WEB-4734 - correct fubar avatar URLs

UPDATE PLAYER SET PICTURE_LOCATION = '%CONTENT%/images/gloss/friend-bar-none-photo.png' WHERE PICTURE_LOCATION = 'default'#
