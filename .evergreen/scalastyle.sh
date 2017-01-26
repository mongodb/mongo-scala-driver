#!/bin/bash

set -o xtrace   # Write all commands first to stderr
set -o errexit  # Exit the script with error if any of the commands fail

############################################
#            Main Program                  #
############################################

echo "Compiling and running ScalaStyle"

export JAVA_HOME="/opt/java/jdk8"
./sbt -java-home $JAVA_HOME version
./sbt -java-home $JAVA_HOME clean scalastyle
