#!/usr/bin/env bash

exec '%java.home%/bin/java' --module-path '%japp.launcher%' --module org.glavo.japp/org.glavo.japp.launcher.Launcher "${BASH_SOURCE[0]}" "$@"
