#!/usr/bin/env bash

project_dir=$(realpath $(dirname $(realpath "$0"))/..)

$project_dir/gradlew -p $project_dir -q --console plain && java -jar "$project_dir/build/packer.jar" "$@"
