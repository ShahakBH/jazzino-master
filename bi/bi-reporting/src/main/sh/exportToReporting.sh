#!/bin/sh
MYSQL_USER=root


echo exporting MPDR
mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataproddw MESSAGING_PLAYER_DEVICE_REGISTRATION
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi
echo exporting seg_sel
mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataproddw SEGMENT_SELECTION
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi
echo exporting gcm
mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataproddw GCM_PLAYER_DEVICE
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi
echo exporting face_excl

mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataproddw FACEBOOK_EXCLUSIONS
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi

echo exporting play_promo_st
mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataprod PLAYER_PROMOTION_STATUS
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi
echo exporting ios_P_D
mysqldump -u"$MYSQL_USER" --skip-opt --default-character-set=utf8 --fields-terminated-by=$'\t' -T/var/tmp strataprod IOS_PLAYER_DEVICE
if [ $? -ne 0 ]; then
    echo "mysqldump failed with error $?"
    exit 1
fi


