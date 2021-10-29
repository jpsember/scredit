#!/usr/bin/env bash
set -e

APP=scredit
MVN=$HOME/.m2/repository
DIR=$(pwd)

###### Custom statements start ##### {~custom1:
###### Custom statements end   ##### ~}

if [ -f "target/classes/js/scredit/ScrEdit.class" ]; then
  APPLOC="target/classes"
else
  APPLOC="$MVN/com/jsbase/$APP/1.0/$APP-1.0.jar"
fi

java -Dfile.encoding=UTF-8 -classpath $APPLOC:$MVN/commons-io/commons-io/2.6/commons-io-2.6.jar\
:$MVN/com/jsbase/base/1.0/base-1.0.jar\
:$MVN/com/jsbase/graphics/1.0/graphics-1.0.jar\
 js.scredit/ScrEdit "$@" &
