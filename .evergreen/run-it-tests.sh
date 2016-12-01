#!/bin/bash

set -o xtrace   # Write all commands first to stderr
set -o errexit  # Exit the script with error if any of the commands fail

# Supported/used environment variables:
#       MONGODB_URI             Set the suggested connection MONGODB_URI (including credentials and topology info)
#       TOPOLOGY                Allows you to modify variables and the MONGODB_URI based on test topology
#                               Supported values: "server", "replica_set", "sharded_cluster"
#       SCALA_VERSION           Set the version of Scala to be used.


MONGODB_URI=${MONGODB_URI:-}
TOPOLOGY=${TOPOLOGY:-server}
JAVA_HOME="/opt/java/jdk8"

############################################
#            Main Program                  #
############################################

# Provision the correct connection string
if [ "$TOPOLOGY" == "sharded_cluster" ]; then
    MONGODB_URI="mongodb://localhost:27017"
fi

echo "Running Integration tests for Scala $SCALA_VERSION, $TOPOLOGY and connecting to $MONGODB_URI"

./sbt -java-home $JAVA_HOME version
./sbt -java-home $JAVA_HOME ++${SCALA_VERSION} it:test -Dorg.mongodb.test.uri=${MONGODB_URI}
