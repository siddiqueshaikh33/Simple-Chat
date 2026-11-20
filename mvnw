#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Shell Script
# ----------------------------------------------------------------------------

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

WRAPPER_JAR="$DIR/.mvn/wrapper/maven-wrapper.jar"

exec "$JAVACMD" -cp "$WRAPPER_JAR" "-Dmaven.multiModuleProjectDirectory=$DIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
