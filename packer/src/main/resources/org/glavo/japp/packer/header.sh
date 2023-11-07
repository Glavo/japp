#!/usr/bin/env bash

# TODO: In future we should look for japp launcher in the PATH
exec "$JAVA_HOME/bin/java" -jar '%japp.launcher%' "${BASH_SOURCE[0]}" "$@"
