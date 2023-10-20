#!/usr/bin/env bash

project_dir=$(realpath $(dirname $(realpath "$0"))/..)

java --module-path "$project_dir/build/japp.jar" --module org.glavo.japp/org.glavo.japp.JAppPacker "$@"
