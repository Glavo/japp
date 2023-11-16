#!/usr/bin/env bash

# TODO: In future we should look for japp launcher in the PATH
exec "%japp.project.directory%/bin/japp-run.sh" "${BASH_SOURCE[0]}" "$@"
