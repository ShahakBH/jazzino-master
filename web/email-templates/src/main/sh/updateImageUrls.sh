#!/bin/bash

ROOT_DIR=$1

sed -i \.pre-content-upload -e s/\\\"assets\\//http:\\/\\/cdn.yazino.com\\/email-content\\/transactional\\/20120918T150000\\/day0\\/$ROOT_DIR\\\/Yazino\\/assets/g $ROOT_DIR/Yazino/index.html
sed -i \.pre-content-upload -e s/\\\"assets\\//http:\\/\\/cdn.yazino.com\\/email-content\\/transactional\\/20120918T150000\\/day0\\/$ROOT_DIR\\\/Facebook\\/assets/g $ROOT_DIR/Facebook/index.html

