#!/usr/bin/env bash

# TODO: In future we should look for japp launcher in the PATH
exec "$JAVA_HOME/bin/java" --module-path '%japp.launcher%' --module org.glavo.japp/org.glavo.japp.launcher.Launcher "${BASH_SOURCE[0]}" "$@"
