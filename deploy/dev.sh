#!/bin/bash
# Install gnul tar: brew install gnu-tar - Require for Mac OS

SERVER_RELEASE=171.244.148.242
TARGET_FOLDER=/opt/deploy/lf/video-service
APP_ID=lf-meida-service-demo

# Cleanup old version
rm -rf release
rm -rf api.tar.gz
mkdir release

echo "Build source..."
cd ../source
mvn clean package

cd ../deploy
echo "Update config"
cp -r ../source/socket/target/lib release/
cp ../source/socket/target/lf.video-1.0-SNAPSHOT.jar release/lib/
mkdir release/classes
cp cfg/configuration-dev.properties release/classes/configuration.properties
cp cfg/log4j2.xml release/classes/log4j2.xml


cp service-socket.service release/$APP_ID.service 
sed -i '' "s/{APP_PATH}/$(printf '%s\n' "$TARGET_FOLDER" | sed -e 's/[]\/$*.^[]/\\&/g')/g" release/$APP_ID.service

cp run.sh release/run.sh
sed -i '' "s/{TARGET_DIR}/$(printf '%s\n' "$TARGET_FOLDER" | sed -e 's/[]\/$*.^[]/\\&/g')/g" release/run.sh

echo "Compress source..."
gtar -czf api.tar.gz release

echo "Deploy to server... $SERVER_RELEASE"
ssh root@$SERVER_RELEASE "mkdir -p $TARGET_FOLDER"
ssh root@$SERVER_RELEASE "rm -rf $TARGET_FOLDER/*"


echo " ---> Stop and Remove old service"
ssh root@$SERVER_RELEASE "systemctl stop $APP_ID.service"
ssh root@$SERVER_RELEASE "rm -rf /lib/systemd/system/$APP_ID.service"


echo " ---> Upload build..."
scp api.tar.gz root@$SERVER_RELEASE:$TARGET_FOLDER/api.tar.gz
ssh root@$SERVER_RELEASE "cd $TARGET_FOLDER && tar -xzf api.tar.gz && rm -rf api.tar.gz && mv release/* . && rm -rf release"
ssh root@$SERVER_RELEASE "chmod +x $TARGET_FOLDER/run.sh"

# Deploy service
ssh root@$SERVER_RELEASE "mv $TARGET_FOLDER/$APP_ID.service  /lib/systemd/system/$APP_ID.service"
ssh root@$SERVER_RELEASE "chmod 644 /lib/systemd/system/$APP_ID.service && systemctl daemon-reload"
ssh root@$SERVER_RELEASE "systemctl enable $APP_ID.service"
ssh root@$SERVER_RELEASE "systemctl start $APP_ID.service"


echo "Cleanup..."
rm -rf release
rm -rf api.tar.gz

echo '==========================================================================================='
echo "                                       All Done"
echo '==========================================================================================='

