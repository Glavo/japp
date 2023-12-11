#!/usr/bin/env bash

project_dir=$(realpath $(dirname $(realpath "$0"))/..)
japp_jar="$project_dir/build/japp.jar"

if [ ! -f "$japp_jar" ]; then
  echo "Please build the project using './gradlew' first" >&2
  exit 1
fi

java -jar $japp_jar "$@"
