-- WEB-4734 - correct fubar avatar URLs

UPDATE lobby_user SET PICTURE_LOCATION = '%CONTENT%/images/gloss/friend-bar-none-photo.png' WHERE PICTURE_LOCATION = 'default';
