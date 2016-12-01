#!/bin/bash

set -o xtrace   # Write all commands first to stderr
set -o errexit  # Exit the script with error if any of the commands fail

# Supported/used environment variables:
#       SCALA_VERSION   Sets the Scala version

JAVA_HOME="/opt/java/jdk8"

############################################
#            Main Program                  #
############################################

echo "Running unit tests for Scala $SCALA_VERSION"

./sbt -java-home $JAVA_HOME version
./sbt -java-home $JAVA_HOME ++${SCALA_VERSION} unit:test
