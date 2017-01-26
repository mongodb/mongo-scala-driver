#!/bin/bash

# DO NOT ECHO COMMANDS AS THEY CONTAIN SECRETS!

set -o errexit  # Exit the script with error if any of the commands fail

############################################
#            Main Program                  #
############################################

echo ${RING_FILE_GPG_BASE64} | base64 -d > ${PROJECT_DIRECTORY}/secring.gpg

echo nexusUsername=${NEXUS_USERNAME} > ${PROJECT_DIRECTORY}/.publishProperties
echo nexusPassword=${NEXUS_PASSWORD} >> ${PROJECT_DIRECTORY}/.publishProperties
echo signing.keyId=${SIGNING_KEY_ID} >> ${PROJECT_DIRECTORY}/.publishProperties
echo signing.password=${SIGNING_PASSWORD} >> ${PROJECT_DIRECTORY}/.publishProperties
echo signing.secretKeyRingFile=${PROJECT_DIRECTORY}/secring.gpg >> ${PROJECT_DIRECTORY}/.publishProperties

echo "Publishing snapshots"

export JAVA_HOME="/opt/java/jdk8"
./sbt -java-home $JAVA_HOME +clean +publishSnapshot -DpublishProperties=$PUBLISH_PROPERTIES_FILE
