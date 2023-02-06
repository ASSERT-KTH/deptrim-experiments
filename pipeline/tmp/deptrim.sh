#!/bin/bash

java -version


REPO_NAME=$1
GITHUB_URL=$2
MODULE_DIR=$3
RELEASE=$4
COMMIT=$5
CURRENT_DIR=$(pwd)

# ==============================================================================
cd "$CURRENT_DIR"
git clone $GITHUB_URL
cd "$CURRENT_DIR"/$REPO_NAME
git checkout $COMMIT
cd "$CURRENT_DIR"/$REPO_NAME/$MODULE_DIR
mvn clean package -DskipTests -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dlicense.skip=true -Dmaven.clean.skip=true
cd "$CURRENT_DIR"/$REPO_NAME/$MODULE_DIR
mkdir deptrim
echo "running deptrim"
mvn -B se.kth.castor:deptrim-maven-plugin:0.1.2:deptrim -DcreateSinglePomSpecialized=true -DverboseMode=true -DignoreScopes=test,provided,system,import,runtime >>deptrim/deptrim.log
echo "deptrim done"
