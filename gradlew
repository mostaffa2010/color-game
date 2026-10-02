#!/bin/sh

# Standard Gradle Wrapper Script

# Attempt to set APP_HOME
APP_HOME=$(cd "$(dirname "$0")" && pwd -P) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
else
    JAVACMD=java
fi

# If wrapper jar doesn't exist yet, check if system gradle exists
if [ ! -f "$CLASSPATH" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    fi
fi

exec "$JAVACMD" "-Dorg.gradle.appname=$0" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
