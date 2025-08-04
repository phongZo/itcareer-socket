#!/bin/bash
RUN_DIR=${PWD}
MYAPP_CLASSPATH=$RUN_DIR/classes
PATH_LIBS=$RUN_DIR/lib/*.jar
LIBS=""
for file in $PATH_LIBS
do
  LIBS=$file:$LIBS 
done
export JAVA_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -server -XX:+DisableExplicitGC"
MYAPP_CLASSPATH=$MYAPP_CLASSPATH:$LIBS
java -Xms1024m -Xmx2048m -cp $MYAPP_CLASSPATH  lf.video.Server
